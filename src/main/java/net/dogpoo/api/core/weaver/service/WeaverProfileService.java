package net.dogpoo.api.core.weaver.service;

import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.net.URI;
import java.security.SecureRandom;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;

@Service public class WeaverProfileService {
 private final WeaverProfileMapper profiles; private final WeaverSessionMapper sessions;
 private final WeaverVerificationMail mail; private final PasswordEncoder encoder; private final Path root;
 public WeaverProfileService(WeaverProfileMapper profiles,WeaverSessionMapper sessions,WeaverVerificationMail mail,PasswordEncoder encoder,@Value("${WEAVER_RESOURCE_DIR:/home/ocaptain/resource}") String root){this.profiles=profiles;this.sessions=sessions;this.mail=mail;this.encoder=encoder;this.root=Path.of(root).resolve("profile/images").toAbsolutePath().normalize();}
 public static String safeAvatar(String value){
  if(value==null || value.length()>2048)return null;
  try{URI u=URI.create(value);String h=u.getHost();if(h==null || u.getUserInfo()!=null)return null;
   // Provider CDNs only. No arbitrary remote images or server-side URL fetching.
   if(!List.of("googleusercontent.com","githubusercontent.com","gitlab.com","pstatic.net","kakaocdn.net","kakao.com").stream().anyMatch(d->h.equals(d)||h.endsWith("."+d)))return null;
   if(!u.getScheme().equals("https") && !u.getScheme().equals("http"))return null;
   return new URI("https",null,h,-1,u.getPath(),u.getQuery(),null).toString();
  }catch(Exception e){return null;}
 }
 private Path file(String id,String path){Path p=root.resolve(path).normalize();if(!p.getParent().equals(root.resolve(id)))throw new IllegalStateException("Invalid profile path");return p;}
 public Map<String,Object> read(String id){
  var a=sessions.account(id);if(a==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  profiles.ensure(id);var p=profiles.get(id);var result=new HashMap<String,Object>();result.put("name",a.displayName()==null?"":a.displayName());result.put("email",a.email());result.put("providers",profiles.providers(id));result.put("defaultFace",p==null?0:p.defaultFace());result.put("emailEnabled",p==null || p.emailEnabled());
  if(p!=null){if(p.avatarPath()!=null){try{result.put("image",Base64.getEncoder().encodeToString(Files.readAllBytes(file(id,p.avatarPath()))));}catch(IOException e){/* Missing image uses the client's bundled fallback. */}}else if(safeAvatar(p.avatarUrl())!=null)result.put("avatarUrl",safeAvatar(p.avatarUrl()));}
  return result;
 }
 @Transactional public void name(String id,String name){profiles.name(id,WeaverAuthService.name(name));}
 @Transactional public void photo(String id,String encoded){
  if(encoded==null || encoded.length()>2800000)throw bad("사진은 2MB 이하로 선택해주세요.");
  byte[] output;
  try{
   byte[] bytes=Base64.getDecoder().decode(encoded);if(bytes.length>2097152)throw bad("사진은 2MB 이하로 선택해주세요.");
   try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
    var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw bad("PNG 또는 JPEG 사진을 선택해주세요.");
    var reader=readers.next();try{
     if(!Set.of("png","jpeg","jpg").contains(reader.getFormatName().toLowerCase(Locale.ROOT)))throw bad("PNG 또는 JPEG 사진을 선택해주세요.");
     reader.setInput(input);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>4096||h>4096)throw bad("사진 크기는 최대 4096×4096입니다.");
     var source=reader.read(0);int edge=Math.min(w,h);var target=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);var g=target.createGraphics();
     g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
     g.drawImage(source,0,0,512,512,(w-edge)/2,(h-edge)/2,(w+edge)/2,(h+edge)/2,null);g.dispose();
     var out=new ByteArrayOutputStream();ImageIO.write(target,"png",out);output=out.toByteArray();
    }finally{reader.dispose();}
   }
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw bad("사진 파일을 읽을 수 없습니다.");}
  profiles.ensure(id);var previous=profiles.lock(id);String relative=id+"/"+UUID.randomUUID()+".png";Path target=file(id,relative);
  try{Files.createDirectories(target.getParent());Files.write(target,output,StandardOpenOption.CREATE_NEW);}catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"사진을 저장하지 못했습니다.");}
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){try{
   if(status!=STATUS_COMMITTED)Files.deleteIfExists(target);
   else if(previous.avatarPath()!=null)Files.deleteIfExists(file(id,previous.avatarPath()));
  }catch(IOException ignored){}}});
  profiles.avatar(id,relative);
 }
 @Transactional public void requestPassword(String id){
  profiles.ensure(id);var p=profiles.lock(id);if(!p.emailEnabled())throw bad("이메일 로그인 계정에서만 비밀번호를 변경할 수 있습니다.");
  if(profiles.recent(id)>0)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"1분 후 다시 요청해주세요.");
  String code=String.format(Locale.ROOT,"%08d",new SecureRandom().nextInt(100000000));
  profiles.challenge(id,WeaverAuthService.hash(id+":"+code));mail.password(sessions.account(id).email(),code);
 }
 // Failed verification attempts must commit, otherwise attackers could retry indefinitely.
 @Transactional(noRollbackFor=ResponseStatusException.class) public void changePassword(String id,WeaverProfileRequest request){
  var a=sessions.account(id);var credentials=new WeaverAuthRequest();credentials.email=a.email();credentials.name=a.displayName();credentials.password=request.password;credentials.passwordConfirmation=request.passwordConfirmation;
  WeaverAuthService.validateCredentials(credentials,false);
  if(!request.password.equals(request.passwordConfirmation))throw bad("비밀번호가 일치하지 않습니다.");
  profiles.ensure(id);if(!profiles.lock(id).emailEnabled())throw bad("이메일 로그인 계정에서만 비밀번호를 변경할 수 있습니다.");
  if(profiles.attempt(id)!=1)throw bad("인증코드가 만료되었거나 시도 횟수를 초과했습니다. 다시 요청해주세요.");
  if(request.code==null || !request.code.matches("[0-9]{8}") || profiles.consume(id,WeaverAuthService.hash(id+":"+request.code))!=1)throw bad("인증코드를 확인해주세요.");
  profiles.password(id,encoder.encode(request.password));sessions.revokeAll(id);sessions.revokeLegacy(id);
 }
 private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
