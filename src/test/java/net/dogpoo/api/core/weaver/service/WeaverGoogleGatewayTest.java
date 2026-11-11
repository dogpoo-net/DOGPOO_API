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
}
