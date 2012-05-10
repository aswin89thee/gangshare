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

class Coordinate{
    public int x;
    public int y;

    Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    Coordinate() {
        this.x = 0;
        this.y = 0;
    }
}
   
class HashSpace{
    Coordinate start;
    Coordinate end;
    String IP;

    HashSpace(Coordinate start, Coordinate end, String ip) {
        this.start = start;
        this.end = end;
        this.IP = ip;
    }

    HashSpace() {
        this.start = new Coordinate();
        this.end = new Coordinate();
        this.IP = "";
    }
}

    
    
    
    
    
    
class ServeClient implements Runnable {
	Socket clientSocket;
	Thread t;
        String msg;
        Connection dbCon;
        InputStream in;
        OutputStream out;
                
        InetAddress threadHost;
        Socket s2;
        OutputStream outgoing;
                
                
	ServeClient(Socket s, Connection con) {
        try {
            clientSocket = s;
            dbCon = con;
            outgoing = s.getOutputStream();
            t = new Thread(this,"Serve Client");
            t.start();
        } catch (IOException ex) {
            Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
                
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
                            
                        }
		}
		catch(IOException e){ 
			System.out.println("EXCEPTION: "+e.getMessage());
		}
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
        
      private void sendHashResponse(String res) {
            try {
                res = res.concat("\n");
                byte bmsg[] = res.getBytes();
                outgoing.write(bmsg);
                System.out.println("Message sent to Client: "+ res);
            } 
            catch(Exception e) {
                System.out.println("EXCEPTION: "+e.getMessage());
            }
    }
        
        private void sendHashResponse(String res, OutputStream out) {
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
              String query = "SELECT password FROM gangshare.login_info where username = \'"+uname+"\';"; 
                    
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
                      System.out.println("Before hashing");
                      String ip = defineHashSpace(clientSocket.getInetAddress().getHostAddress());
                      if(ip.equals("nothing"))
                          System.out.println("IP IS NULL");
                      
                      System.out.println("Login Successful!");
                      IPServer.loggedUsers.add(clientSocket.getInetAddress().getHostAddress());
                      sendResponse("0");
                      sendHashSpace(ip,1);
                      broadcastHashSpace(clientSocket.getInetAddress().getHostAddress());
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
              String query = "SELECT Username,password FROM gangshare.login_info where email = \'"+email+"\';"; 
                    
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
                
                System.out.println("PASSWORD REMINDER: Username: "+uname+"Password: "+pwd);
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
            String query = "select digest,name,ip,fileabstract, size from gangshare.files";
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
                if(i > 1) {     //to filter out unmatched files
                resultset.add(new Ranking(i,rs.getString(2),rs.getString(3),rs.getString(4),rs.getDouble(5)));
                }
                //System.out.println("after comparing count = " + i);               
            }
            Collections.sort(resultset, Ranking.COMPARE_BY_ONES);
            Iterator<Ranking> it = resultset.iterator();
            //while(it.hasNext()){
            //    System.out.println(it.next().ones);
            //}
            
            sendSearchResult(resultset.size(),it);
            System.out.println();
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

    void sendSearchResult(int resultCount, Iterator<Ranking> it) {
        int i;
        String msg;
        sendResponse("69:" + resultCount);
        while(it.hasNext()){
                Ranking e = it.next();
                msg = "70:"+e.fileName+":"+e.ip+":"+e.abs+":"+e.size;
                System.out.println("Sending Search Result to client: "+msg);
                sendResponse(msg);
            }
        }

    void sendHashSpace(String ipToContact,int flag) {
        int i;
        String msg;
        Iterator<HashSpace> it = IPServer.hashList.iterator(); 
        
        try{
        String ip = clientSocket.getInetAddress().getHostAddress();
        s2 = new Socket(ip,20000) ;
        System.out.println("Host " + ip + " connected to client on port 20000");
        OutputStream outgoing = s2.getOutputStream();
        
        if(flag==1)
           sendHashResponse("76:"+ipToContact+":"+getHash(s2.getInetAddress().getHostAddress()), outgoing);
        
        sendHashResponse("77:" + IPServer.hashList.size(), outgoing);
        while( it.hasNext()){
                HashSpace e = it.next();
                msg = "78:"+e.start.x+":"+e.start.y+":"+e.end.x+":"+e.end.y+":"+ e.IP;
                System.out.println("Sending HashSpace to client: "+msg);
                sendHashResponse(msg, outgoing);
            }
        //s2.close();
        }
        catch(Exception e){System.out.println(e.getMessage());}
        }
    
    void broadcastHashSpace(String ipNotToSend) {
        int i;
        String msg;
        Iterator<String> it1 = IPServer.loggedUsers.iterator(); 
        
        try{
            while(it1.hasNext()) {
                String ip = it1.next();
                if(ip.equals(ipNotToSend)) continue;
                s2 = new Socket(ip,20000) ;
                System.out.println("Server " + ip + " connected to client on port 20000");
                OutputStream outgoing = s2.getOutputStream();
                Iterator<HashSpace> it = IPServer.hashList.iterator(); 
                sendHashResponse("77:" + IPServer.hashList.size(), outgoing);
                while( it.hasNext()){
                    HashSpace e = it.next();
                    msg = "78:"+e.start.x+":"+e.start.y+":"+e.end.x+":"+e.end.y+":"+ e.IP;
                    System.out.println("Sending HashSpace to client: "+msg);
                    sendHashResponse(msg, outgoing);
                }
            }
            //s2.close();
        }
        catch(Exception e){System.out.println(e.getMessage());}
        }
    
    public String getHash(String ip) {
        Iterator<HashSpace> it = IPServer.hashList.iterator(); 
        while( it.hasNext()){
                HashSpace e = it.next();
                if(e.IP.equals(ip)) {
                    String msg = e.start.x+":"+e.start.y+":"+e.end.x+":"+e.end.y;
                System.out.println("Sending HashSpace to client: "+msg);
                return msg;
                }
            }
        return "nothing";
    }
    
    // the function that determines which hash space to be split / reassigned
    private String defineHashSpace(String ip) {
        if(IPServer.hashList.isEmpty()){
            Coordinate start = new Coordinate(0,0);
            Coordinate end = new Coordinate(4096,4096);
            HashSpace h = new HashSpace(start, end, ip);
            IPServer.hashList.add(h);
            System.out.println("New1 ("+h.start.x+","+h.start.y+"), "+"("+h.end.x+","+h.end.y+")");
            return "nothing";
        }
        else{
            int duplicate = hasDuplicate();
            if(duplicate == -1){
                int largest = findLargest();
                HashSpace hash = IPServer.hashList.get(largest);
                Coordinate newStart = new Coordinate();
                Coordinate newEnd = new Coordinate();
                if(hash.end.x - hash.start.x > hash.end.y - hash.start.y){
                    newStart.x = (hash.start.x+hash.end.x)/2;
                    newStart.y = hash.start.y;
                    newEnd.x = hash.end.x;
                    newEnd.y = hash.end.y;
                    hash.end.x = newStart.x -1;
                }
                else{
                    newStart.x = hash.start.x;
                    newStart.y = (hash.start.y + hash.end.y)/2;
                    newEnd.x = hash.end.x;
                    newEnd.y = hash.end.y;
                    hash.end.y = newStart.y -1;
                }

                IPServer.hashList.add(new HashSpace(newStart, newEnd, ip));
                System.out.println("New2 ("+newStart.x+","+newStart.y+"), "+"("+newEnd.x+","+newEnd.y+")");
                System.out.println("New3 ("+hash.start.x+","+hash.start.y+"), "+"("+hash.end.x+","+hash.end.y+")");

                return hash.IP;
            }
            else{
                String retValue = IPServer.hashList.get(duplicate).IP;
                IPServer.hashList.get(duplicate).IP = ip;
                return retValue;
            }
        }   
    }
    
      //Logout a client from the system
        private void logoutClient(String msg)
        {
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String IP1 = clientSocket.getInetAddress().getHostAddress();
            System.out.println("Client "+IP1+" logging out");
            
            if(IPServer.loggedUsers.size() == 1){
                IPServer.loggedUsers.clear();
                IPServer.hashList.clear();
                System.out.println("last client");
                sendResponse("133:");
                return;
            }
            //Find IP of another peer to assign this peer's hash space
            String IP2 = findSmallest(IP1);
            System.out.println("Client "+IP2+" to replace logging out client");
            
            //Assign P1's hash space to P2
            assignHashSpace(IP2,IP1);
            System.out.println("assignHashSpace");
            
            
            //Broadcast the hashspace to all the peers
            sendHashSpace(IP2,0);   //Send the new hash space to IP2 first
            System.out.println("sendHashSpace");
            
            broadcastHashSpace(IP2);
            System.out.println("broadcastHashSpace");
            
            //Ask p2 to get file info from p1
            getFileInfoBeforeLogout(IP2,IP1);
            System.out.println("getFileInfoBeforeLogout");
            
            //Tell p1 that it is safe to logout
            getLost(IP1);
            System.out.println("getLost");
            //Remove IP1 from loggedUsers
            Iterator<String> it = IPServer.loggedUsers.iterator();
            IPServer.loggedUsers.remove(IP1);
            System.out.println("removed");
            
            //Broadcast to peers asking them to remove files of IP1 from their database
            String removeAnIPString = "93:"+IP1+"\n";
            broadCastMsg(removeAnIPString);
            System.out.println("broadCastMsg");
        }
        
        //Broadcast a message to all the peers
        public void broadCastMsg(String bcastmsg)
        {
            Iterator<String> bcastIterator = IPServer.loggedUsers.iterator();
            
            //Send this message to the peers one by one
            while(bcastIterator.hasNext())
            {
            try {
                
                String currentPeerIP = bcastIterator.next();
                Socket curPeerSocket = new Socket(currentPeerIP,20000);
                OutputStream curPeerOut = curPeerSocket.getOutputStream();
                curPeerOut.write(bcastmsg.getBytes());
                
            } catch (UnknownHostException ex) {
                Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);}
              catch (IOException ex) {
                Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);}
            }
        }
        
        //Contact p2's PeerThread, ask it to get file info from p1 and then get ack from it
        private void getFileInfoBeforeLogout(String P2,String P1)
        {
        try {
            
            Socket p2socket = new Socket(P2,20000);
            OutputStream p2out = p2socket.getOutputStream();
            InputStream p2in = p2socket.getInputStream();
            msg = "122:"+P1+"\n";
            p2out.write(msg.getBytes());
            
            //P2 will get the file info from p1 and send an ack to the server
            int i = 0;
            int c;
            char [] ch = new char[1000];
            while (( c = p2in.read()) != '\n') {
                ch[i] = (char) c;
                i++;
            }
            //Since this is just an ack, we are not going to do anything more with this data received
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
        
        //Sent by server after hash space and file info was transferred to IP2. So IP1 is safe to gtfo :)
        private void getLost(String P1)
        {
        try {
            
            String getLostMsg = "133:"+"\n";
            out.write(getLostMsg.getBytes());
            
        } catch (IOException ex) {
            Logger.getLogger(ServeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
        
    
        //Assign the hash space of one IP to another
        private void assignHashSpace(String IP2,String IP1)
        {
            Iterator<HashSpace> it = IPServer.hashList.iterator();
            while(it.hasNext())
            {
                HashSpace temp = it.next();
                if(temp.IP.equals(IP1))
                {
                    temp.IP = IP2;
                }
            }
        }
    private int area(HashSpace h){
        int len = h.end.x - h.start.x;
        int breadth = h.end.y - h.start.y;
        return len*breadth;
    }
    
    
    private int findLargest() {
    
        Iterator<HashSpace> it = IPServer.hashList.iterator();
        HashSpace max = new HashSpace();
        int maxIndex=0;
        int index = -1;
        while(it.hasNext()){
            index++;
            HashSpace temp = it.next();
            if(area(temp)>area(max)){
                max = temp;
                maxIndex = index;
            }
        }
        return maxIndex;

    }

    
    
    //Find the IP of the peer that currently has the smallest hash space area
    private String findSmallest(String exceptMe) 
    {
        int minIndex = 0;
        
        Iterator<HashSpace> it = IPServer.hashList.iterator();
        
        //Find the total area occupied by the first element in the hashList. Needed to compare and find min
        int firstArea = 0;
        HashSpace min = IPServer.hashList.get(0);
        String smallestIP = min.IP;
        Iterator<HashSpace> firstIterator = IPServer.hashList.iterator();
        while(firstIterator.hasNext())
        {
            HashSpace temp = firstIterator.next();
            if(temp.IP.equals(min.IP))
            {
                firstArea += area(temp);
            }
        }
        
        //int minArea = firstArea;
        int minArea = 17572864;
        int index = -1;
        while(it.hasNext())
        {
            index++;
            HashSpace temp = it.next();
            if(temp.IP.equals(exceptMe))
            {
                continue;
            }
            //iterate through the hashList and find the total area occupied by this peer
            Iterator<HashSpace> it2 = IPServer.hashList.iterator();
            int totalarea = 0;
            while(it2.hasNext())
            {
                HashSpace temp1 = it2.next();
                if(temp1.IP.equals(temp.IP))
                {
                    totalarea += area(temp1);
                }
            }
            if(totalarea < minArea)
            {
                minArea = totalarea;
                minIndex = index;
                min = temp;
                smallestIP = temp.IP;
            }
            
        }
        
        return smallestIP;
    }
    private int hasDuplicate() {
        Iterator<HashSpace> it = IPServer.hashList.iterator();
        int returnValue = -1;
        Map hash = new LinkedHashMap();
        int index = 0;
        while(it.hasNext()){
            HashSpace h = it.next();
            if(hash.containsKey(h.IP)){
                returnValue = index;
                break;
            }
            else{
                hash.put(h.IP, new Integer(0));
            }
            index++;
        }
        
        return returnValue;
    }
}
    
    
class Ranking {

    public Integer ones;
    public String fileName;
    public String ip;
    public String abs; //abstract
    public String size;
    
    Ranking(int o, String f, String i, String a, double size){
        ones = new Integer(o);
        fileName = f;
        ip = i;
        abs = a;
        this.size = size+"";
    }
    
    public static Comparator<Ranking> COMPARE_BY_ONES = new Comparator<Ranking>() {
        public int compare(Ranking one, Ranking other) {
            return other.ones.compareTo(one.ones);
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
        public static ArrayList<HashSpace> hashList = new ArrayList<HashSpace>();
        public static ArrayList<String> loggedUsers;
        
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
                loggedUsers = new ArrayList<String>();
                
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
