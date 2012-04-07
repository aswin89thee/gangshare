package ipserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Properties;

/**
 *
 * @author Ramit
 */
class ServeClient implements Runnable {
	Socket clientSocket;
	Thread t;
        
	ServeClient(Socket s) {
		clientSocket = s;
		t = new Thread(this,"Serve Client");
		t.start();
	}
        
    	public void run() {
		int c, i=0;
                char [] ch = new char[1000];
                String msg;
		try {
			InetAddress cia = clientSocket.getInetAddress();
			System.out.println("Client " +cia.getHostAddress()+ " connected.");
			InputStream in = clientSocket.getInputStream();
			while (( c = in.read()) != -1) {
                          ch[i] = (char) c;
                          i++;
			  System.out.print((char) c);
			}
			msg = new String(ch);
                        msg = msg.trim();
                        System.out.print("Message from client: " + msg);
			//clientSocket.close();
		}
		catch(IOException e){ 
			System.out.println("EXCEPTION: "+e.getMessage());
		}
	}
        
}

public class IPServer {

	static int serverPort = 6000;
        //Database
        private static final String DB = "gangshare",
                                HOST = "jdbc:mysql://localhost/",
                                ACCOUNT = "root",
                                PASSWORD = "password",
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

        //select customer record
private static void selectLogin() {
    try{
           System.out.println("Selecting....");
           Statement stmt = con.createStatement();
           String query = "SELECT * FROM login_info"; 
                    
              System.out.println(query);
              ResultSet table = stmt.executeQuery(query);
              
              
              if(!table.next())  {
                  System.out.println("No Row.");
              }
              else {
                 System.out.println("Login Id: " + table.getInt(1));
                 System.out.println("Username: " + table.getString(2));
                 System.out.println("Password: " + table.getString(3));
                 System.out.println("Email: " + table.getString(4)); 
               }
            }
            catch(Exception e) {
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
			new ServeClient(s);
		}
	}
}
