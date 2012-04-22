/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ipclient;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aswin
 */
public class PeerThread implements Runnable {
    
    int peerport;
    ServerSocket ps;
    
    PeerThread(IPClient parent,int peerport)
    {
        this.peerport = peerport;
    }
    
    public void run()
    {
        try {
            ps = new ServerSocket(peerport);
            
            while(true)
            {
                Socket s = ps.accept();
                System.out.println("Client conection accepted!");
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                
                //Get the request from peer
                int i = 0;
                int c;
                char [] ch = new char[10000];
                while (( c = in.read()) != '\n') {
                    ch[i] = (char) c;
                    i++;
                    System.out.print((char) c);
                }
                String response = new String(ch);
                StringTokenizer st = new StringTokenizer(response,":");
                String type = st.nextToken();
                String fileName = st.nextToken();

                if(type.equals("50"))   //Request for a file. Should reply with the full file contents
                {
                    String fullFilePath = "C:/GangsharedFiles/"+fileName;
                    File requestedFile = new File(fullFilePath);
                    long filesize = requestedFile.length();
                    FileInputStream fin = new FileInputStream(fullFilePath);
                    byte[] fileData = new byte[(int)filesize];
                    System.out.println("File Sixe: " + filesize);
                    fin.read(fileData);
                    System.out.println("File Content Read: " + fileData);
                    //Send it to the peer requested
                    out.write(fileData);
                    System.out.println("File sent to client.");
                    
                }
                
            }
            
            
        } catch (IOException ex) {
            Logger.getLogger(PeerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
