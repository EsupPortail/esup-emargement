package org.esupportail.emargement.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
  
    @Autowired
    public JavaMailSender emailSender;
    
    @Resource
    AppliConfigService appliConfigService;
    
	@Value("${app.nomDomaine}")
	private String nomDomaine;
	
	@Value("${app.noreply}")
	private String noReply;
 
    public void sendSimpleMessage(String to, String subject, String text, String [] cc) {
        
        SimpleMailMessage message = new SimpleMailMessage();
        if(!appliConfigService.getTestEmail().isEmpty()) {
			to = appliConfigService.getTestEmail();
		}
        message.setTo(to); 
        message.setSubject(subject); 
        message.setText(text);
        message.setFrom(noReply.concat("@").concat(nomDomaine));
        if(cc.length>0) {
        	message.setCc(cc);
        }
        emailSender.send(message);
       
    }
    
    public void sendMessageWithAttachment(String to, String subject, String text, String pathToAttachment, String fileName, String [] cc, 
    		InputStream inputStream) throws MessagingException, IOException {
         
        MimeMessage message = emailSender.createMimeMessage();
    
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        if(!appliConfigService.getTestEmail().isEmpty()) {
			to = appliConfigService.getTestEmail();
		}
         
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);
        helper.setFrom(noReply.concat("@").concat(nomDomaine));
        if(cc.length>0) {
        	helper.setCc(cc);
        }
        FileSystemResource file = null;
        if(inputStream != null) {
	        helper.addAttachment(fileName,
	        new ByteArrayResource(IOUtils.toByteArray(inputStream)));
        }else {
        	file = new FileSystemResource(new File(pathToAttachment));
        	helper.addAttachment(fileName, file);
        }
     
        emailSender.send(message);
    }
}
