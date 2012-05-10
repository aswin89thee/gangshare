/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package framework.EmailManager;


import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
/**
*
* @author Aswin
*/
public class EmailManager {
   
   String toAddress;
   String content;
   private final String username = "gangshare.daemon@gmail.com";
   private final String password = "gangshare1234";
   
   public EmailManager(String emailAddress, String content)
   {
       toAddress = emailAddress;
       this.content = content;
   }
   
   public void sendEmail()
   {
       Properties props = new Properties();
       props.put("mail.smtp.host", "gmail-smtp-msa.l.google.com");
       props.put("mail.smtp.socketFactory.port", "465");
       props.put("mail.smtp.socketFactory.class",
                       "javax.net.ssl.SSLSocketFactory");
       props.put("mail.smtp.auth", "true");
       props.put("mail.smtp.port", "465");
       props.put("mail.smtp.socketFactory.fallback", "false"); 

       Session session = Session.getDefaultInstance(props,
                       new javax.mail.Authenticator() {
           @Override
                               protected PasswordAuthentication getPasswordAuthentication() {
                                       return new PasswordAuthentication(username,password);
                               }
                       });
       
       try {

                       Message message = new MimeMessage(session);
                       message.setFrom(new InternetAddress("password_daemon@gangshare.com"));
                       message.setRecipients(Message.RecipientType.TO,
                                       InternetAddress.parse(toAddress));
                       message.setSubject("Your forgotten Gangshare password");
                       message.setText(content+"\n\n\n\n\nThis is an automated email. Please do not reply.");

                       Transport.send(message);

                       System.out.println("Email Sent successfully!");

               } catch (MessagingException e) {
                       throw new RuntimeException(e);
               }
       
       
   }
   
}