/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmt.proxy;

import mmt.proxy.util.FileUtils;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author nguye
 */
public class Proxy implements Runnable {
    private int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private ArrayList<Thread> runningThreads;
    
    public Proxy(int port) {
        this.port = port;
        runningThreads = new ArrayList<>();
        createSocket();
        new Thread(this).start();
    }

    public void listen() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(new RequestHandler(clientSocket));
                runningThreads.add(clientThread);
                clientThread.start();
                System.out.println("New socket client started");
            } catch (IOException ex) {
                System.out.println("Error while accepting and creating client thread:" + ex.getMessage());
            }
        }
    }

    private void createSocket() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Proxy server started at " + port);
        } catch (IOException ex) {
            System.out.println("Error while creating socket with port " + port + ex.getMessage());
            for (int i = 1024; i < 65535; i++) {
                this.port = i;
                try {
                    serverSocket = new ServerSocket(port);
                    running = true;
                    System.out.println("Proxy server started at " + port);
                    break;
                } catch (IOException e) {
                    System.out.println("Error while creating socket with port " + port + ex.getMessage());
                }
            }
        }
    }
    

    private void closeServer() {
        System.out.println("Closing server");
        FileUtils.writeFile();
        closeAllRunningThread();
    }

    private void closeAllRunningThread() {
        for (Thread thread : runningThreads) {
            try {
                if (thread.isAlive()) {
                    System.out.println("Closing thread: " + thread.getId());
                    thread.join();
                }
            } catch (Exception ex) {
                System.out.println("Closing thread failed " + thread.getId() + " : " + ex.getMessage());
            }
        }

        try {
            System.out.println("Closing server");
            serverSocket.close();
        } catch (Exception ex) {
            System.out.println("Error closing proxy server " + ex.getMessage());
        }
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String command;
        while (running) {
            command = scanner.nextLine();
            if (command.equals("close")) {
                running = false;
                closeServer();
                System.exit(0);
            }
        }
        scanner.close();
    }
}
