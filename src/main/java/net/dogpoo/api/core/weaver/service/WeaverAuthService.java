package net.dogpoo.api.core.weaver.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.WeaverAuthMapper;
import net.dogpoo.api.core.weaver.model.*;

@Service
public class WeaverAuthService {
 private final WeaverAuthMapper mapper;
 private final PasswordEncoder encoder;
 private final WeaverVerificationMail mail;
 private final String dummyHash;
 private final SecureRandom random=new SecureRandom();
 public WeaverAuthService(WeaverAuthMapper mapper,PasswordEncoder encoder,WeaverVerificationMail mail) {
  this.mapper=mapper;this.encoder=encoder;this.mail=mail;dummyHash=encoder.encode(UUID.randomUUID().toString());
 }
 public static String email(String value) {
  String email=value==null?"":value.strip().toLowerCase(Locale.ROOT);
  if(email.length()>254 || !email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,63}"))
   throw bad("올바른 이메일 주소를 입력해주세요.");
  return email;
 }
 private static void password(String value) {
  if(value==null || value.length()<10 || value.getBytes(StandardCharsets.UTF_8).length>72)
   throw bad("비밀번호는 10자 이상, UTF-8 기준 72바이트 이하로 입력해주세요.");
 }
 public static void validateCredentials(WeaverAuthRequest request,boolean signup) {
  email(request.email);password(request.password);
  if(signup && !request.password.equals(request.passwordConfirmation)) throw bad("비밀번호가 일치하지 않습니다.");
 }
 @Transactional
 public void signup(WeaverAuthRequest request) {
  validateCredentials(request,true);
  String email=email(request.email);
  String hash=encoder.encode(request.password);
  if(mapper.account(email)!=null) return; // Identical public response for existing accounts.
  String id=UUID.randomUUID().toString();
  mapper.insertAccount(id,email,hash);
  send(id,email);
 }
 @Transactional
 public void resend(String input) {
  String email=email(input);
  WeaverAccount account=mapper.lockAccount(email);
  if(account==null || account.verifiedAt()!=null || mapper.recentlySent(account.id())>0) return;
  send(account.id(),email);
 }
 private void send(String id,String email) {
  String token=randomToken();mapper.token(id,hash(token));mail.send(email,token);
 }
 @Transactional
 public void verify(String token) {
  validateToken(token);
  String hash=hash(token);
  if(mapper.verify(hash)!=1) throw bad("인증 링크가 만료되었거나 이미 사용되었습니다. 로그인하거나 인증메일을 다시 요청해주세요.");
  mapper.consume(hash);
 }
 @Transactional
 public Map<String,Object> login(WeaverAuthRequest request) {
  String email=email(request.email);password(request.password);
  WeaverAccount account=mapper.account(email);
  boolean matched=encoder.matches(request.password,account==null?dummyHash:account.passwordHash());
  if(!matched || account==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"이메일 또는 비밀번호를 확인해주세요.");
  if(account.verifiedAt()==null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"이메일 인증 후 로그인해주세요.");
  String token=randomToken();mapper.cleanSessions();mapper.session(hash(token),account.id());
  return Map.of("id",account.id(),"email",account.email(),"sessionToken",token,"expiresIn",43200);
 }
 public Map<String,Object> me(String authorization) {
  WeaverAccount account=mapper.sessionAccount(hash(bearer(authorization)));
  if(account==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 만료되었습니다.");
  return Map.of("id",account.id(),"email",account.email());
 }
 public void logout(String authorization) { mapper.logout(hash(bearer(authorization))); }
 private String bearer(String value) {
  if(value==null || !value.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다.");
  String token=value.substring(7);validateToken(token);return token;
 }
 private static void validateToken(String value) {
  if(value==null || !value.matches("[A-Za-z0-9_-]{43}")) throw bad("올바르지 않은 인증 정보입니다.");
 }
 private String randomToken() { byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
 static String hash(String token) {
  try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 private static ResponseStatusException bad(String reason){return new ResponseStatusException(HttpStatus.BAD_REQUEST,reason);}
}
