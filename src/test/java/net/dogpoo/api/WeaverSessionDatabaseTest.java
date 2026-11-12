package net.dogpoo.api;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.AopTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.*;
import net.dogpoo.api.core.weaver.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@EnabledIfEnvironmentVariable(named="WEAVER_AUTH_DB_TEST",matches="true")
@SpringBootTest(properties={"weaver.scheduling.enabled=false"})
class WeaverSessionDatabaseTest {
 @Autowired WeaverAuthService auth;
 @Autowired WeaverSessionService sessions;
 @Autowired JdbcTemplate jdbc;
 @MockitoBean WeaverVerificationMail mail;
 String email="session-test-"+UUID.randomUUID()+"@example.invalid";
 WeaverAuthRequest request;
 Instant time=Instant.now();
 void advance(Duration duration){time=time.plus(duration);ReflectionTestUtils.setField((Object)AopTestUtils.getUltimateTargetObject(sessions),"clock",Clock.fixed(time,ZoneOffset.UTC));}
 @BeforeEach void signup(){
  advance(Duration.ZERO);request=new WeaverAuthRequest();request.name="테스트 사용자";request.email=email;request.password="test-only-long-password";request.passwordConfirmation=request.password;
  auth.signup(request);var token=org.mockito.ArgumentCaptor.forClass(String.class);verify(mail).send(eq(email),token.capture());auth.verify(token.getValue());
 }
 @AfterEach void cleanup(){jdbc.update("DELETE FROM WEAVER_USER_ACCOUNT WHERE email=?",email);ReflectionTestUtils.setField((Object)AopTestUtils.getUltimateTargetObject(sessions),"clock",Clock.systemUTC());}
 String refresh(Map<String,Object> data){return (String)data.get("refreshToken");}
 String access(Map<String,Object> data){return "Bearer "+data.get("accessToken");}
 @Test void accessExpiresInThirtyMinutesAndRefreshSurvivesThirtyDaysButNotNinetyIdleDays(){
  var login=sessions.login(request,"native",null);assertEquals(email,sessions.me(access(login)).get("email"));
  advance(Duration.ofMinutes(31));assertThrows(ResponseStatusException.class,()->sessions.me(access(login)));
  advance(Duration.ofDays(30));var refreshed=sessions.refresh(refresh(login),"r".repeat(43));assertEquals(email,refreshed.get("email"));assertNotEquals(refresh(login),refresh(refreshed));
  advance(Duration.ofDays(90));assertThrows(ResponseStatusException.class,()->sessions.refresh(refresh(refreshed),"r".repeat(43)));
 }
 @Test void nativeSessionRemainsValidBeyondOneYearWithRegularActivity(){
  var login=sessions.login(request,"native",null);
  for(int i=0;i<4;i++){advance(Duration.ofDays(80));login=sessions.refresh(refresh(login),"r".repeat(43));}
  advance(Duration.ofDays(80));String token=refresh(login);assertEquals(email,sessions.refresh(token,"r".repeat(43)).get("email"));
 }
 @Test void deviceLogoutLeavesOtherDeviceAndGlobalLogoutRevokesEverything(){
  var first=sessions.login(request,"native",null);var second=sessions.login(request,"native",null);
  sessions.logout(refresh(first),false);assertThrows(ResponseStatusException.class,()->sessions.me(access(first)));
  assertEquals(email,sessions.me(access(second)).get("email"));
  var third=sessions.login(request,"native",null);var old=auth.login(request);
  sessions.logout(refresh(second),true);
  assertThrows(ResponseStatusException.class,()->sessions.refresh(refresh(third),"r".repeat(43)));
  assertThrows(ResponseStatusException.class,()->sessions.me(access(third)));
  assertThrows(ResponseStatusException.class,()->auth.me("Bearer "+old.get("sessionToken")));
 }
 @Test void refreshReplayRevocationCommitsAndInvalidatesReplacement(){
  var first=sessions.login(request,"native",null);var next=sessions.refresh(refresh(first),"r".repeat(43));
  assertEquals(refresh(next),refresh(sessions.refresh(refresh(first),"r".repeat(43))));
  assertThrows(ResponseStatusException.class,()->sessions.refresh(refresh(first),"s".repeat(43)));
  assertThrows(ResponseStatusException.class,()->sessions.refresh(refresh(next),"r".repeat(43)));
  assertThrows(ResponseStatusException.class,()->sessions.me(access(next)));
 }
 @Test void webReloadRequiresBothCookieAndTheOriginalTabProof(){
  String proof="a".repeat(43);var login=sessions.login(request,"web",proof);String cookie=(String)login.get("cookieToken");
  assertFalse(login.containsKey("refreshToken"));assertFalse(login.containsKey("accessToken"));
  assertEquals(email,sessions.webResume(cookie,proof).get("email"));
  assertThrows(ResponseStatusException.class,()->sessions.webResume(cookie,"b".repeat(43)));
  assertThrows(ResponseStatusException.class,()->sessions.webResume(cookie,null));
  assertThrows(ResponseStatusException.class,()->sessions.refresh(cookie,"r".repeat(43)));
  sessions.webLogout(cookie,proof,false);assertThrows(ResponseStatusException.class,()->sessions.webResume(cookie,proof));
 }
}
