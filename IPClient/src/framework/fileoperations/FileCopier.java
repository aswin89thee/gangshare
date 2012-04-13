/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.fileoperations;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
/**
 *
 * @author Aswin
 */
public class FileCopier {
    
    public static void copyFile(File sourceFile, File destFile)
                throws IOException {
        if (!sourceFile.exists()) {
                return;
        }
        if (!destFile.exists()) {
                destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
                destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
                source.close();
        }
        if (destination != null) {
                destination.close();
        }

}
    
}
