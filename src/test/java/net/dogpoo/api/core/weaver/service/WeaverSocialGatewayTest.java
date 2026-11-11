package net.dogpoo.api.core.weaver.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class WeaverSocialGatewayTest {
 ObjectMapper json=new ObjectMapper();
 @Test void kakaoRejectsAnotherAppOrUserAndUnverifiedEmail() throws Exception {
  var info=json.readTree("{\"app_id\":1646791,\"id\":123,\"expires_in\":100}");
  var profile=json.readTree("{\"id\":123,\"kakao_account\":{\"email\":\"user@example.com\",\"is_email_valid\":true,\"is_email_verified\":true,\"profile\":{\"nickname\":\"민수\"}}}");
  assertEquals("민수",WeaverKakaoGateway.identity(info,profile,1646791).name());
  assertThrows(ResponseStatusException.class,()->WeaverKakaoGateway.identity(info,profile,999));
  ((com.fasterxml.jackson.databind.node.ObjectNode)profile).put("id",456);
  assertThrows(ResponseStatusException.class,()->WeaverKakaoGateway.identity(info,profile,1646791));
  ((com.fasterxml.jackson.databind.node.ObjectNode)profile).put("id",123);
  ((com.fasterxml.jackson.databind.node.ObjectNode)profile.path("kakao_account")).put("is_email_verified",false);
  assertThrows(ResponseStatusException.class,()->WeaverKakaoGateway.identity(info,profile,1646791));
 }
 @Test void githubOnlyUsesVerifiedEmailsAndDoesNotAskForRepositoryAccess() throws Exception {
  var profile=json.readTree("{\"id\":123,\"login\":\"tester\",\"name\":null}");
  var emails=json.readTree("[{\"email\":\"unverified@example.com\",\"primary\":true,\"verified\":false},{\"email\":\"verified@example.com\",\"verified\":true}]");
  assertEquals("verified@example.com",WeaverGithubGateway.identity(profile,emails).email());
  assertEquals("tester",WeaverGithubGateway.identity(profile,emails).name());
  assertThrows(ResponseStatusException.class,()->WeaverGithubGateway.identity(profile,json.readTree("[]")));
  String url=new WeaverGithubGateway("id","test-only-secret","https://example.com/callback").authorize("state");
  assertTrue(url.contains("read%3Auser+user%3Aemail"));assertFalse(url.contains("repo"));assertFalse(url.contains("test-only-secret"));
 }
 @Test void naverAndGitlabRequireNameAndEmail() throws Exception {
  assertEquals("naver",WeaverNaverGateway.identity(json.readTree("{\"resultcode\":\"00\",\"response\":{\"id\":\"id\",\"email\":\"user@example.com\",\"name\":\"민수\"}}")).provider());
  assertThrows(ResponseStatusException.class,()->WeaverNaverGateway.identity(json.readTree("{\"resultcode\":\"00\",\"response\":{\"id\":\"id\"}}")));
  assertEquals("gitlab",WeaverGitlabGateway.identity(json.readTree("{\"sub\":\"id\",\"email\":\"user@example.com\",\"email_verified\":true,\"name\":\"민수\"}")).provider());
  assertThrows(ResponseStatusException.class,()->WeaverGitlabGateway.identity(json.readTree("{\"sub\":\"id\",\"email\":\"user@example.com\",\"email_verified\":false,\"name\":\"민수\"}")));
 }
 @Test void gitlabOnlyRequestsIdentityScopesAndRequiresVerifiedEmail() throws Exception {
  var profile=json.readTree("{\"sub\":\"42\",\"name\":\"민수\",\"email\":\"user@example.com\",\"email_verified\":true}");
  assertEquals("gitlab",WeaverGitlabGateway.identity(profile).provider());
  ((com.fasterxml.jackson.databind.node.ObjectNode)profile).put("email_verified",false);
  assertThrows(ResponseStatusException.class,()->WeaverGitlabGateway.identity(profile));
  String url=new WeaverGitlabGateway("id","test-only-secret","https://example.com/callback","https://gitlab.com").authorize("state");
  assertTrue(url.contains("openid+profile+email"));assertFalse(url.contains("read_repository"));assertFalse(url.contains("test-only-secret"));
 }
 @Test void namesAreTrimmedAndBounded(){
  assertEquals("민수",WeaverAuthService.name("  민수  "));
  assertThrows(ResponseStatusException.class,()->WeaverAuthService.name(" "));
  assertThrows(ResponseStatusException.class,()->WeaverAuthService.name("a".repeat(81)));
  assertThrows(ResponseStatusException.class,()->WeaverAuthService.name("name\nother"));
 }
}
