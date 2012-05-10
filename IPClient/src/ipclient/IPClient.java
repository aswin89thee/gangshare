package ipclient;

import framework.fileoperations.FileCopier;
import framework.hashing.Trigest;
import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
/**
 *
 * @author Ramit
 */
public class IPClient extends javax.swing.JFrame {
    
    //TCP
    InetAddress host;
    Socket s;
    int port = 6000;
    OutputStream out;
    InputStream in;
    static String publicKey;
    Thread mythread;
    static int peerport = 20000;
    String serverIp;
    Config parent;
    /**
     * Creates new form IPClient2
     */
    public IPClient(Config c,String ip) {
        serverIp = ip;
        parent = c;
        initComponents();
        initConnection();
        mythread = new Thread(new PeerThread(this,peerport));
        mythread.start();

    }

     private void initConnection() {
        try {
        host = InetAddress.getByName(serverIp);
	s = new Socket(host,port) ;
        System.out.println("Host " + host.getHostName() + " connected to Server on port " + port);
        out = s.getOutputStream();
        in = s.getInputStream();
        readKeyFromStream(s);
        System.out.println("Public Key recieved: "+publicKey);
        } 
        catch(Exception e) {
            System.out.println("EXCEPTION:: initConnection : " + e.getMessage());
            JOptionPane.showMessageDialog(this,"Failed to connect to server.!","Error",JOptionPane.ERROR_MESSAGE);
            this.setVisible(false);
            parent.setVisible(true);
        }
    }
     
    void readKeyFromStream(Socket s) throws IOException{
        StringBuffer fileData = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        char[] buf = new char[1024];
        int numRead;
        numRead = br.read(buf);
        //br.close();
        publicKey = String.valueOf(buf, 0, numRead).substring(3);
    }
     
    public void sendMsg(String msg) {
        try {
            msg = msg.concat("\n");
            byte bmsg[] = msg.getBytes();
            out.write(bmsg);
            System.out.println("Message sent to Server: "+ msg);
        } 
        catch(Exception e) {
            System.out.println("EXCEPTION:: sendMsg : "+e.getMessage());
        }
    }
    
    //Send byte message to server
     public void sendMsg(byte[] bmsg)
     {
        try {
            out.write(bmsg);
        } catch (IOException ex) {
            Logger.getLogger(IPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
     }
     
     
     //Download a specific file from a specific peer
     public void downloadFile(String IP, String fileName, int fileSize, JFrame f)
     {
        try {
            OutputStream pout;
            InputStream pin;
            
            Socket fileSocket = new Socket(IP,peerport);
            String msg = "50:"+fileName+"\n";
            System.out.println("After connect: "+msg);
            pout = fileSocket.getOutputStream();
            pin = fileSocket.getInputStream();
            
            //Request the server for the file by sending the command msg
            pout.write(msg.getBytes());
            
            byte[] fileContents = new byte[fileSize];
            pin.read(fileContents);
            
            
            //Put it in the file
            String targetFile = "C:/GangdownloadedFiles/"+fileName;
                //Create the file
            File file = new File(targetFile);
            file.createNewFile();
            FileOutputStream fout = new FileOutputStream(targetFile);
            fout.write(fileContents);
            
            //Message sent to Server to increment Hit Count
            msg = "111:"+fileName+":"+IP;
            System.out.println("Message sent to server to incrememt HitCount= "+ msg);
            sendMsg(msg);
            
            //Create a dialog saying file successfully downloaded
            JOptionPane.showMessageDialog(f,"File successfully downloaded!");
            
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(IPClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(IPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
     }
    
     
    
    //Publish its own file to the server
    public void publishFile(String path, String abstractOfFile)
    {
        try {
            String type = "04";
            String fileName = "";
            double fileSize = 0f;
            String hostIP = host.toString();
            byte[] digestOfFile = new byte[1024];
            
            //Get the details of the file - name, size
            File file = new File(path);
            fileSize = file.length();
            fileName = file.getName();
            
            
            //Copy the file to c:/GangsharedFiles
            String sharedDir = "C:/GangsharedFiles/";
            String destFilePath = sharedDir + fileName;
            File destFile = new File(destFilePath);
            System.out.println("Source = "+file.getAbsolutePath());
            System.out.println("Destination = "+destFile.getAbsolutePath());
            FileCopier.copyFile(file,destFile);
            
            //Calculate the digest of the file
            Trigest trigest = new Trigest(destFile);
            digestOfFile = trigest.getSignature();
            
            //Form the message to be sent
            String msg = type + ":" + fileName + ":" + fileSize + ":" + abstractOfFile + ":";
            sendMsg(msg);
            
            
            //Send the length of the digest of the file
            DataOutputStream dout = new DataOutputStream(out);
            dout.writeInt(digestOfFile.length);
            System.out.println("Length of digest is"+digestOfFile.length);
            
            //Now send the digest of the file
            /*for(int i = 0; i < 1024;i++)
            {
                System.out.println(digestOfFile[i]);
            }*/
            sendMsg(digestOfFile);
            
            
        } catch (Exception ex) {
            Logger.getLogger(IPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

     
     public String receiveResponse() {
        int c, i;
        String msg="";
        char [] ch = new char[10000];	
        try {
            i=0;
            while (( c = in.read()) != '\n') {
                ch[i] = (char) c;
                i++;
                //System.out.print((char) c);
            }
            msg = new String(ch);
            msg = msg.trim();
            System.out.println("\nResponse from server: " + msg);
        }
        catch(Exception e){
            System.out.println("EXCEPTION:: receiveResponse :"+e.getMessage());
        }
        return msg;
   }
     
     private void sendLogin(String msg) {
         String response = "";
         sendMsg(msg);
         response = receiveResponse(); //0=Success -1=Pwd incorrect -2=Username incorrect
         if(response.charAt(0) == '0')
         {
             new DashBoard(this);
         }
         else JOptionPane.showMessageDialog(this,"Username or Password is incorrect.","Error",JOptionPane.ERROR_MESSAGE); 
     }
     
     private void sendForgotPwd(String msg) {
         sendMsg(msg);
         String res = receiveResponse(); //0=Success -1=Failure
         if(res.equals("0")) {
            JOptionPane.showMessageDialog(this,"Email sent.","Notification",JOptionPane.INFORMATION_MESSAGE);
         }
         else JOptionPane.showMessageDialog(this,"Email not sent. Please try later.","Error",JOptionPane.ERROR_MESSAGE);
        jTextFieldEmail.setText("");
     }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel5 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jButtonLogin = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldUname = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPasswordFieldPwd = new javax.swing.JPasswordField();
        jButtonExit = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jButtonSignup = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jButtonForgotPwd = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldEmail = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Gangshare-C Login");

        jLabel5.setFont(new java.awt.Font("Baveuse", 0, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(153, 0, 0));
        jLabel5.setText("GangShare-C");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Enter your credentials:"));

        jButtonLogin.setText("Login");
        jButtonLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoginActionPerformed(evt);
            }
        });

        jLabel1.setText("Username :");

        jLabel2.setText("Password :");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextFieldUname, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                            .addComponent(jPasswordFieldPwd))))
                .addContainerGap(19, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldUname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jPasswordFieldPwd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonLogin))
        );

        jButtonExit.setText("Exit");
        jButtonExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExitActionPerformed(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Not a member yet?"));

        jButtonSignup.setText("New User SignUp");
        jButtonSignup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSignupActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(46, Short.MAX_VALUE)
                .addComponent(jButtonSignup)
                .addGap(42, 42, 42))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(jButtonSignup)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Forgot Password?"));

        jButtonForgotPwd.setText("Submit");
        jButtonForgotPwd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonForgotPwdActionPerformed(evt);
            }
        });

        jLabel3.setText("Confirm Email Id :");

        jLabel4.setText("Enter your registered email address below and we will mail you your password.");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(63, 63, 63)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonForgotPwd, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addComponent(jLabel4)))
                .addContainerGap(29, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextFieldEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonForgotPwd))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jButtonExit, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(180, 180, 180))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(115, 115, 115)
                        .addComponent(jLabel5)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel5)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExit)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSignupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSignupActionPerformed
        // TODO add your handling code here:
        new SignUp(this,out,in).setVisible(true);
        this.setVisible(false); 
    }//GEN-LAST:event_jButtonSignupActionPerformed

    private void jButtonExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExitActionPerformed
        // TODO add your handling code here:
        /*try {
            s.close();
        }
        catch(Exception e) {
            System.out.println("EXCEPTION: "+e.getMessage());;
        }*/
        System.exit(1);
    }//GEN-LAST:event_jButtonExitActionPerformed

    private void jButtonLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoginActionPerformed
        // TODO add your handling code here:
        if(jTextFieldUname.getText().equals("") || (new String(jPasswordFieldPwd.getPassword())).equals("")) {
            JOptionPane.showMessageDialog(this,"Username and Password cannot be empty!","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }
        String msg = "02:"+jTextFieldUname.getText()+":"+RSAEncryption.encrypt(publicKey,new String(jPasswordFieldPwd.getPassword()));
        //Login Message = 02:USERNAME:PASSWORD~
        sendLogin(msg);
    }//GEN-LAST:event_jButtonLoginActionPerformed

    private void jButtonForgotPwdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonForgotPwdActionPerformed
        // TODO add your handling code here:
        if(jTextFieldEmail.getText().equals("")) {
            JOptionPane.showMessageDialog(this,"Enter email id!","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }
        String msg = "03:"+jTextFieldEmail.getText();
        //Forgot Pwd Message = 03:EMAIL~
        sendForgotPwd(msg);
    }//GEN-LAST:event_jButtonForgotPwdActionPerformed

 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonExit;
    private javax.swing.JButton jButtonForgotPwd;
    private javax.swing.JButton jButtonLogin;
    private javax.swing.JButton jButtonSignup;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPasswordField jPasswordFieldPwd;
    private javax.swing.JTextField jTextFieldEmail;
    private javax.swing.JTextField jTextFieldUname;
    // End of variables declaration//GEN-END:variables
}
