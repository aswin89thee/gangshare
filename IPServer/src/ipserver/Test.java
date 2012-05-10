/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ipserver;

import framework.hashing.Trigest;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hp
 */
public class Test {
    public static void main(String args[]){
        try {
            File f = new File("pdf.txt");
            Trigest trigest = new Trigest(f);
            byte[] digest = trigest.getSignature();
            Trigest trigest1 = new Trigest("dgoogle");
            byte[] digest1 = trigest1.getSignature();
            for(int i=0;i<1024;i++){
                if(digest1[i] != 0)
                    System.out.println(i + "\t" + digest[i] + "\t" + digest1[i]);
            }
        } catch (Exception ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
