/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ipclient;

import framework.dboperations.DBOperations;
import framework.fileoperations.FileCopier;
import framework.hashing.Trigest;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author Aswin
 */

class ProcessRequest implements Runnable
{
    Thread t;
    Socket s;
    InputStream in;
    OutputStream out;
    IPClient parent;
    int peerport;
    
    ProcessRequest(Socket s,IPClient parent, int peerport)
    {
        this.peerport = peerport;
        this.parent = parent;
        this.s = s;
        t = new Thread(this);
        t.start();
    }
    public void run()
    {
        try {
            
            in = s.getInputStream();
            out = s.getOutputStream();
            
            
            //Get the request from peer
                
                //while(true){
                    int i = 0;
                    int c;
                    char [] ch = new char[10000];
                    while (( c = in.read()) != '\n') {
                        ch[i] = (char) c;
                        i++;
                        System.out.print((char) c);
                    }
                    String response = new String(ch);
                    response = response.trim();
                    StringTokenizer st = new StringTokenizer(response,":");
                    String type = st.nextToken();

                    if(type.equals("50"))   //Request for a file. Should reply with the full file contents
                    {
                        String fileName = st.nextToken();
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
                        incrementHitCount(response);

                    }
                    else if(type.equals("148"))
                    {
                        storePublishedFileInfo(response);
                    }
                    else if(type.equals("93"))
                    {
                        deleteFilesOfAnIP(response);
                    }
                    else if(type.equals("122")) //message from server asking it to get file info from p1 since p1 is logging out
                    {
                        getAllFileInfoAndAck(response);
                    }
                    else if(type.equals("130")) //Request from p2 for entire db so that p1 can logout
                    {
                        System.out.println("calling sendAllFileInfo");
                        sendAllFileInfo(response);
                    }
                    else if(type.equals("55")) {        //if msg_type is request for file info
                        sendFileInfo(response,out);
                    }
                    else if(type.equals("25")){         // unpublish the file from the client
                        unpublishFile(response, s.getInetAddress().getHostAddress());
                    }
                    else if(type.equals("05"))
                        search(response);
                    //else if(type.equals("111"))
                      //  incrementHitCount(response);
                    else
                        receiveHashCode(response);          //receive hash code from Server
                    //s.close();
                //}
                
            
        } catch (IOException ex) {
            Logger.getLogger(ProcessRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //increment HitCount of file in DB
        public void incrementHitCount(String msg) {
            System.out.println("HitCount updating in DB.");
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String fileName = st.nextToken();
            //String IP = st.nextToken();
            DBOperations.updateHitCount(fileName,IPClient.con);
            System.out.println("HitCount updated in DB.");
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
            Statement stmt = IPClient.con.createStatement();
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
                resultset.add(new Ranking(rs.getString(2),rs.getString(3),rs.getString(4),rs.getDouble(5),rs.getInt(6),s));
                }
                //System.out.println("after comparing count = " + i);               
            }
            //Collections.sort(resultset, Ranking.COMPARE_BY_SUMONES_HITCOUNT);
            Iterator<Ranking> it = resultset.iterator();
            
            sendSearchResult(resultset.size(),it);
            
        } catch (Exception ex) {
            System.out.println("EXCEPTION: search() "+ex.getMessage());
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
 
        String msg;
        sendResponse("69:" + resultCount);
        System.out.println("====>Search result Ranking Order:");
        while(it.hasNext()){
                Ranking e = it.next();
                msg = "70:"+e.fileName+":"+e.ip+":"+e.abs+":"+e.size+":"+e.hitCount+":"+e.sumOnes;
                System.out.println("Sending Search Result to client: "+msg);
                sendResponse(msg);
            }
        }
    
    private void sendResponse(String res) {
            try {
                res = res.concat("\n");
                byte bmsg[] = res.getBytes();
                //System.out.println("Message sent to Client: "+ res);
                out.write(bmsg);
                
            } 
            catch(Exception e) {
                System.out.println("EXCEPTION: "+e.getMessage());
            }
    }
    
    private void unpublishFile(String msg, String hostAddress) {
        try {
            StringTokenizer st = new StringTokenizer(msg, ":");
            st.nextToken();
            String fileName = st.nextToken();
            System.out.println("msg received in unpublish :" + msg);
            String query = "delete from gangshare_client.files where ip = '" + hostAddress + "' and name = '" + fileName + "'";
            System.out.println("query to db :" + query);
            Statement stmt = IPClient.con.createStatement();
            stmt.executeUpdate(query);
            sendMsg("26:1",out);
            System.out.println("successfully unpublished");
        } catch (SQLException ex) {
            System.out.println("Exception in unpublish file : " + ex.getMessage());
            sendMsg("26:0", out);
            Logger.getLogger(ProcessRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    //Store the info of a published file
    void storePublishedFileInfo(String response)
    {
        try {
            System.out.println("\nEntered StorePublishedFileInfo");
            StringTokenizer st = new StringTokenizer(response, ":");
            String type = st.nextToken();
            String fileName = st.nextToken();
            String fileSize = st.nextToken();
            String abstractOfFile = st.nextToken();
            //System.out.println("extracted msgs");
            byte[] digest = new byte[1024];
            for(int i = 0; i < 1024 ; i++)
            {
                 digest[i] = (byte) in.read();
                 System.out.print(digest[i]);
            }
            System.out.println("\ndigest extracted");
            //Time to insert into the database
            Properties vals = new Properties();
            vals.put("name", fileName);
            vals.put("size",fileSize);
            vals.put("fileabstract",abstractOfFile);
            vals.put("digest",digest.toString());
            vals.put("ip",s.getInetAddress().getHostAddress());
            DBOperations.insertIntoFiles(vals,IPClient.con,digest);
            System.out.println("Inserted successfully");
        }catch (IOException ex) {
            System.out.println(" Exception in storePublishedFileInfo :" + ex.getMessage());
                Logger.getLogger(ProcessRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
       
    }
    
    //Delete the files of an IP. Called after that IP logs out
    public void deleteFilesOfAnIP(String message)
    {
        StringTokenizer st = new StringTokenizer(message, ":");
        String type = st.nextToken();
        String IPToDelete = st.nextToken();
        DBOperations.deleteAnIP(IPToDelete, IPClient.con);
    }
    
    //Executed by P1, in response to request by P2 for all file info so that p1 can logout
    public void sendAllFileInfo(String response)
    {
        try {
            System.out.println("entered sendAllFileInfo");
            String query = "select digest,name,ip,fileabstract,size,hitcount from files";
            Statement stmt = IPClient.con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            //Form the message to be sent
            int counter = 0;
            
            while(rs.next()){
                counter++;
            }
            sendMsg(counter+"",out);   //send the count of files
            //query = "select digest,name,ip,fileabstract, size from files";
            //stmt = IPClient.con.createStatement();
            rs = stmt.executeQuery(query);

            while(rs.next()){
                //45:filename:filesize:ip:fileabstract:digest
                String fname = rs.getString(2);
                double fsize = rs.getDouble(5);
                String ip = rs.getString(3);
                String fabstract = rs.getString(4);
                int hitcount = rs.getInt(6);
                byte[] digestOfFile = rs.getBytes(1);
                
                String outgoingMsg = "45:"+fname+":"+fsize+":"+ip+":"+fabstract+":"+hitcount+":";    //send each file info in a loop
                sendMsg(outgoingMsg,out);
                //sendMsg(digestOfFile,out);
                out.write(digestOfFile);
            }
            
        } catch (Exception ex) {
            Logger.getLogger(IPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    //Gets all the file info from a peer P1 so that it can logout safely
    public void getAllFileInfoAndAck(String response)
    {
        try {
            
            StringTokenizer st = new StringTokenizer(response,":");
            System.out.println("Msg from server - getAllfileInfoAck : " + response);
            String type = st.nextToken();
            String IP1 = st.nextToken();
            Socket p1socket = new Socket(IP1,peerport);
            InputStream p1in = p1socket.getInputStream();
            OutputStream p1out = p1socket.getOutputStream();
            //Send a request to p1 requesting all file info
            String msg = "130:"+"\n";
            p1out.write(msg.getBytes());
            System.out.println("Sending 130 message to P1");
            //get the count of the db table entries
            String countMsg = receiveMsg(p1in);
            
            System.out.println("returned from receiveMsg : "+ countMsg + " : " + countMsg.length());
            int count = Integer.parseInt(countMsg);
            
            //Receive the files table entries in a loop
            for(int j=0 ; j < count ; j++)
                    receiveFileInfoWithHitcount(msg, p1in);
            
            //Ack the server that it has got all the file info and p1 can logout
            String ackmsg = "99:"+"\n";
            out.write(ackmsg.getBytes());
            System.out.println(" Msg sent to server - getallfileinfoack : " + ackmsg);
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(ProcessRequest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ProcessRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //to receive one record of file DB info and add to DB from peer
     private void receiveFileInfoWithHitcount(String msg, InputStream in)
    {
        try {
            int c, i;
            msg="";
            char [] ch = new char[10000];	
            i=0;
            while (( c = in.read()) != '\n') {
                ch[i] = (char) c;
                i++;
            }
            //System.out.print((char) c);
            msg = new String(ch);
            msg = msg.trim();
            System.out.println("message received in peer : "+msg);
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String filename = st.nextToken();
            double filesize = Double.parseDouble(st.nextToken());
            String filesizeString = filesize+"";
            String ip = st.nextToken();
            String abstractOfFile = st.nextToken();
            int hitcount = Integer.parseInt(st.nextToken());
            //byte[] fdigest = st.nextToken().getBytes();
            byte[] digest = new byte[1024];
            for(int n = 0; n < 1024 ; n++)
            {
                digest[n] = (byte) in.read();
            }
            
            //ip = ip.substring(1);
            System.out.println("IPServer: Client IP is "+ip);
            
            //Time to insert into the database
            Properties vals = new Properties();
            vals.put("name", filename);
            vals.put("size",filesizeString);
            vals.put("fileabstract",abstractOfFile);
            vals.put("digest",digest.toString());
            vals.put("ip",ip);
            vals.put("hitcount",hitcount+"");
            DBOperations.insertIntoFilesWithHitCount(vals,IPClient.con,digest);
            System.out.println("File info inserted successfully!");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    public void receiveHashCode(String msg) {
        int c, i;
        
        StringTokenizer st = new StringTokenizer(msg,":");
        String msg_type = st.nextToken();
        
        parent.hashList = new ArrayList<HashSpace>();        
        char [] ch = new char[1000];
        
        System.out.println("====>"+msg+": "+msg.length());
        
        try {
            if(msg_type.equals("76")) {     //receive IP to Contact from server
                parent.ipToContact = st.nextToken();
                if(!parent.ipToContact.equals("nothing")) {
                    System.out.println("Comes in!: "+ parent.ipToContact);
                    int x1 = Integer.parseInt(st.nextToken());
                    int y1 = Integer.parseInt(st.nextToken());
                    int x2 = Integer.parseInt(st.nextToken());
                    int y2 = Integer.parseInt(st.nextToken());
                    requestFileInfo(parent.ipToContact,new Coordinate(x1,y1),new Coordinate(x2,y2));
                }
                
                msg = "";
                ch = new char[1000];	
                i = 0;
                while (( c = in.read()) != '\n') {
                    ch[i] = (char) c;
                    i++;
                    System.out.print((char) c);
                }
                
                msg = new String(ch);
                msg = msg.trim();
                //System.out.println("\nMessage from server: " + msg);
                st = new StringTokenizer(msg,":");
                msg_type = st.nextToken();
            }
            int hashCount = Integer.parseInt(st.nextToken());
            if(msg_type.equals("77") && hashCount != 0){        //receive hash table count from server
                parent.hashList.clear();
                for(int k=0; k < hashCount; k++) {
                    msg="";
                    ch = new char[1000];	
                    i = 0;
                    while (( c = in.read()) != '\n') {
                        ch[i] = (char) c;
                        i++;
                //System.out.print((char) c);
                    }
                    msg = new String(ch);
                    msg = msg.trim();
                    System.out.println("\nResponse from server: " + msg);
                
                    st = new StringTokenizer(msg,":");
                    msg_type = st.nextToken();                  //receive hash table entries in a loop
                    int x1 = Integer.parseInt(st.nextToken());
                    int y1 = Integer.parseInt(st.nextToken());
                    int x2 = Integer.parseInt(st.nextToken());
                    int y2 = Integer.parseInt(st.nextToken());
                    String ip = st.nextToken();
                    parent.hashList.add(new HashSpace(new Coordinate(x1,y1),new Coordinate(x2,y2),ip));
                    
                }
                Iterator<HashSpace> it = parent.hashList.iterator(); 
                while( it.hasNext()){
                    HashSpace e = it.next();
                    e.printHashSpace();
                }
            }
        }
        catch(Exception e){
            System.out.println("EXCEPTION:: receiveResponse :"+e.getMessage());
        }
   }
    
    
    public void requestFileInfo(String ipToContact,Coordinate start, Coordinate end) {
         try {
            OutputStream pout;
            InputStream pin;
            
            Socket peerSocket = new Socket(ipToContact,peerport);
            String msg = "55:"+start.x+":"+start.y+":"+end.x+":"+end.y+"\n";    //55:coordinates
            System.out.println("After connect: "+msg);
            pout = peerSocket.getOutputStream();
            pin = peerSocket.getInputStream();
            
            //Request the server for the file info by sending the command msg
            pout.write(msg.getBytes());
            
            //receive response
            int c, i;
            msg="";
            char [] ch = new char[10000];	
            i=0;
            while (( c = pin.read()) != '\n') {
                ch[i] = (char) c;
                i++;
            }
            //System.out.print((char) c);
            msg = new String(ch);
            msg = msg.trim();
            System.out.println("Message received : " + msg);
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            if(msg_type.equals("44")) {           //received message is FILE INFO COUNT
                int fileInfoCount = Integer.parseInt(st.nextToken());
                for(int j=0 ; j < fileInfoCount ; j++){
                    receiveFileInfo(msg, pin);
                    //System.out.println("");
                }
            }
         }
         catch(Exception e){System.out.println(e.getMessage());}
     }
    
    //to receive one record of file DB info and add to DB from peer
     private void receiveFileInfo(String msg, InputStream in)
    {
        try {
            int c, i;
            msg="";
            char [] ch = new char[10000];	
            i=0;
            while (( c = in.read()) != '\n') {
                ch[i] = (char) c;
                i++;
            }
            //System.out.print((char) c);
            msg = new String(ch);
            msg = msg.trim();
            System.out.println("message received in peer : "+msg);
            StringTokenizer st = new StringTokenizer(msg,":");
            String msg_type = st.nextToken();
            String filename = st.nextToken();
            double filesize = Double.parseDouble(st.nextToken());
            String filesizeString = filesize+"";
            String ip = st.nextToken();
            String abstractOfFile = st.nextToken();
            //byte[] fdigest = st.nextToken().getBytes();
            byte[] digest = new byte[1024];
            for(int n = 0; n < 1024 ; n++)
            {
                digest[n] = (byte) in.read();
            }
            
            //ip = ip.substring(1);
            System.out.println("IPServer: Client IP is "+ip);
            
            //Time to insert into the database
            Properties vals = new Properties();
            vals.put("name", filename);
            vals.put("size",filesizeString);
            vals.put("fileabstract",abstractOfFile);
            vals.put("digest",digest.toString());
            vals.put("ip",ip);
            DBOperations.insertIntoFiles(vals,IPClient.con,digest);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
     
     //send file DB info to peer requesting it based on coordinates
     public void sendFileInfo(String msg,OutputStream out)
    {
        try {
            StringTokenizer st = new StringTokenizer(msg,":");
            st.nextToken();
            int startX = Integer.parseInt(st.nextToken());
            int startY = Integer.parseInt(st.nextToken());
            int endX = Integer.parseInt(st.nextToken());
            int endY = Integer.parseInt(st.nextToken());
            ArrayList<String> tempArray = new ArrayList<String>();
            ArrayList<byte[]> digestArray = new ArrayList<byte[]>();
            
            String query = "select digest,name,ip,fileabstract, size from gangshare_client.files";
            Statement stmt = IPClient.con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            //Form the message to be sent
            
            while(rs.next()){
                String fname = rs.getString(2);
                double fsize = rs.getDouble(5);
                String ip = rs.getString(3);
                String fabstract = rs.getString(4);
                byte[] digestOfFile = rs.getBytes(1);
                
                for(int k = 0; k<1024; k++){
                    System.out.print(digestOfFile[k]);
                    if(k == 1023)
                        System.out.print(" half ");
                }
                System.out.println();
                if(isPresent(new Coordinate(startX,startY), new Coordinate(endX,endY), digestOfFile)){
                    String outgingMsg = "45:"+fname+":"+fsize+":"+ip+":"+fabstract+":";    //send each file info in a loop
                    tempArray.add(outgingMsg);
                    digestArray.add(digestOfFile);
                    String query1 = "delete from gangshare_client.files where name = '" + fname + "' and ip  = '" + ip + "'";
                    //System.out.println("query = " + query1);
                    Statement stmt1 = IPClient.con.createStatement();
                    stmt1.executeUpdate(query1);
                }
                
            }
            
            System.out.println("size of temp array : " + tempArray.size());
            sendMsg("44:"+tempArray.size(),out);   //send the count of files
            int i = 0;
            while(i<tempArray.size()){
                //45:filename:filesize:ip:fileabstract:digest
                /*String fname = rs.getString(2);
                double fsize = rs.getDouble(5);
                String ip = rs.getString(3);
                String fabstract = rs.getString(4);
                byte[] digestOfFile = rs.getBytes(1);
                
                String outgingMsg = "45:"+fname+":"+fsize+":"+ip+":"+fabstract+":";    //send each file info in a loop
                */
                sendMsg(tempArray.get(i),out);
                sendMsg(digestArray.get(i),out);
                i++;
            }
            
            
        } catch (Exception ex) {
            Logger.getLogger(IPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
     
      public String receiveMsg(InputStream sin)
     {
         String msg="";
        try {
            int c, i;
            char [] ch = new char[10000];	
            i=0;
            while (( c = sin.read()) != '\n') {
                ch[i] = (char) c;
                i++;
            }
            //System.out.print((char) c);
            msg = new String(ch);
            msg = msg.trim();
        } catch (IOException ex) {
            Logger.getLogger(ProcessRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return msg;
     }
      
     public void sendMsg(String msg,OutputStream out) {
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
     
     public void sendMsg(byte[] bmsg,OutputStream out)
     {
        try {
            out.write(bmsg);
        } catch (IOException ex) {
            Logger.getLogger(IPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
     }

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
     
    private boolean isPresent(Coordinate start, Coordinate end, byte[] digestOfFile) {
        int firstHalf = countFirstHalf(digestOfFile);
        int secondHalf = countLastHalf(digestOfFile);
        System.out.println("the corrdinate of file is " + firstHalf + ":" + secondHalf);
        if((firstHalf>=start.x && firstHalf<=end.x) && (secondHalf>=start.y && secondHalf<=end.y)){
            return true;
        }
        return false;
    }
     
}


public class PeerThread implements Runnable {
    
    int peerport;
    ServerSocket ps;
    IPClient parent;
    InputStream in;
    OutputStream out;
    Socket s;
    
    PeerThread(IPClient parent,int peerport)
    {
        this.parent = parent;
        this.peerport = peerport;
    }
    
    public void run()
    {
        try {
            ps = new ServerSocket(peerport);
            
            while(true)
            {
                s = ps.accept();
                new ProcessRequest(s,parent,peerport);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(PeerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
