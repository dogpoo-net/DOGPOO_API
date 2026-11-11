package net.dogpoo.api;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
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
@SpringBootTest(properties={"weaver.scheduling.enabled=false"})
class WeaverSocialDatabaseTest {
 @Autowired WeaverOAuthService oauth;
 @Autowired WeaverSocialAccountService social;
 @Autowired WeaverSessionService sessions;
 @Autowired JdbcTemplate jdbc;
 @MockitoBean WeaverNaverGateway naver;
 @MockitoBean WeaverGithubGateway github;
 @MockitoBean WeaverGitlabGateway gitlab;
 String nonce=UUID.randomUUID().toString();
 String email="social-test-"+nonce+"@example.invalid";
 List<String> flows=new ArrayList<>();
 String hash(String s)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
 @AfterEach void cleanup()throws Exception{
  for(String flow:flows)jdbc.update("DELETE FROM WEAVER_OAUTH_FLOW WHERE id_hash=?",hash(flow));
  jdbc.update("DELETE FROM WEAVER_USER_ACCOUNT WHERE email=?",email);
 }
 @Test void providerIdentityIsStableAndEmailEqualityDoesNotAutoLink(){
  var first=social.resolve(new WeaverSocialIdentity("kakao",nonce,email,"민수"),null);
  assertEquals(first.id(),social.resolve(new WeaverSocialIdentity("kakao",nonce,"changed@example.com","다른 이름"),null).id());
  var second=new WeaverSocialIdentity("naver",nonce,email,"민수");
  assertEquals(409,assertThrows(ResponseStatusException.class,()->social.resolve(second,null)).getStatusCode().value());
  assertEquals(first.id(),social.resolve(second,first.id()).id());
  assertEquals(first.id(),social.resolve(second,null).id());
  var result=sessions.issue(first,"native",null);assertEquals("민수",result.get("name"));
 }
 @Test void oauthStateProofProviderAndOneTimeCompletionAreEnforced()throws Exception{
  AtomicReference<String> state=new AtomicReference<>();
  when(naver.authorize(anyString())).thenAnswer(i->{state.set(i.getArgument(0));return "https://nid.naver.com/oauth2.0/authorize?state="+state.get();});
  when(naver.exchange(eq("test-code"),anyString())).thenReturn(new WeaverSocialIdentity("naver",nonce,email,"민수"));
  String proof="p".repeat(43);var start=oauth.start("naver","native",proof,null);String flow=(String)start.get("flow");flows.add(flow);
  assertEquals(true,oauth.finish("naver",flow,"native",proof,null,null,null).get("pending"));
  assertThrows(ResponseStatusException.class,()->oauth.callback("github","test-code",state.get(),null));
  oauth.callback("naver","test-code",state.get(),null);
  assertThrows(ResponseStatusException.class,()->oauth.callback("naver","test-code",state.get(),null));
  assertThrows(ResponseStatusException.class,()->oauth.finish("naver",flow,"native","x".repeat(43),null,null,null));
  assertThrows(ResponseStatusException.class,()->oauth.finish("github",flow,"native",proof,null,null,null));
  var result=oauth.finish("naver",flow,"native",proof,null,null,null);assertNotNull(result.get("refreshToken"));
  assertEquals(email,sessions.me("Bearer "+result.get("accessToken")).get("email"));
  assertThrows(ResponseStatusException.class,()->oauth.finish("naver",flow,"native",proof,null,null,null));
 }
 @Test void githubAndGitlabUseTheSameBoundWebSessionFlow(){
  for(String provider:List.of("github","gitlab")){
   AtomicReference<String> state=new AtomicReference<>();
   var identity=new WeaverSocialIdentity(provider,nonce,email,"민수");
   if(provider.equals("github")){
    when(github.authorize(anyString())).thenAnswer(i->{state.set(i.getArgument(0));return "https://github.com/login/oauth/authorize?state="+state.get();});
    when(github.exchange(anyString(),anyString())).thenReturn(identity);
   }else{
    when(gitlab.authorize(anyString())).thenAnswer(i->{state.set(i.getArgument(0));return "https://gitlab.com/oauth/authorize?state="+state.get();});
    when(gitlab.exchange(anyString(),anyString())).thenReturn(identity);
   }
   // Explicit linking prevents an email-only merge between the providers.
   if(provider.equals("gitlab")){
    String id=jdbc.queryForObject("SELECT id FROM WEAVER_USER_ACCOUNT WHERE email=?",String.class,email);social.resolve(identity,id);
   }
   String proof="w".repeat(43);var start=oauth.start(provider,"web",proof,null);String flow=(String)start.get("flow");flows.add(flow);
   oauth.callback(provider,"test-code",state.get(),null);
   var result=oauth.finish(provider,flow,"web",proof,null,null,proof);
   assertFalse(result.containsKey("refreshToken"));assertEquals(email,sessions.webResume((String)result.get("cookieToken"),proof).get("email"));
  }
 }
 @Test void abandonedAndExpiredFlowsCannotIssueSessions()throws Exception{
  when(naver.authorize(anyString())).thenReturn("https://nid.naver.com/oauth2.0/authorize");
  String flow=(String)oauth.start("naver","native","p".repeat(43),null).get("flow");flows.add(flow);
  jdbc.update("UPDATE WEAVER_OAUTH_FLOW SET expires_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 SECOND) WHERE id_hash=?",hash(flow));
  assertThrows(ResponseStatusException.class,()->oauth.finish("naver",flow,"native","p".repeat(43),null,null,null));
 }
}
