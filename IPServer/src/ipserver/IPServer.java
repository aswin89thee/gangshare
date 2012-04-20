package ipserver;

import framework.dboperations.DBOperations;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ramit
 */
class ServeClient implements Runnable {
	Socket clientSocket;
	Thread t;
        String msg;
        Connection dbCon;
        InputStream in;
        OutputStream out;
		
	ServeClient(Socket s, Connection con) {
		clientSocket = s;
                dbCon = con;
                t = new Thread(this,"Serve Client");
		t.start();
	}
        
        public void sendInitialMessage(){
            String str = "00";
            str += ":"+IPServer.publicKey;
            sendResponse(str);
        }
    	public void run() {
		int c, i;
                try {
			InetAddress cia = clientSocket.getInetAddress();
			System.out.println("Client " +cia.getHostAddress()+ " connected.");
			in = clientSocket.getInputStream();
                        out = clientSocket.getOutputStream();
			sendInitialMessage();
                        while(true) {
                            i = 0;
                            char [] ch = new char[10000];
                            while (( c = in.read()) != '\n') {
                                ch[i] = (char) c;
                                i++;
                                System.out.print((char) c);
                            }
                            msg = new String(ch);
                            msg = msg.trim();
                            System.out.println("\nMessage from client: " + msg);
                            StringTokenizer st = new StringTokenizer(msg,":");
                            String msg_type = st.nextToken();
                            if(msg_type.equals("01"))
                                insertLogin(msg);
                            else if(msg_type.equals("02"))
                                verifyLogin(msg);
                            else if(msg_type.equals("03"))
                                verifyForgotPwd(msg);
                            else if(msg_type.equals("04"))
                                publishFile(msg,in);
                            else if(msg_type.equals("100"))
                                logoutClient(msg);
                        }
		}
		catch(IOException e){ 
			System.out.println("EXCEPTION: "+e.getMessage());
		}
	}
        
        //Logout a client from the system
        private void logoutClient(String msg)
        {
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String IP = st.nextToken();
            DBOperations.deleteAnIP(IP,IPServer.con);
        }
        
        
        //Save the published file from client in the database
    private void publishFile(String msg, InputStream in)
    {
        try {
            char[] digest = new char[1024];
            //Receive the length of the signature first through a DataInputStream
            DataInputStream din = new DataInputStream(in);
            int lengthOfDigest = din.readInt();
            
            //Get the digest of the file
            for(int i = 0; i < lengthOfDigest;i++)
            {
                digest[i] = (char)in.read();
            }
            
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String filename = st.nextToken();
            double filesize = Integer.parseInt(st.nextToken());
            String abstractOfFile = st.nextToken();
            String ip = clientSocket.getInetAddress().toString();
            
            //Time to insert into the database
            Properties vals = new Properties();
            vals.put("name", filename);
            vals.put("size",filesize);
            vals.put("fileabstract",abstractOfFile);
            vals.put("digest",digest.toString());
            vals.put("ip",ip);
            DBOperations.insertIntoFiles(vals,IPServer.con);
            
            
            
        } catch (IOException ex) {
            Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
        
        //Send response to client
    private void sendResponse(String res) {
            try {
                res = res.concat("\n");
                byte bmsg[] = res.getBytes();
                out.write(bmsg);
                System.out.println("Message sent to Client: "+ res);
            } 
            catch(Exception e) {
                System.out.println("EXCEPTION: "+e.getMessage());
            }
    }
        
    //Insert new user data in login_info table and return 0=Success -1=Failure to client
    private void insertLogin(String msg) {
        System.out.println("Inserting...");
        try {
                StringTokenizer st = new StringTokenizer(msg,":");
                int i;
                
                // MESSAGE_TYPE:USERNAME:PASSWORD:EMAIL~ (Delimiter = :)
                String msg_type = st.nextToken();
                String uname = st.nextToken();
                String encryptedPwd = st.nextToken();
                String email = st.nextToken();
                String pwd = RSADecryption.decrypt(encryptedPwd);
                
                Statement stmt2 = dbCon.createStatement();
                String query = "INSERT INTO LOGIN_INFO"  ;
                query += " VALUES(\'"+uname+"\',\'"+pwd+"\',\'"+email+"\');";
                
                System.out.println(query);
                i = stmt2.executeUpdate(query);
                
                if(i > 0) {
                    System.out.println("1 record inserted.");
                    sendResponse("0");
                }
                else {
                    System.out.println("No record inserted.");
                    sendResponse("-1");
                }
        }
        catch(Exception e){
            System.out.println("EXCEPTION: "+e.getMessage());
            sendResponse("-1");
        }    
    }        
    
//verify login credentials 0=Success -1=Pwd incorrect -2=Username incorrect
private void verifyLogin(String msg) {
    try{
              StringTokenizer st = new StringTokenizer(msg,":");
              // 02:USERNAME:PASSWORD~
              String msg_type = st.nextToken();
              String uname = st.nextToken();
              String encryptedPwd = st.nextToken();
              String pwd = RSADecryption.decrypt(encryptedPwd);
                  
              System.out.println("Selecting....");
              Statement stmt = dbCon.createStatement();
              String query = "SELECT password FROM login_info where username = \'"+uname+"\';"; 
                    
              System.out.println(query);
              ResultSet table = stmt.executeQuery(query);
              
              if(!table.next())  {
                  System.out.println("Username invalid.");
                  sendResponse("-2");
              }
              else {
                  if(!pwd.equals(table.getString(1)) ) {
                      System.out.println("Password invalid.");
                      sendResponse("-1");
                  }
                  else {
                      System.out.println("Login SUccessful!");
                      sendResponse("0");
                  } 
                  
               }
            }
            catch(Exception e) {
                System.out.println("EXCEPTION: "+e.getMessage());
            } 
   }

private void verifyForgotPwd(String msg) {
    try{
              StringTokenizer st = new StringTokenizer(msg,":");
              // 03:EMAIL~
              String msg_type = st.nextToken();
              String email = st.nextToken();
                
              System.out.println("Selecting....");
              Statement stmt = dbCon.createStatement();
              String query = "SELECT Username,password FROM login_info where email = \'"+email+"\';"; 
                    
              System.out.println(query);
              ResultSet table = stmt.executeQuery(query);
              
              if(!table.next())  {
                  System.out.println("Email not registered.");
                  sendResponse("-1");
              }
              else {
              // SEND USERNAME AND PASSWORD IN MAIL
                String uname = table.getString(1);
                String pwd = table.getString(2);
                System.out.println("PASSWORD REMINDER: Username: "+uname+"Password: "+pwd);
                sendResponse("0");
              }
        } 
      catch(Exception e) {
                System.out.println("EXCEPTION: "+e.getMessage());
                    sendResponse("-1");
            } 
   }
}


public class IPServer {

	static int serverPort = 6000;
        static String publicKey;
        //Database
        private static final String DB = "gangshare",
                                HOST = "jdbc:mysql://localhost/",
                                ACCOUNT = "root",
                                PASSWORD = "mysql",
                                DRIVER = "com.mysql.jdbc.Driver";
        public static Connection con;

            //Method to initialize DB connections
        private static void initDB(){
            try {
            //JDBC Connectivity part       
                Properties props = new Properties();
                props.setProperty("user", ACCOUNT);
                props.setProperty("password", PASSWORD);
               
                  // load driver and prepare to access
                Class.forName(DRIVER).newInstance();
                con = DriverManager.getConnection(HOST + DB, props);
                  
                System.out.println("JDBC Initialized.");
                //selectLogin();
                } catch (Exception e) {
			System.out.println("EXCEPTION: "+e.getMessage());
                }
        }

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		ServerSocket ss = new ServerSocket(serverPort);
		System.out.println("ServerSocket created. Waiting for client to connect to port "+serverPort+"...");
		GenerateRSAKeys generateRSAKeys = new GenerateRSAKeys();
                generateRSAKeys.generate("C:/RSA/Public", "C:/RSA/Private");
                publicKey = readFileAsString("C:/RSA/Public");
                initDB();
                while(true) {
                        Socket s = ss.accept();
			new ServeClient(s,con);
		}
	}
        
        private static String readFileAsString(String filePath) throws java.io.IOException{
            StringBuffer fileData = new StringBuffer(1000);
            BufferedReader reader = new BufferedReader(
                    new FileReader(filePath));
            char[] buf = new char[1024];
            int numRead=0;
            while((numRead=reader.read(buf)) != -1){
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();
            //System.out.println(fileData.toString());
            return fileData.toString();
        }
}
