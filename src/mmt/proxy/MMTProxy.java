/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mmt.proxy;

/**
 *
 * @author nguye
 */
public class MMTProxy {
    
    private static final int PORT = 8888;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        FileUtils.init();
        NetUtils.init();
        Proxy proxy = new Proxy(PORT);
        proxy.listen();
    }
    
}
