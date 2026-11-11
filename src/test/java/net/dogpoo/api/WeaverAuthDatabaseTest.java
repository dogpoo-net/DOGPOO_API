package net.dogpoo.api;

import java.util.UUID;
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
class WeaverAuthDatabaseTest {
 @Autowired WeaverAuthService service;
 @Autowired JdbcTemplate jdbc;
 @MockitoBean WeaverVerificationMail mail;
 String email="auth-test-"+UUID.randomUUID()+"@example.invalid";
 @AfterEach void cleanup(){jdbc.update("DELETE FROM WEAVER_USER_ACCOUNT WHERE email=?",email);}
 @Test void realDatabaseVerificationSessionAndRollback() {
  var r=new WeaverAuthRequest();r.email=email;r.password="test-only-long-password";r.passwordConfirmation=r.password;
  service.signup(r);
  var token=org.mockito.ArgumentCaptor.forClass(String.class);verify(mail).send(eq(email),token.capture());
  assertEquals(403,assertThrows(ResponseStatusException.class,()->service.login(r)).getStatusCode().value());
  jdbc.update("UPDATE WEAVER_EMAIL_TOKEN t JOIN WEAVER_USER_ACCOUNT a ON a.id=t.user_id SET t.expires_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 SECOND) WHERE a.email=?",email);
  assertThrows(ResponseStatusException.class,()->service.verify(token.getValue()));
  jdbc.update("UPDATE WEAVER_EMAIL_TOKEN t JOIN WEAVER_USER_ACCOUNT a ON a.id=t.user_id SET t.sent_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 2 MINUTE) WHERE a.email=?",email);
  service.resend(email);
  verify(mail,times(2)).send(eq(email),token.capture());String latest=token.getValue();
  service.verify(latest);assertThrows(ResponseStatusException.class,()->service.verify(latest));
  var result=service.login(r);String session=(String)result.get("sessionToken");
  assertEquals(email,service.me("Bearer "+session).get("email"));
  service.logout("Bearer "+session);assertThrows(ResponseStatusException.class,()->service.me("Bearer "+session));
  jdbc.update("DELETE FROM WEAVER_USER_ACCOUNT WHERE email=?",email);
  doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)).when(mail).send(eq(email),anyString());
  assertThrows(ResponseStatusException.class,()->service.signup(r));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM WEAVER_USER_ACCOUNT WHERE email=?",Integer.class,email));
 }
}
