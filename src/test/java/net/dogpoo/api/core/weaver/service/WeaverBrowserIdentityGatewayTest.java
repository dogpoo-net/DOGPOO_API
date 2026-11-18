package net.dogpoo.api.core.weaver.service;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class WeaverBrowserIdentityGatewayTest {
 private WeaverBrowserIdentityGateway gateway(String secret){return new WeaverBrowserIdentityGateway("web-id",secret,"rest-id",secret,"https://api.example/auth",null,null);}
 @Test void authorizationUsesServerCallbackAndKeepsSecretsOffBrowser(){
  for(String provider:new String[]{"google","kakao"}){
   String url=gateway("PRIVATE_SECRET").authorize(provider,"request-state");
   assertTrue(url.contains("state=request-state"));
   assertTrue(url.contains("https%3A%2F%2Fapi.example%2Fauth%2F"+provider+"%2Fcallback"));
   assertFalse(url.contains("PRIVATE_SECRET"));
   assertTrue(url.startsWith(provider.equals("google")?"https://accounts.google.com/":"https://kauth.kakao.com/"));
  }
 }
 @Test void missingSecretAndUnknownProviderFailBeforeStartingFlow(){
  assertThrows(ResponseStatusException.class,()->gateway("").authorize("google","s"));
  assertThrows(ResponseStatusException.class,()->gateway("").authorize("kakao","s"));
  assertThrows(ResponseStatusException.class,()->gateway("secret").authorize("unknown","s"));
 }
}
