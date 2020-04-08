package org.esupportail.emargement.services;

import java.io.File;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
  
    @Autowired
    public JavaMailSender emailSender;
 
    public void sendSimpleMessage(String to, String subject, String text, String [] cc) {
        
        SimpleMailMessage message = new SimpleMailMessage(); 
        message.setTo(to); 
        message.setSubject(subject); 
        message.setText(text);
        if(cc.length>0) {
        	message.setCc(cc);
        }
        emailSender.send(message);
       
    }
    
    public void sendMessageWithAttachment(String to, String subject, String text, String pathToAttachment, String fileName, String [] cc) throws MessagingException {
         
        MimeMessage message = emailSender.createMimeMessage();
    
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
         
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);
        if(cc.length>0) {
        	helper.setCc(cc);
        }
             
        FileSystemResource file 
          = new FileSystemResource(new File(pathToAttachment));
        helper.addAttachment(fileName, file);
     
        emailSender.send(message);
    }
   
    
}
