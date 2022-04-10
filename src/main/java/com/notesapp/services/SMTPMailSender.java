package com.notesapp.services;

import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * A class for handling outgoing emails.
 *  
 * @author stephen
 *
 */
@Component
public class SMTPMailSender {
	
	@Autowired
	private JavaMailSender javaMailSender;
	
	@Autowired
	private TemplateEngine templateEngine;
	
	/**
	 * Send email with verification code.
	 * 
	 * @param to address to send to
	 * @param subject email subject
	 * @param code verification code
	 * @throws MessagingException
	 */
	public void send(String to, String subject, int code) 
			throws MessagingException {
		Context context = new Context();
		context.setVariable("receiver", to);
		context.setVariable("receiverFirstLetter", String.valueOf(to.charAt(0)).toUpperCase());
		context.setVariable("code", code);
		
		String process = templateEngine.process("verification", context);
		
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper;
		
		helper = new MimeMessageHelper(message, true);
		
		try {
			helper.setFrom(new InternetAddress("orangenotes@gmail.com", 
					"Orange Notes"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		helper.setSubject(subject);
		helper.setTo(to);
		helper.setText(process, true);
		
		javaMailSender.send(message);
	}
}
