package net.dogpoo.api.core.weaver.service;

import java.time.*;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import net.dogpoo.api.core.weaver.model.*;
import net.dogpoo.api.core.weaver.mapper.WeaverSessionMapper;

@Service
public class WeaverSessionService {
 private final WeaverSessionMapper mapper;
 private final WeaverAuthService auth;
 private final Clock clock;
 private final String rotationKey;
 private final SecureRandom random=new SecureRandom();
 @Autowired public WeaverSessionService(WeaverSessionMapper mapper,WeaverAuthService auth,@org.springframework.beans.factory.annotation.Value("${jwt.secret}") String key){this(mapper,auth,Clock.systemUTC(),key);}
 public WeaverSessionService(WeaverSessionMapper mapper,WeaverAuthService auth,Clock clock,String key){this.mapper=mapper;this.auth=auth;this.clock=clock;this.rotationKey=key;}
 private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);}
 private String secret(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 private String hash(String token){return WeaverAuthService.hash(token);}
 private boolean equal(String a,String b){return a!=null && b!=null && MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.US_ASCII),b.getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
 private ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 만료되었습니다. 다시 로그인해주세요.");}
 private String[] split(String value){if(value==null || !value.matches("[a-f0-9-]{36}\\.[A-Za-z0-9_-]{43}"))throw denied();return value.split("\\.",2);}
 private WeaverDeviceSession valid(WeaverDeviceSession s,String kind){
  if(s==null || !kind.equals(s.kind()) || !now().isBefore(s.idleExpiresAt()) || (s.absoluteExpiresAt()!=null && !now().isBefore(s.absoluteExpiresAt())))throw denied();
  return s;
 }
 private LocalDateTime idle(WeaverDeviceSession s){
  LocalDateTime candidate="web".equals(s.kind())?now().plusHours(24):now().plusDays(90);
  return s.absoluteExpiresAt()==null || candidate.isBefore(s.absoluteExpiresAt())?candidate:s.absoluteExpiresAt();
 }
 private Map<String,Object> identity(WeaverDeviceSession s){
  WeaverAccount a=mapper.account(s.userId());
  if(a==null || a.verifiedAt()==null)throw denied();
  return new HashMap<>(Map.of("id",a.id(),"email",a.email(),"name",a.displayName()==null?"":a.displayName(),"expiresIn",1800));
 }
 @Transactional public Map<String,Object> login(WeaverAuthRequest request,String kind,String proof){
  return issue(auth.authenticate(request),kind,proof);
 }
 @Transactional public Map<String,Object> issue(WeaverAccount a,String kind,String proof){
  if(a==null || a.verifiedAt()==null)throw denied();
  if(!Set.of("native","web").contains(kind))throw denied();
  if("web".equals(kind) && (proof==null || !proof.matches("[A-Za-z0-9_-]{43}")))throw denied();
  LocalDateTime now=now();String id=UUID.randomUUID().toString(),token=secret();
  WeaverDeviceSession s=new WeaverDeviceSession(id,a.id(),kind,hash(token),null,"web".equals(kind)?hash(proof):null,
    null,now,now,"web".equals(kind)?now.plusHours(24):now.plusDays(90),"native".equals(kind)?null:now.plusDays(365));
  mapper.insert(s);
  Map<String,Object> result=identity(s);
  if("native".equals(kind)){result.put("refreshToken",id+"."+token);issueAccess(s,result);}
  else result.put("cookieToken",id+"."+token);
  return result;
 }
 private void issueAccess(WeaverDeviceSession s,Map<String,Object> result){
  String token=secret();mapper.clearAccess(s.id());mapper.issueAccess(hash(token),s.id(),now().plusMinutes(30));result.put("accessToken",token);
 }
 // Replay revocation must commit even though the client receives 401.
 @Transactional(noRollbackFor=ResponseStatusException.class)
 public Map<String,Object> refresh(String value,String requestId){
  if(requestId==null || !requestId.matches("[A-Za-z0-9_-]{43}"))throw denied();
  String[] parts=split(value);WeaverDeviceSession s=valid(mapper.lock(parts[0]),"native");String incoming=hash(parts[1]);
  String requestHash=hash(requestId),next=rotation(value,requestId);
  if(!equal(incoming,s.currentHash())){
   if(equal(incoming,s.previousHash())){
    if(equal(requestHash,s.lastRequestHash()) && equal(hash(next),s.currentHash())){
     var repeated=identity(s);issueAccess(s,repeated);repeated.put("refreshToken",s.id()+"."+next);return repeated;
    }
    mapper.revoke(s.id());
   }
   throw denied();
  }
  Map<String,Object> result=identity(s);
  mapper.rotate(s.id(),hash(next),requestHash,now(),idle(s));issueAccess(s,result);result.put("refreshToken",s.id()+"."+next);return result;
 }
 private String rotation(String token,String requestId){
  try {
   var mac=javax.crypto.Mac.getInstance("HmacSHA256");
   mac.init(new javax.crypto.spec.SecretKeySpec(rotationKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));
   return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("WEAVER_REFRESH_V2:"+token+":"+requestId).getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
  }catch(GeneralSecurityException e){throw new IllegalStateException(e);}
 }
 private WeaverDeviceSession web(String cookie,String proof){
  String[] parts=split(cookie);WeaverDeviceSession s=valid(mapper.lock(parts[0]),"web");
  if(proof==null || !equal(hash(parts[1]),s.currentHash()) || !equal(hash(proof),s.proofHash()))throw denied();
  return s;
 }
 @Transactional public Map<String,Object> attachWeb(String cookie,String sourceProof,String targetProof){
  WeaverDeviceSession source=web(cookie,sourceProof);
  return issue(mapper.account(source.userId()),"web",targetProof);
 }
 @Transactional public Map<String,Object> webResume(String cookie,String proof){
  WeaverDeviceSession s=web(cookie,proof);mapper.touch(s.id(),now(),idle(s));return identity(s);
 }
 @Transactional public Map<String,Object> me(String bearer){
  if(bearer==null || !bearer.matches("Bearer [A-Za-z0-9_-]{43}"))throw denied();
  WeaverDeviceSession s=valid(mapper.access(hash(bearer.substring(7)),now()),"native");
  mapper.touch(s.id(),now(),idle(s));return identity(s);
 }
 @Transactional public void logout(String value,boolean all){
  String[] p=split(value);WeaverDeviceSession s=mapper.lock(p[0]);
  if(s==null){if(all)throw denied();return;} // Idempotent device logout.
  if(!"native".equals(s.kind()) || !equal(hash(p[1]),s.currentHash()))throw denied();
  if(all){valid(s,"native");mapper.revokeAll(s.userId());mapper.revokeLegacy(s.userId());}else mapper.revoke(s.id());
 }
 @Transactional public void webLogout(String cookie,String proof,boolean all){
  WeaverDeviceSession s=web(cookie,proof);
  if(all){mapper.revokeAll(s.userId());mapper.revokeLegacy(s.userId());}else mapper.revoke(s.id());
 }
}
