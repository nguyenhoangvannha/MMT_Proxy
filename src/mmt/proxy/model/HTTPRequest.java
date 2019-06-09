/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmt.proxy.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nguye
 */
public class HTTPRequest {

    String method;
    String uri;
    String host;
    int contentLength;
    String accept;
    String contentType;
    String acceptLanguage;
    String protocol;
    String payload;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getAccept() {
        return accept;
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    public void setAcceptLanguage(String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public HTTPRequest(Socket clientSocket) {
        try {
            BufferedReader clientToProxyBr = null;
            clientToProxyBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            
            while (!(inputLine = clientToProxyBr.readLine()).equals("")) {
                processLine(inputLine);
            }
            if (contentLength > 0) {
                StringBuilder payload = new StringBuilder("\n");
                while (clientToProxyBr.ready()) {
                    char c = (char) clientToProxyBr.read();
                    payload.append(c);
                }
                setPayload(payload.toString());
            }
            
        } catch (IOException ex) {
            Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void processLine(String line) {
        String prop = line.substring(0, line.indexOf(" "));
        if (!prop.contains(":")) {
            setMethod(prop);
            setUri(line.substring(4, line.lastIndexOf(" ")));
            setProtocol(line.substring(line.lastIndexOf(" ") + 1));
        } else {
            prop = prop.replace(":", "");
            switch (prop) {
                case "Host":
                    setHost(line.substring(line.indexOf(" ") + 1));
                    break;
                case "Accept":
                    setAccept(line.substring(line.indexOf(" ") + 1));
                    break;
                case "Content-Length":
                    String strLength = line.substring(line.indexOf(" ") + 1);
                    try {
                        setContentLength(Integer.parseInt(strLength));
                    } catch (Exception ex) {
                        setContentLength(0);
                    }
                    break;
                case "Content-Type":
                    setContentType(line.substring(line.indexOf(" ") + 1));
                    break;
                case "Accept-Language":
                    setAcceptLanguage(line.substring(line.indexOf(" ") + 1));
                    break;

            }
        }
    }

    @Override
    public String toString() {
        return "HTTPRequest{" + "method=" + method + ", uri=" + uri + ", contentLength=" + contentLength + ", contentType=" + contentType + ", protocol=" + protocol + '}';
    }

}
