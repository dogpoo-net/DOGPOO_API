package net.dogpoo.api.common.util;

import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public final class MailSendUtil {
	
	// naver => smtp.naver.com
	// google => smtp.gmail.com
	// daum => smtp.daum.net
//	final static String HOST = System.getenv("SMTP_HOST"); 
	final static String HOST = System.getenv("SMTP_HOST"); 
	final static String FROM = System.getenv("SMTP_FROM");
	final static String ID = System.getenv("SMTP_USERNAME");
	final static String PW = System.getenv("SMTP_PASSWORD");
//	final static String ID = System.getenv("SMTP_USERNAME");
//	final static String PW = System.getenv("SMTP_PASSWORD");
	
	// SSL => 465
	// TLS => 587
	final static int PORT = 465;
	
	/**
	 * 메일 송신
	 * @param toList (수신자 리스트)
	 * @param subject (메일 제목)
	 * @param body (메일 내용)
	 * @throws AddressException
	 * @throws MessagingException
	 * @author De-Kay
	 */
	public static boolean mailSender(List<String> toList, String subject, String body) {
		
		Properties props = System.getProperties();
		
		// set SMTP server
		props.put("mail.smtp.host", HOST);
		props.put("mail.smtp.port", PORT);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.ssl.enable", "true");
		props.put("mail.smtp.ssl.trust", HOST);
		
		Session session = Session.getDefaultInstance(props, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(ID, PW);
			}
		});
		session.setDebug(true);		//for debug

		Message message = new MimeMessage(session);
		try {
			message.setFrom(new InternetAddress(FROM));
			
			// .TO => 일반
			// .CC => 참조
			// .BCC => 숨은참조
			for(String to : toList) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			}
			message.setSubject(subject);
			message.setContent(body.replace("\n", "<br>"), "text/html;charset=utf-8");
			
			Transport.send(message);
			return true;
		} catch (AddressException e) {
			e.printStackTrace();
			return false;
		} catch (MessagingException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean isValidEmail(String email) {
		boolean err = false;
		String regex = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(email);
		if (m.matches()) {
			err = true;
		}
		return err;
	}

//	private Authenticator createAuthenticator(String id, String pw) {
//		return new Authenticator() {
//			protected PasswordAuthentication getPasswordAuthentication() {
//				return new PasswordAuthentication(id, pw);
//			}
//		};
//	}
	
}