/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmt.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;

/**
 *
 * @author nguye
 */
public class Utils {
    private static HashSet<String> imageTypes;
    private static HashSet<String> unsupportCharracters;
    
    public Utils() {
        
    }
    
    public static void init(){
        imageTypes = new HashSet<>();
        unsupportCharracters = new HashSet<>();
        imageTypes.add("png");
        imageTypes.add("jpg");
        imageTypes.add("jpeg");
        imageTypes.add("gif");
        unsupportCharracters.add("\\");
        unsupportCharracters.add("/");
        unsupportCharracters.add(":");
        unsupportCharracters.add("*");
        unsupportCharracters.add("?");
        unsupportCharracters.add("\"");
        unsupportCharracters.add(">");
        unsupportCharracters.add("<");
        unsupportCharracters.add("|");
    }
    
    public static boolean isImage(String ext) {
        return imageTypes.contains(ext);
    }
    
    public static String createCachedFileNameFromUrl(String url) {
        url = url.replace("http://", "");
        if (url.length() > 55) {
            url = url.substring(0, 15) + "_" + url.substring(url.length() - 35);
        }
        for (String unsupportChar : unsupportCharracters) {
            url = url.replace(unsupportChar, "");
        }
        return url;
    }
    public static String getFileExt(File file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }

    public static String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public static String getFileName(File file) {
        return file.getName().substring(0, file.getName().lastIndexOf("."));
    }
    
    public static BufferedReader sendGetToRemoteServer(String destUrl) {
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
    
    public static void sendNotFoundMessageToClient(BufferedWriter proxyToClientBw) {
        try {
            String error = "HTTP/1.0 404 NOT FOUND\n"
                    + "Proxy-agent: ProxyServer/1.0\n"
                    + "\r\n";
            proxyToClientBw.write(error);
            proxyToClientBw.flush();
        } catch (Exception ex) {
            System.out.println("Send not found error: " + ex.getMessage());
        }
    }
    
    public static void sendOkMessageToClient(BufferedWriter proxyToClientBw) {
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
    
    public static void sendBlockedResponse(BufferedWriter proxyToClientBw) {
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
}
