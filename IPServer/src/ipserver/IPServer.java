package ipserver;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 *
 * @author Ramit
 * 
 * 
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
        
    	public void run() {
		int c, i;
                try {
			InetAddress cia = clientSocket.getInetAddress();
			System.out.println("Client " +cia.getHostAddress()+ " connected.");
			in = clientSocket.getInputStream();
                        out = clientSocket.getOutputStream();
			
                        while(true) {
                            i = 0;
                            char [] ch = new char[1000];
        	
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
                        }
		}
		catch(IOException e){ 
			System.out.println("EXCEPTION: "+e.getMessage());
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
                String pwd = st.nextToken();
                String email = st.nextToken();
                
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
              String pwd = st.nextToken();
                
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
		initDB();
                while(true) {
                        Socket s = ss.accept();
			new ServeClient(s,con);
		}
	}
}
