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
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmt.proxy.model.CachedDetails;

/**
 *
 * @author nguye
 */
public class FileUtils {
    private static HashMap<String, CachedDetails> cachedSites;
    private static HashSet<String> blackList;
    
    private static final int TTL = 15000; //Time to live in milis
    private static final String FILE_CACHED_MAP = "CACHED_FILE";
    private static final String FILE_BLACK_LIST = "blacklist.conf";
    
    public static void init(){
        cachedSites = new HashMap<String, CachedDetails>();
        blackList = new HashSet<>();
        createCachedDir();
        readFile();
    }
    
    private static void readFile(){
        readCachedSites();
        readBlackList();
    }
    
    public static void writeFile(){
        writeCachedMap(FILE_CACHED_MAP, cachedSites);
        writeBlackList(FILE_BLACK_LIST, blackList);
    }
    
    private static void readCachedSites() {
        try {
            File cachedFile = new File(FILE_CACHED_MAP);
            if (!cachedFile.exists()) {
                System.out.println("No cached file, creating new one");
                cachedFile.createNewFile();
            } else {
                FileInputStream fileInputStream = new FileInputStream(cachedFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                cachedSites = (HashMap<String, CachedDetails>) objectInputStream.readObject();
                fileInputStream.close();
                objectInputStream.close();
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Error while loading cached files: " + ex.getMessage());
        }
    }

    private static void readBlackList() {
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
    
    private static void writeCachedMap(String fileName, Object data) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(data);
            objectOutputStream.close();
            fileOutputStream.close();
            System.out.println("Saved " + fileName);
        } catch (IOException ex) {
            System.out.println("Error while saving " + fileName + ": " + ex.getMessage());
        }
    }

    private static void writeBlackList(String fileName, HashSet<String> blackList) {
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
    
    public static boolean isBlockedSite(String host) {
        return blackList.contains(host);
    }

    public static File getCachedFile(String url) {
        CachedDetails cachedDetails = cachedSites.get(url);
        if (cachedDetails == null) {
            return null;
        } else {
            if (!NetUtils.netIsAvailable()) {
                return cachedDetails.getFile();
            }

            long liveTime = Math.abs(System.currentTimeMillis() - cachedDetails.getAddTime());
            if (liveTime > TTL) {
                return null;
            } else {
                return cachedDetails.getFile();
            }
        }
    }

    public static void addCachedFile(String url, File file) {
        if (cachedSites.containsKey(url)) {
            cachedSites.remove(url);
        }
        cachedSites.put(url, new CachedDetails(file));
    }

    private static void createCachedDir() {
        File file = new File("cached");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }
    }
}
