/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ipclient;

import framework.hashing.Trigest;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.net.InetAddress;
import javax.swing.JOptionPane;

/**
 *
 * @author Ramit
 */


public class Search extends javax.swing.JFrame {
    
    DashBoard parent;
    IPClient root;
    Vector vResult;
    ArrayList<Ranking> resultset; 
        
    final int peerport = 20000;
                
    /**
     * Creates new form Search
     */
    public Search(DashBoard d, IPClient rt) {
        parent = d;
        root = rt;
        initComponents();
        jButtonDload.setVisible(false);
        jListResult.setEnabled(false);
    }
    

   /* public String receiveSearchResult() {
        int c, i;
        String msg="";
        Vector vResult = new Vector();
        resultset = new ArrayList<Ranking>();        
        char [] ch = new char[1000];	
        try {
            i = 0;
            while (( c = root.in.read()) != '\n') {
                ch[i] = (char) c;
                i++;
                //System.out.print((char) c);
            }
            msg = new String(ch);
            msg = msg.trim();
            System.out.println("\nResponse from server: " + msg);
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            int resultCount = Integer.parseInt(st.nextToken());
            if(msg_type.equals("69") && resultCount != 0){
                jButtonDload.setVisible(true);
                for(int k=0; k < resultCount; k++) {
                    msg="";
                    ch = new char[1000];	
                    i = 0;
                    while (( c = root.in.read()) != '\n') {
                        ch[i] = (char) c;
                        i++;
                    //System.out.print((char) c);
                    }
                    msg = new String(ch);
                    msg = msg.trim();
                    System.out.println("\nResponse from server: " + msg);
                    
                    st = new StringTokenizer(msg,":");
                    msg_type = st.nextToken();
                    String filename = st.nextToken();
                    String ip = st.nextToken();
                    String abs = st.nextToken();
                    String size = st.nextToken();
                    vResult.add(filename+"\t:\t"+abs);
                    resultset.add(new Ranking(filename,ip,abs,Double.parseDouble(size),));
                }
                
            }
            else {
                vResult.add("No matched files.");
                jButtonDload.setVisible(false);
            }
            jListResult.setListData(vResult);
        }
        catch(Exception e){
            System.out.println("EXCEPTION:: receiveResponse :"+e.getMessage());
        }
        return msg;
   }*/
    
     public static int countFirstHalf(byte[] array){
         int count = 0;
         for(int i = 0; i<array.length/2;i++){
             byte temp = array[i];
             int c = 0;
             while(c < 8){
                 count += temp&1;
                 temp>>=1;
                 c++;
             }
         }
         return count;
     }
     
     
     public static int countLastHalf(byte[] array){
         int count = 0;
         for(int i = array.length/2; i<array.length;i++){
             byte temp = array[i];
             int c = 0;
             while(c < 8){
                 count += temp&1;
                 temp>>=1;
                 c++;
             }
         }
         return count;
     }
    
    private boolean isPresentDSearch(Coordinate start, Coordinate end, byte[] digestOfFile) {
        int firstHalf = countFirstHalf(digestOfFile);
        int secondHalf = countLastHalf(digestOfFile);
        System.out.println("the corrdinate of file is " + firstHalf + ":" + secondHalf);
        if((/*firstHalf>=start.x && */firstHalf<=end.x) && (/*secondHalf>=start.y && */secondHalf<=end.y)){
            return true;
        }
        return false;
    }
    
    public void search(String searchString) {
        try {
            Iterator<HashSpace> it = parent.parent.hashList.iterator();
            vResult = new Vector();
            resultset = new ArrayList<Ranking>();
            Trigest trigest = new Trigest(searchString);
            byte[] digest = trigest.getSignature();
            Map sentIPs = new LinkedHashMap();
            while(it.hasNext()) {
                HashSpace hs = it.next();
                if(isPresentDSearch(hs.start, hs.end, digest)){
                    if(!sentIPs.containsKey(hs.IP)){
                        sendReceiveSearchRequest(searchString,hs.IP);
                        sentIPs.put(hs.IP, new Integer(1));
                    }
                }
            }
            if(resultset.isEmpty()) {
                    vResult.add("No matched files.");
                    jButtonDload.setVisible(false);
                    jButton1.setVisible(false);
                }
            else {
                Collections.sort(resultset, Ranking.COMPARE_BY_SUMONES_HITCOUNT);
                Iterator<Ranking> it2 = resultset.iterator();
                
                while(it2.hasNext()) {
                    Ranking rk = it2.next();
                    vResult.add(rk.fileName);
                }
                jButtonDload.setVisible(true);
                jButton1.setVisible(true);
            }
            jListResult.setListData(vResult);
        } catch (Exception ex) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendReceiveSearchRequest(String searchString, String ip) {
        
        try {
            String type = "05";
            String msg = type + ":" + searchString+"\n";
            Socket peerSocket = new Socket(ip,peerport);
            OutputStream pout = peerSocket.getOutputStream();
            InputStream pin = peerSocket.getInputStream();
            
            //Request the server for the file info by sending the command msg
            System.out.println("Sending Search Request "+msg+" to IP "+ip);
            pout.write(msg.getBytes());
            
            
            msg="";
            char[] ch = new char[10000];	
            int c,i = 0;
            while (( c = pin.read()) != '\n') {
                ch[i] = (char) c;
                i++;
                System.out.print((char) c);
            }
            msg = new String(ch);
            msg = msg.trim();
            System.out.println("\nResponse from peer: " + msg);

            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            int resultCount = Integer.parseInt(st.nextToken());
            
            if(msg_type.equals("69") && resultCount != 0){
                System.out.println("Received Count.. receiving files list!");
                for(int k=0; k < resultCount; k++) {
                    msg="";
                    ch = new char[1000];	
                    i = 0;
                    while (( c = pin.read()) != '\n') {
                        ch[i] = (char) c;
                        i++;
                    System.out.print((char) c);
                    }
                    msg = new String(ch);
                    msg = msg.trim();
                    System.out.println("\nResponse from server: " + msg);
                    
                    st = new StringTokenizer(msg,":");
                    msg_type = st.nextToken();
                    String filename = st.nextToken();
                    String fileIP = st.nextToken();
                    String abs = st.nextToken();
                    int size = (int)Double.parseDouble(st.nextToken());
                    int hitCount = Integer.parseInt(st.nextToken());
                    int sumOnes = Integer.parseInt(st.nextToken());

                    resultset.add(new Ranking(filename, fileIP, abs, size, hitCount, sumOnes));
                }
            }
            
        }
        catch(Exception e){System.out.println("EXCEPTION: sendSearchRequest()"+e.getMessage());
        }
        
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        searchField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jButtonSearch = new javax.swing.JButton();
        jButtonBack = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListResult = new javax.swing.JList();
        jLabel2 = new javax.swing.JLabel();
        jButtonDload = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Gangshare-D Search");
        setName("GANGSHARE - Search");

        jLabel1.setText("Keywords:");

        jButtonSearch.setText("Search Gangshare");
        jButtonSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSearchActionPerformed(evt);
            }
        });

        jButtonBack.setText("Back");
        jButtonBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonBackActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButtonSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonBack, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonSearch)
                    .addComponent(jButtonBack))
                .addContainerGap())
        );

        jListResult.setEnabled(false);
        jScrollPane1.setViewportView(jListResult);

        jLabel2.setText("Search Results");

        jButtonDload.setText("Download File");
        jButtonDload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDloadActionPerformed(evt);
            }
        });

        jButton1.setText("Show Abstract");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1)
                            .addComponent(jSeparator1))
                        .addContainerGap())
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGroup(layout.createSequentialGroup()
                .addGap(135, 135, 135)
                .addComponent(jLabel2)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addGap(78, 78, 78)
                .addComponent(jButtonDload)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonDload)
                    .addComponent(jButton1))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSearchActionPerformed
        try {
            if(searchField.getText().equals("")) {
            JOptionPane.showMessageDialog(this,"Enter search keyword!","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }
            // TODO add your handling code here:
            String searchString = searchField.getText();
            
            //Trigest trigest = new Trigest(searchString);
            //byte[] signature = trigest.getSignature();
            
            jButtonDload.setVisible(true);
            jListResult.setEnabled(true);
            search(searchString);
        } catch (Exception ex) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }//GEN-LAST:event_jButtonSearchActionPerformed

    private void jButtonDloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDloadActionPerformed
        // TODO add your handling code here:
        if(jListResult.getSelectedIndex() == -1) return;
        int index = jListResult.getSelectedIndex();
        Ranking selectedItem = resultset.get(index);
        try{
        if(selectedItem.ip.equals(InetAddress.getLocalHost().getHostAddress())) {
         JOptionPane.showMessageDialog(this,"This is a loca file!","Error",JOptionPane.ERROR_MESSAGE);
         return;   
        }
        }catch(Exception e){ System.out.println("EXCEPTION"+e.getMessage());}
        System.out.println("Download FIle:" + selectedItem.ip + selectedItem.fileName + (int)Double.parseDouble(selectedItem.size));
        root.downloadFile(selectedItem.ip, selectedItem.fileName, (int)Double.parseDouble(selectedItem.size), this);
    }//GEN-LAST:event_jButtonDloadActionPerformed

    private void jButtonBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonBackActionPerformed
        // TODO add your handling code here:
        this.setVisible(false);
        parent.setVisible(true);
    }//GEN-LAST:event_jButtonBackActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        int selectedIndex = jListResult.getSelectedIndex();
        if( selectedIndex== -1){
            JOptionPane.showMessageDialog(this, "No file Selected", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Ranking selectedItem = resultset.get(selectedIndex);
        
        new ShowAbstract(this, selectedItem.fileName, selectedItem.abs).setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    //public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
   /*
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new Search().setVisible(true);
            }
        });
    }*/
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonBack;
    private javax.swing.JButton jButtonDload;
    private javax.swing.JButton jButtonSearch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JList jListResult;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField searchField;
    // End of variables declaration//GEN-END:variables
}
