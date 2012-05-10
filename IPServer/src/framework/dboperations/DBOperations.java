/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.dboperations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aswin
 */
public class DBOperations {
    
    public static void insertIntoFiles(Properties vals,Connection con,byte[] digestOfFile)
    {
        try {
            String name = vals.getProperty("name");
            double filesize = Double.parseDouble(vals.getProperty("size"));
            String fileAbstract = vals.getProperty("fileabstract");
            String digest = vals.getProperty("digest");
            String ip = vals.getProperty("ip");
            String hitCount = vals.getProperty("hit_count");
            
            String query = "insert into gangshare.files values (?,?,?,?,?,?)";
            
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setDouble(2, filesize);
            stmt.setString(3, fileAbstract);
            stmt.setString(4, ip);
            stmt.setBytes(5, digestOfFile);
            stmt.setInt(6, Integer.parseInt(hitCount));
            stmt.executeUpdate();
            
            
        } catch (SQLException ex) {
            Logger.getLogger(DBOperations.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static void deleteAnIP(String IP, Connection con)
    {
        try {
            String query = "delete from gangshare.files where ip=?";
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setString(1,IP);
            stmt.executeUpdate();
            
        } catch (SQLException ex) {
            Logger.getLogger(DBOperations.class.getName()).log(Level.SEVERE, null, ex);
        }
                
    }
    
    public static void updateHitCount(String fileName, String IP, Connection con) {
        try {
            String query = "update gangshare.files set hitcount = hitcount + 1 where name = ? and ip=? ";
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setString(1,fileName);
            stmt.setString(2,IP);
            stmt.executeUpdate();
            
        } catch (SQLException ex) {
            Logger.getLogger(DBOperations.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
