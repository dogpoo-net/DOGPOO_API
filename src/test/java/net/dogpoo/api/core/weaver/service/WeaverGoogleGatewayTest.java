package net.dogpoo.api.core.weaver.service;
import org.junit.jupiter.api.Test;import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class WeaverGoogleGatewayTest {
 @Test void requiresGoogleIssuerAudienceVerifiedEmailAndUnexpiredToken() throws Exception {
  var p=new ObjectMapper().readTree("{\"aud\":\"client\",\"iss\":\"https://accounts.google.com\",\"exp\":2000,\"email_verified\":true,\"email\":\"a@example.com\",\"sub\":\"123\",\"name\":\"User\"}");
  assertEquals("google",WeaverGoogleGateway.identity(p,"client",1000).provider());
  assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.identity(p,"other",1000));
  assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.identity(p,"client",3000));
  var object=(com.fasterxml.jackson.databind.node.ObjectNode)p;object.put("email_verified",false);assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.identity(p,"client",1000));
  object.put("email_verified",true);object.put("iss","https://evil.example");assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.identity(p,"client",1000));
 }
 @Test void customButtonTokenRequiresClientExpirySubjectAndVerifiedProfile() throws Exception {
  var mapper=new ObjectMapper();
  var claims=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree("{\"aud\":\"client\",\"azp\":\"client\",\"sub\":\"123\",\"exp\":2000}");
  var profile=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree("{\"sub\":\"123\",\"email\":\"test@example.com\",\"email_verified\":true}");
  assertEquals("test",WeaverGoogleGateway.accessIdentity(claims,profile,"client",1000).name());
  assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.accessIdentity(claims,profile,"other",1000));
  assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.accessIdentity(claims,profile,"client",3000));
  profile.put("sub","other");assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.accessIdentity(claims,profile,"client",1000));
  profile.put("sub","123");profile.put("email_verified",false);assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.accessIdentity(claims,profile,"client",1000));
  profile.put("email_verified",true);claims.put("azp","other");assertThrows(ResponseStatusException.class,()->WeaverGoogleGateway.accessIdentity(claims,profile,"client",1000));
 }
 @Test void missingOrOversizedProviderNamesHaveUsableFallbacks(){
  assertEquals("user",WeaverAuthService.socialName(null,"user@example.com"));
  assertEquals("user",WeaverAuthService.socialName("  ","user@example.com"));
  assertEquals(80,WeaverAuthService.socialName("가".repeat(100),"user@example.com").length());
 }
}
