package ipserver;

import framework.EmailManager.EmailManager;
import framework.dboperations.DBOperations;
import framework.hashing.Trigest;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*; 

    
    
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
                            System.out.println("\nMessage from client123: " + msg);
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
                            else if(msg_type.equals("05"))
                                search(msg);
                            else if(msg_type.equals("100"))
                                logoutClient(msg);
                            else if(msg_type.equals("25"))
                                unpublishFile(msg, clientSocket.getInetAddress().getHostAddress());
                            else if(msg_type.equals("111"))
                                incrementHitCount(msg);
                            
                            
                        }
		}
		catch(IOException e){ 
			System.out.println("EXCEPTION: "+e.getMessage());
		}
	}

        //increment HitCount of file in DB
        public void incrementHitCount(String msg) {
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String fileName = st.nextToken();
            String IP = st.nextToken();
            DBOperations.updateHitCount(fileName,IP,IPServer.con);
            System.out.println("HitCount updated in DB.");
        }
        
        //Logout a client from the system
        private void logoutClient(String msg)
        {
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String IP = clientSocket.getInetAddress().getHostAddress();
            DBOperations.deleteAnIP(IP,IPServer.con);
        }
        
        
        //Save the published file from client in the database
    private void publishFile(String msg, InputStream in)
    {
        try {
            byte[] digest = new byte[1024];
            //Receive the length of the signature first through a DataInputStream
            DataInputStream din = new DataInputStream(in);
            int lengthOfDigest = din.readInt();
            
            //Get the digest of the file
            for(int i = 0; i < 1024;i++)
            {
                digest[i] = (byte) in.read();
            }
            
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String filename = st.nextToken();
            double filesize = Double.parseDouble(st.nextToken());
            String filesizeString = filesize+"";
            String abstractOfFile = st.nextToken();
            String ip = clientSocket.getInetAddress().toString();
            ip = ip.substring(1);
            System.out.println("IPServer: Client IP is "+ip);
            
            //Time to insert into the database
            Properties vals = new Properties();
            vals.put("name", filename);
            vals.put("size",filesizeString);
            vals.put("fileabstract",abstractOfFile);
            vals.put("digest",digest.toString());
            vals.put("ip",ip);
            vals.put("hit_count", "0");
            DBOperations.insertIntoFiles(vals,IPServer.con,digest);
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
                
                Statement stmt2 = dbCon.createStatement();
                String query = "INSERT INTO LOGIN_INFO"  ;
                query += " VALUES(\'"+uname+"\',\'"+encryptedPwd+"\',\'"+email+"\');";
                
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
                  if(!pwd.equals(RSADecryption.decrypt(table.getString(1))) ) {
                      System.out.println("Password invalid.");
                      sendResponse("-1");
                  }
                  else {
                      System.out.println("Login Successful!");
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
                String content = "Dear "+uname+",\n\nYour password is "+RSADecryption.decrypt(pwd)+"\n\nPlease keep your password safe!";
                EmailManager newEmail = new EmailManager(email,content);
                newEmail.sendEmail();
                sendResponse("0");
              }
        } 
      catch(Exception e) {
                System.out.println("EXCEPTION: "+e.getMessage());
                    sendResponse("-1");
            } 
   }
private void search(String msg) {
        try {
            byte[] fileDigest;
            StringTokenizer st = new StringTokenizer(msg, ":");
            String type = st.nextToken();
            String searchString = st.nextToken();
            Trigest trigest = new Trigest(searchString.toLowerCase());
            byte[] searchDigest = trigest.getSignature();
            String query = "select digest,name,ip,fileabstract,size,hitcount from files";
            Statement stmt = dbCon.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            ArrayList<Ranking> resultset = new ArrayList<Ranking>();
            for(int k=0;k<1024;k++)
            System.out.print(searchDigest[k]+" ");
            System.out.println();
            while(rs.next()){
                fileDigest = rs.getBytes(1);  
                System.out.println("File " + rs.getString(2) +" with digest " +fileDigest);
                int i = compareSignatures(searchDigest, fileDigest);
                int s = sumOnesInSignature(fileDigest);
                if(i > 1) {     //to filter out unmatched files
                resultset.add(new Ranking(i,rs.getString(2),rs.getString(3),rs.getString(4),rs.getDouble(5),rs.getInt(6),s));
                }
                //System.out.println("after comparing count = " + i);               
            }
            Collections.sort(resultset, Ranking.COMPARE_BY_SUMONES_HITCOUNT);
            Iterator<Ranking> it = resultset.iterator();
            
            sendSearchResult(resultset.size(),it);
            
        } catch (Exception ex) {
            Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    int compareSignatures(byte[] s1,byte[] s2) {
           int output = 0;
           int flag = 0;
           for(int i = 0; i<1024; i++){
               byte andResult = (byte)(s1[i] & s2[i]);
               //System.out.println(i+" " + s1[i]+ " " + s2[i]);
               if(andResult!=s1[i]){
                   flag = 1;
                   break;
               }
               if(andResult>0){
                   while(andResult != 0){
                       System.out.println("inside while " + andResult + "for i = " + i);
                       output += andResult & 1;
                       andResult >>= 1;
                   }
               }
               else{
                   while(andResult != 0){
                       if((andResult & (-128)) != 0){
                           output++;
                       }
                       andResult <<= 1;
                   }
               }
           }
           if(flag == 0)
               return output;
           else
               return -1;
   }

    int sumOnesInSignature(byte[] s1) {
         int count = 0;
         for(int i = 0; i<s1.length;i++){
             byte temp = s1[i];
             int c = 0;
             while(c < 8){
                 count += temp&1;
                 temp>>=1;
                 c++;
             }
         }
         return count;
    }
    
    void sendSearchResult(int resultCount, Iterator<Ranking> it) {
        int i;
        String msg;
        sendResponse("69:" + resultCount);
        System.out.println("====>Search result Ranking Order:");
        while(it.hasNext()){
                Ranking e = it.next();
                msg = "70:"+e.fileName+":"+e.ip+":"+e.abs+":"+e.size;
                System.out.println(e.fileName+" "+e.ip+" "+e.size+" "+e.sumOnes+" "+e.hitCount);
                System.out.println("Sending Search Result to client: "+msg);
                sendResponse(msg);
            }
        }

    private void unpublishFile(String msg, String hostAddress) {
        try {
            StringTokenizer st = new StringTokenizer(msg, ":");
            st.nextToken();
            String fileName = st.nextToken();
            String query = "delete from gangshare.files where ip = '" + hostAddress + "' and name = '" + fileName + "'";
            Statement stmt = dbCon.createStatement();
            stmt.executeUpdate(query);
            sendResponse("26:1");
        } catch (SQLException ex) {
            sendResponse("26:0");
            Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
}
    
    
class Ranking {

    public Integer ones;
    public String fileName;
    public String ip;
    public String abs; //abstract
    public String size;
    public Integer hitCount;
    public Integer sumOnes;
    
    Ranking(int o, String f, String i, String a, double size, int hc, int sumOnes){
        ones = new Integer(o);
        fileName = f;
        ip = i;
        abs = a;
        this.size = size+"";
        hitCount = hc;
        this.sumOnes = sumOnes;
    }
    
    public static Comparator<Ranking> COMPARE_BY_SUMONES_HITCOUNT = new Comparator<Ranking>() {
        public int compare(Ranking o1, Ranking o2) {

            Integer x1 = o1.sumOnes;
            Integer x2 = o2.sumOnes;
            int sComp = x1.compareTo(x2);

            if (sComp != 0) {
               return sComp;
            } else {
               Integer y1 = o1.hitCount;
               Integer y2 = o2.hitCount;
               return y2.compareTo(y1);
            }
       }
};
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
