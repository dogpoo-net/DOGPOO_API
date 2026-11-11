package net.dogpoo.api.core.weaver.service;

import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class WeaverVerificationMail {
 private final String host,from,username,password,baseUrl;
 public WeaverVerificationMail(@Value("${SMTP_HOST:}") String host,@Value("${SMTP_FROM:}") String from,
   @Value("${SMTP_USERNAME:}") String username,@Value("${SMTP_PASSWORD:}") String password,
   @Value("${WEAVER_PUBLIC_URL:https://weaver.dogpoo.net/}") String baseUrl) {
  this.host=host;this.from=from;this.username=username;this.password=password;this.baseUrl=baseUrl;
 }
 public void send(String email,String token) {
  deliver(email,"WEAVER 이메일 인증","WEAVER 회원가입을 완료하려면 아래 링크를 열고 이메일 인증을 눌러주세요.\n\n"+baseUrl+"#verify-email="+token+"\n\n링크는 30분 동안 한 번만 사용할 수 있습니다. 가입을 요청하지 않았다면 이 메일을 무시하세요.");
 }
 public void invite(String email,String token,String role){deliver(email,"WEAVER 워크스페이스 초대","워크스페이스에 "+(role.equals("editor")?"편집자":"읽기 전용 참여자")+"로 초대되었습니다. 초대받은 이메일 계정으로 로그인한 뒤 수락해주세요.\n\n"+baseUrl+"#invite="+token+"\n\n초대는 7일 동안 유효합니다.");}
 private void deliver(String email,String subject,String body) {
  if(host.isBlank() || from.isBlank() || username.isBlank() || password.isBlank())
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"인증메일 설정을 확인해주세요.");
  Properties p=new Properties();
  p.setProperty("mail.smtp.host",host);p.setProperty("mail.smtp.port","465");
  p.setProperty("mail.smtp.auth","true");p.setProperty("mail.smtp.ssl.enable","true");
  p.setProperty("mail.smtp.ssl.checkserveridentity","true");
  p.setProperty("mail.smtp.connectiontimeout","10000");p.setProperty("mail.smtp.timeout","10000");p.setProperty("mail.smtp.writetimeout","10000");
  Session session=Session.getInstance(p,new Authenticator(){
   protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(username,password);}
  });
  try {
   MimeMessage m=new MimeMessage(session);
   m.setFrom(new InternetAddress(from));m.setRecipient(Message.RecipientType.TO,new InternetAddress(email));
   m.setSubject(subject,"UTF-8");
   // Fragment keeps the token out of HTTP access logs and Referer headers.
   m.setText(body,"UTF-8");
   Transport.send(m);
  } catch(MessagingException e) {
   // Do not log transport payloads, recipient addresses or credentials.
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"인증메일을 보내지 못했습니다. 잠시 후 다시 시도해주세요.");
  }
 }
}
