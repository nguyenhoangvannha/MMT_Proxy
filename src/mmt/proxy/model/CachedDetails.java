/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmt.proxy.model;

import java.io.File;
import java.io.Serializable;

/**
 *
 * @author nguye
 */
public class CachedDetails implements Serializable{
    private File file;
    private long addTime;

    public CachedDetails(File file) {
        this.file = file;
        this.addTime = System.currentTimeMillis();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }
}
