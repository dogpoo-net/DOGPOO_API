package net.dogpoo.api.core.weaver.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.WeaverAuthMapper;
import net.dogpoo.api.core.weaver.model.*;
import java.time.*;

class WeaverAuthServiceTest {
 WeaverAuthMapper mapper=mock(WeaverAuthMapper.class);
 WeaverVerificationMail mail=mock(WeaverVerificationMail.class);
 BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
 WeaverAuthService service=new WeaverAuthService(mapper,encoder,mail);
 WeaverAuthRequest request(){var r=new WeaverAuthRequest();r.email="Person@Example.com";r.password="test-only-long-password";r.passwordConfirmation=r.password;return r;}
 @Test void signupHashesPasswordAndToken() {
  var r=request();service.signup(r);
  verify(mapper).insertAccount(anyString(),eq("person@example.com"),argThat(h->!h.equals(r.password)&&encoder.matches(r.password,h)));
  var token=org.mockito.ArgumentCaptor.forClass(String.class);
  verify(mail).send(eq("person@example.com"),token.capture());
  assertEquals(43,token.getValue().length());
  verify(mapper).token(anyString(),eq(WeaverAuthService.hash(token.getValue())));
 }
 @Test void rejectsMismatchAndBcryptTruncation() {
  var r=request();r.passwordConfirmation="different";
  assertThrows(ResponseStatusException.class,()->service.signup(r));
  r.password="가".repeat(25);r.passwordConfirmation=r.password;
  assertThrows(ResponseStatusException.class,()->service.signup(r));verifyNoInteractions(mapper,mail);
 }
 @Test void existingEmailNeverReplacesPasswordOrSendsMail() {
  when(mapper.account("person@example.com")).thenReturn(new WeaverAccount("id","person@example.com","hash",null));
  service.signup(request());verify(mapper,never()).insertAccount(any(),any(),any());verifyNoInteractions(mail);
 }
 @Test void loginRejectsWrongPasswordAndUnverifiedAccount() {
  var r=request();when(mapper.account("person@example.com")).thenReturn(new WeaverAccount("id","person@example.com",encoder.encode(r.password),null));
  assertEquals(403,assertThrows(ResponseStatusException.class,()->service.login(r)).getStatusCode().value());
  r.password="wrong-password-value";
  assertEquals(401,assertThrows(ResponseStatusException.class,()->service.login(r)).getStatusCode().value());
  verify(mapper,never()).session(any(),any());
 }
 @Test void verifiedLoginReturnsOpaqueSessionStoredOnlyHashed() {
  var r=request();when(mapper.account("person@example.com")).thenReturn(new WeaverAccount("id","person@example.com",encoder.encode(r.password),LocalDateTime.now()));
  var result=service.login(r);String token=(String)result.get("sessionToken");
  verify(mapper).session(WeaverAuthService.hash(token),"id");assertFalse(result.containsKey("passwordHash"));
 }
 @Test void invalidExpiredOrReusedVerificationCannotSucceed() {
  assertThrows(ResponseStatusException.class,()->service.verify("invalid"));
  String token="a".repeat(43);when(mapper.verify(WeaverAuthService.hash(token))).thenReturn(1,0);
  service.verify(token);assertThrows(ResponseStatusException.class,()->service.verify(token));
  verify(mapper,times(1)).consume(WeaverAuthService.hash(token));
 }
 @Test void resendIsThrottledAndVerifiedAccountsAreIgnored() {
  when(mapper.lockAccount("person@example.com")).thenReturn(new WeaverAccount("id","person@example.com","hash",null));
  when(mapper.recentlySent("id")).thenReturn(1);service.resend("person@example.com");verifyNoInteractions(mail);
 }
 @Test void expiredSessionCannotAuthenticate() {
  assertEquals(401,assertThrows(ResponseStatusException.class,()->service.me("Bearer "+"a".repeat(43))).getStatusCode().value());
 }
 @Test void limitsRejectExcessRequests() {
  var limiter=new WeaverAuthRateLimit(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));
  limiter.check("account",1);
  assertEquals(429,assertThrows(ResponseStatusException.class,()->limiter.check("account",1)).getStatusCode().value());
 }
}
