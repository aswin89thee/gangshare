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
    
    public static void insertIntoFiles(Properties vals,Connection con)
    {
        try {
            String name = vals.getProperty("name");
            double filesize = Double.parseDouble(vals.getProperty("size"));
            String fileAbstract = vals.getProperty("fileabstract");
            String digest = vals.getProperty("digest");
            String ip = vals.getProperty("ip");
            
            String query = "insert into gangshare.files values (?,?,?,?,?)";
            
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setDouble(2, filesize);
            stmt.setString(3, fileAbstract);
            stmt.setString(4, digest);
            stmt.setString(5, ip);
            
            stmt.executeUpdate();
            
            
        } catch (SQLException ex) {
            Logger.getLogger(DBOperations.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
