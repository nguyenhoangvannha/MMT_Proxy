/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmt.proxy;

import mmt.proxy.util.FileUtils;
import mmt.proxy.util.NetUtils;
import mmt.proxy.model.HTTPRequest;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author nguye
 */
public class RequestHandler implements Runnable {

    private Socket clientSocket;
    //BufferedReader clientToProxyBr;
    BufferedWriter proxyToClientBw;

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.clientSocket.setSoTimeout(4000);
            //clientToProxyBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException ex) {
            System.out.println("Cannot get data stream between client and server: " + ex.getMessage());
        }
    }

    @Override
    public void run() {
        HTTPRequest request = new HTTPRequest(clientSocket);
        if (request.getMethod() == null || request.getMethod().isEmpty()) {
            System.out.println("Request error");
            return;
        } else {
            System.out.println(request.toString());
        }
        String method = request.getMethod();
        String destUrl = request.getUri();

        if (FileUtils.isBlockedSite(request.getHost())) {
            System.out.println("Blocked request: " + destUrl);
            NetUtils.sendBlockedResponse(proxyToClientBw);
            return;
        }

        if (method.equalsIgnoreCase("CONNECT")) {
            System.out.println("Drop https reqest: " + destUrl);
        } else if (method.equalsIgnoreCase("POST")) {
            sendNonCachedToClient(request);
        } else if (method.equalsIgnoreCase("GET")) {
            doGetMethod(request);
        }
    }

    private void doGetMethod(HTTPRequest request) {
        String uri = request.getUri();
        File cachedFile;
        if ((cachedFile = FileUtils.getCachedFile(uri)) != null) {
            System.out.println("Sending cached file: " + uri);
            sendCachedToClient(cachedFile);
        } else {
            System.out.println("Requesting : " + uri + "\n");
            sendNonCachedToClient(request);
        }
    }

    private void sendCachedToClient(File cachedFile) {
        try {
            String fileExt = NetUtils.getFileExt(cachedFile);
            if (NetUtils.isImage(fileExt)) {
                BufferedImage image = ImageIO.read(cachedFile);
                if (image == null) {
                    NetUtils.sendNotFoundMessageToClient(proxyToClientBw);
                } else {
                    sendImageToClient(image, fileExt);
                }
            } else {
                BufferedReader cachedFileBR = new BufferedReader(new InputStreamReader(new FileInputStream(cachedFile)));
                NetUtils.sendOkMessageToClient(proxyToClientBw);
                String line = "";
                while ((line = cachedFileBR.readLine()) != null) {
                    proxyToClientBw.write(line);
                }
                proxyToClientBw.flush();
                cachedFileBR.close();
            }
            if (proxyToClientBw != null) {
                proxyToClientBw.close();
            }
        } catch (IOException ex) {
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

    private void sendImageToClient(BufferedImage image, String fileExt) {
        try {
            NetUtils.sendOkMessageToClient(proxyToClientBw);
            ImageIO.write(image, fileExt, clientSocket.getOutputStream());
        } catch (Exception ex) {
            System.out.println("Send image error: " + ex.getMessage());
        }
    }

    private void sendNonCachedToClient(HTTPRequest request) {
        String destUrl = request.getUri();
        try {
            String cachedFileName = NetUtils.createCachedFileNameFromUrl(destUrl);
            String fileExt = NetUtils.getFileExt(destUrl);
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

            if (NetUtils.isImage(fileExt)) {
                URL remoteURL = new URL(destUrl);
                BufferedImage image = ImageIO.read(remoteURL);
                if (image != null) {
                    ImageIO.write(image, fileExt, cacheFile);
                    sendImageToClient(image, fileExt);
                } else {
                    NetUtils.sendNotFoundMessageToClient(proxyToClientBw);
                    return;
                }
            } else {
                BufferedReader proxyToServerBR = null;
                if (request.getMethod().equalsIgnoreCase("GET")) {
                    proxyToServerBR = NetUtils.sendGetToRemoteServer(destUrl);
                } else {
                    proxyToServerBR = NetUtils.sendPostToRemoteServer(request);
                }
                if (proxyToServerBR == null ) {
                    return;
                }
                NetUtils.sendOkMessageToClient(proxyToClientBw);
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
                FileUtils.addCachedFile(destUrl, cacheFile);
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
}
