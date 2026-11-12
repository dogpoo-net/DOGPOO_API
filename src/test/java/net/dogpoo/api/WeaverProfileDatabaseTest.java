package net.dogpoo.api;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.*;
import net.dogpoo.api.core.weaver.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@EnabledIfEnvironmentVariable(named="WEAVER_AUTH_DB_TEST",matches="true")
@SpringBootTest(properties={"weaver.scheduling.enabled=false","WEAVER_RESOURCE_DIR=/tmp/weaver-profile-tests"})
class WeaverProfileDatabaseTest {
 @Autowired WeaverProfileService profiles;@Autowired WeaverAuthService auth;@Autowired WeaverSocialAccountService social;@Autowired WeaverSessionService sessions;@Autowired JdbcTemplate jdbc;
 @MockitoBean WeaverVerificationMail mail;
 String id,email,code;
 @BeforeEach void setup(){
  email="profile-test-"+UUID.randomUUID()+"@example.invalid";
  var r=new WeaverAuthRequest();r.email=email;r.name="Before";r.password="old-password-123";r.passwordConfirmation=r.password;
  auth.signup(r);id=jdbc.queryForObject("SELECT id FROM WEAVER_USER_ACCOUNT WHERE email=?",String.class,email);
  jdbc.update("UPDATE WEAVER_USER_ACCOUNT SET verified_at=UTC_TIMESTAMP(6) WHERE id=?",id);
  doAnswer(i->{code=i.getArgument(1);return null;}).when(mail).password(eq(email),anyString());
 }
 @AfterEach void cleanup()throws Exception{
  jdbc.update("DELETE FROM WEAVER_USER_ACCOUNT WHERE id=?",id);
  Path path=Path.of("/tmp/weaver-profile-tests/profile/images",id);if(Files.exists(path))try(var files=Files.walk(path)){for(var f:files.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}
 }
 WeaverProfileRequest change(String c){var r=new WeaverProfileRequest();r.code=c;r.password="new-password-456";r.passwordConfirmation=r.password;return r;}
 @Test void passwordRequiresEmailCodeConsumesOnceAndRevokesEverySession(){
  var r=new WeaverSessionRequest();r.email=email;r.password="old-password-123";var nativeSession=sessions.login(r,"native",null);var web=sessions.login(r,"web","p".repeat(43));
  jdbc.update("UPDATE WEAVER_USER_ACCOUNT SET display_name=NULL WHERE id=?",id);
  profiles.requestPassword(id);assertNotNull(code);assertThrows(ResponseStatusException.class,()->profiles.requestPassword(id));
  assertThrows(ResponseStatusException.class,()->profiles.changePassword(id,change("invalid")));
  profiles.changePassword(id,change(code));
  assertThrows(ResponseStatusException.class,()->profiles.changePassword(id,change(code)));
  assertThrows(ResponseStatusException.class,()->sessions.me("Bearer "+nativeSession.get("accessToken")));
  assertThrows(ResponseStatusException.class,()->sessions.webResume((String)web.get("cookieToken"),"p".repeat(43)));
  assertThrows(ResponseStatusException.class,()->sessions.login(r,"native",null));r.password="new-password-456";assertNotNull(sessions.login(r,"native",null));
 }
 @Test void failedAttemptsPersistAndExpiredCodesFail(){
  profiles.requestPassword(id);
  for(int i=0;i<5;i++)assertThrows(ResponseStatusException.class,()->profiles.changePassword(id,change("invalid")));
  assertThrows(ResponseStatusException.class,()->profiles.changePassword(id,change(code)));
  assertEquals(5,jdbc.queryForObject("SELECT attempts FROM WEAVER_PASSWORD_CHALLENGE WHERE user_id=?",Integer.class,id));
  jdbc.update("UPDATE WEAVER_PASSWORD_CHALLENGE SET sent_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 2 MINUTE) WHERE user_id=?",id);
  profiles.requestPassword(id);jdbc.update("UPDATE WEAVER_PASSWORD_CHALLENGE SET expires_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 SECOND) WHERE user_id=?",id);
  assertThrows(ResponseStatusException.class,()->profiles.changePassword(id,change(code)));
 }
 @Test void nameAndAvatarPersistWithoutChangingIdentityOrOverwritingCustomPhoto()throws Exception{
  profiles.name(id,"  After  ");assertEquals("After",profiles.read(id).get("name"));
  var remote=new WeaverSocialIdentity("github",UUID.randomUUID().toString(),email,"Social","https://avatars.githubusercontent.com/u/1");social.resolve(remote,id);
  assertEquals(remote.avatarUrl(),profiles.read(id).get("avatarUrl"));
  var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);String encoded=Base64.getEncoder().encodeToString(out.toByteArray());
  profiles.photo(id,encoded);String first=(String)profiles.read(id).get("image");assertNotNull(first);
  social.resolve(remote,id);assertEquals(first,profiles.read(id).get("image"));assertFalse(profiles.read(id).containsKey("avatarUrl"));assertEquals("After",profiles.read(id).get("name"));
  profiles.photo(id,encoded);try(var files=Files.list(Path.of("/tmp/weaver-profile-tests/profile/images",id))){assertEquals(1,files.count());}
  assertThrows(ResponseStatusException.class,()->profiles.photo(id,Base64.getEncoder().encodeToString("not an image".getBytes())));
  assertNotNull(profiles.read(id).get("defaultFace"));
 }
 @Test void socialOnlyAccountCannotChangePasswordAndUnsafeRemoteImagesAreIgnored(){
  String subject=UUID.randomUUID().toString();String otherEmail="profile-social-"+subject+"@example.invalid";
  var a=social.resolve(new WeaverSocialIdentity("github",subject,otherEmail,"Social","https://127.0.0.1/private"),null);
  try{assertEquals(false,profiles.read(a.id()).get("emailEnabled"));assertFalse(profiles.read(a.id()).containsKey("avatarUrl"));assertThrows(ResponseStatusException.class,()->profiles.requestPassword(a.id()));}
  finally{jdbc.update("DELETE FROM WEAVER_USER_ACCOUNT WHERE id=?",a.id());}
 }
}
