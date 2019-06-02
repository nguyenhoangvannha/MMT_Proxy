/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmt.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nguye
 */
public class Proxy implements Runnable {

    private int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private static HashMap<String, File> cachedSites;
    private static HashSet<String> blackList;
    private ArrayList<Thread> runningThreads;

    private static final String FILE_CACHED_MAP = "CACHED_FILE";
    private static final String FILE_BLACK_LIST = "blacklist.conf";

    public Proxy(int port) {
        this.port = port;
        cachedSites = new HashMap<>();
        blackList = new HashSet<>();
        runningThreads = new ArrayList<>();
        createCachedDir();
        readCachedSites();
        readBlackList();
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
            } catch (Exception ex) {
                System.out.println("Error while accepting and creating client thread:" + ex.getMessage());
            }
        }
    }

    private void readCachedSites() {
        try {
            File cachedFile = new File(FILE_CACHED_MAP);
            if (!cachedFile.exists()) {
                System.out.println("No cached file, creating new one");
                cachedFile.createNewFile();
            } else {
                FileInputStream fileInputStream = new FileInputStream(cachedFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                cachedSites = (HashMap<String, File>) objectInputStream.readObject();
                fileInputStream.close();
                objectInputStream.close();
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Error while loading cached files: " + ex.getMessage());
        }
    }

    private void readBlackList() {
        try {
            File blackListFile = new File(FILE_BLACK_LIST);
            BufferedReader br = new BufferedReader(new FileReader(blackListFile));
            String line;
            while ((line = br.readLine()) != null) {
                blackList.add(line);
            }
        } catch (IOException ex) {
            System.out.println("Error while loading black list: " + ex.getMessage());
        }
    }

    private void createSocket() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Proxy server started at " + port);
        } catch (IOException ex) {
            System.out.println("Error while creating socket: " + ex.getMessage());
        }
    }

    private void closeServer() {
        System.out.println("Closing server");
        writeCachedMap(FILE_CACHED_MAP, cachedSites);
        writeBlackList(FILE_BLACK_LIST, blackList);
        closeAllRunningThread();
    }

    private void writeCachedMap(String fileName, Object data) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(data);
            objectOutputStream.close();
            fileOutputStream.close();
            System.out.println("Saved " + fileName);
        } catch (Exception ex) {
            System.out.println("Error while saving " + fileName + ": " + ex.getMessage());
        }
    }

    private void writeBlackList(String fileName, HashSet<String> blackList) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
            for (String site : blackList) {
                fw.write(site + "\n");
            }
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    public static boolean isBlockedSite(String host) {
        return blackList.contains(host);
    }

    public static File getCachedFile(String url) {
        return cachedSites.get(url);
    }

    public static void addCachedFile(String url, File file) {
        cachedSites.put(url, file);
    }

    private void createCachedDir() {
        File file = new File("cached");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
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
