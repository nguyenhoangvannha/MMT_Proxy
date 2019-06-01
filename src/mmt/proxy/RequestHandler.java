/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmt.proxy;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author nguye
 */
public class RequestHandler implements Runnable {

    private Socket clientSocket;
    BufferedReader clientToProxyBr;
    BufferedWriter proxyToClientBw;
    private HashSet<String> imageTypes;
    private HashSet<Character> unsupportCharracters;

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        initConstantList();
        try {
            this.clientSocket.setSoTimeout(2000);
            clientToProxyBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException ex) {
            System.out.println("Cannot get data stream between client and server: " + ex.getMessage());
        }
    }

    @Override
    public void run() {
        String requestString = "";
        try {
            requestString = clientToProxyBr.readLine();
            System.out.println("==================================");
            System.out.println("REQEST: " + requestString);
        } catch (Exception ex) {
            System.out.println("Error reading request from client: " + ex.getMessage());
            return;
        }
        String[] requestDetails = requestString.split(" ");
        String method = requestDetails[0];
        String destUrl = requestDetails[1];

        if (Proxy.isBlockedSite(destUrl)) {
            System.out.println("Blocked request: " + destUrl);
            sendBlockedResponse();
            return;
        }

        if (method.equalsIgnoreCase("CONNECT")) {
            System.out.println("Drop https reqest: " + destUrl);
        } else {
            File cachedFile;
            if ((cachedFile = Proxy.getCachedFile(destUrl)) != null) {
                System.out.println("Sending cached file: " + destUrl);
                sendCachedToClient(cachedFile);
            } else {
                System.out.println("Requesting : " + destUrl + "\n");
                sendNonCachedToClient(destUrl);
            }
        }
    }

    private void sendBlockedResponse() {
        try {
            String line = "HTTP/1.0 403 Access Forbidden \n"
                    + "User-Agent: ProxyServer/1.0\n"
                    + "\r\n";
            proxyToClientBw.write(line);
            proxyToClientBw.flush();
            proxyToClientBw.close();
        } catch (Exception ex) {
            System.out.println("Error sending blocked site: " + ex.getMessage());
        }
    }

    private void sendCachedToClient(File cachedFile) {
        try {
            String fileExt = getFileExt(cachedFile);
            if (isImage(fileExt)) {
                BufferedImage image = ImageIO.read(cachedFile);
                if (image == null) {
                    sendNotFoundMessageToClient(cachedFile.getName());
                } else {
                    sendImageToClient(image, fileExt);
                }
            } else {
                BufferedReader cachedFileBR = new BufferedReader(new InputStreamReader(new FileInputStream(cachedFile)));
                sendOkMessageToClient();
                String line = "";
                while ((line = cachedFileBR.readLine()) != null) {
                    proxyToClientBw.write(line);
                }
                proxyToClientBw.flush();
                if (cachedFileBR != null) {
                    cachedFileBR.close();
                }
            }
            if (proxyToClientBw != null) {
                proxyToClientBw.close();
            }
        } catch (Exception ex) {
            System.out.println("Send cached file error: " + ex.getMessage());
            if (proxyToClientBw != null) {
                try {
                    proxyToClientBw.close();
                } catch (IOException ex1) {
                    Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }

    private void sendOkMessageToClient() {
        try {
            String response = "HTTP/1.0 200 OK\n"
                    + "Proxy-agent: ProxyServer/1.0\n"
                    + "\r\n";
            proxyToClientBw.write(response);
            proxyToClientBw.flush();
        } catch (IOException ex) {
            System.out.println("SendOkMessageToClient error: " + ex.getMessage());
        }
    }

    private void sendImageToClient(BufferedImage image, String fileExt) {
        try {
            sendOkMessageToClient();
            ImageIO.write(image, fileExt, clientSocket.getOutputStream());
        } catch (Exception ex) {
            System.out.println("Send image error: " + ex.getMessage());
        }
    }

    private void sendNotFoundMessageToClient(String destUrl) {
        try {
            System.out.println("File not found: " + destUrl);
            String error = "HTTP/1.0 404 NOT FOUND\n"
                    + "Proxy-agent: ProxyServer/1.0\n"
                    + "\r\n";
            proxyToClientBw.write(error);
            proxyToClientBw.flush();
        } catch (Exception ex) {
            System.out.println("Send not found error: " + ex.getMessage());
        }
    }

    private void sendNonCachedToClient(String destUrl) {
        try {
            String cachedFileName = createCachedFileNameFromUrl(destUrl);
            String fileExt = getFileExt(destUrl);
            boolean caching = true;
            File cacheFile = null;
            BufferedWriter cacheFileBW = null;
            try {
                cacheFile = new File("cached/" + cachedFileName);
                if (!cacheFile.exists()) {
                    cacheFile.createNewFile();
                }
                cacheFileBW = new BufferedWriter(new FileWriter(cacheFile));
            } catch (Exception ex) {
                System.out.println("Couldn't cache: " + cachedFileName + ": " + ex.getMessage());
                caching = false;
            }

            if (isImage(fileExt)) {
                URL remoteURL = new URL(destUrl);
                BufferedImage image = ImageIO.read(remoteURL);
                if (image != null) {
                    ImageIO.write(image, fileExt, cacheFile);
                    sendImageToClient(image, fileExt);
                } else {
                    sendNotFoundMessageToClient(destUrl);
                    return;
                }
            } else {

                BufferedReader proxyToServerBR = getResultStreamFromServer(destUrl);
                sendOkMessageToClient();
                String line = null;
                while ((line = proxyToServerBR.readLine()) != null) {
                    proxyToClientBw.write(line);
                    if (caching) {
                        cacheFileBW.write(line);
                    }
                }

                proxyToClientBw.flush();

                if (proxyToServerBR != null) {
                    proxyToServerBR.close();
                }
            }

            if (caching) {
                cacheFileBW.flush();
                Proxy.addCachedFile(destUrl, cacheFile);
                System.out.println("Cached: " + destUrl);
            }

            if (cacheFileBW != null) {
                cacheFileBW.close();
            }

            if (proxyToClientBw != null) {
                proxyToClientBw.close();
            }

        } catch (Exception ex) {
            System.out.println("Send non cached file error: " + ex.getMessage());
            if (proxyToClientBw != null) {
                try {
                    proxyToClientBw.close();
                } catch (IOException ex1) {
                    Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }

    private BufferedReader getResultStreamFromServer(String destUrl) {
        URL remoteURL;
        try {
            remoteURL = new URL(destUrl);
            HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();
            proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            proxyToServerCon.setRequestProperty("Content-Language", "en-US");
            proxyToServerCon.setUseCaches(false);
            proxyToServerCon.setDoOutput(true);
            BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
            return proxyToServerBR;
        } catch (Exception ex) {
            System.out.println("GetFileStreamFromServer error : " + ex.getMessage());
        }
        return null;
    }

    private String getFileExt(File file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }

    private String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String createCachedFileNameFromUrl(String url) {
        url = url.replace("http://", "");
        if (url.length() > 55) {
            url = url.substring(0, 15) + "_" + url.substring(url.length() - 35);
        }
        for (char unsupportChar : unsupportCharracters) {
            url = url.replace(unsupportChar + "", "");
        }
        return url;
    }

    private String getFileName(File file) {
        return file.getName().substring(0, file.getName().lastIndexOf("."));
    }

    private boolean isImage(String ext) {
        return imageTypes.contains(ext);
    }

    private void initConstantList() {
        imageTypes = new HashSet<>();
        unsupportCharracters = new HashSet<>();
        imageTypes.add("png");
        imageTypes.add("jpg");
        imageTypes.add("jpeg");
        imageTypes.add("gif");
        unsupportCharracters.add('\\');
        unsupportCharracters.add('/');
        unsupportCharracters.add(':');
        unsupportCharracters.add('*');
        unsupportCharracters.add('?');
        unsupportCharracters.add('"');
        unsupportCharracters.add('>');
        unsupportCharracters.add('<');
        unsupportCharracters.add('|');
    }
}
