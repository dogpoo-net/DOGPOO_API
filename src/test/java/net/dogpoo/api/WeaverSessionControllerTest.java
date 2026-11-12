package net.dogpoo.api;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import net.dogpoo.api.core.weaver.controller.WeaverSessionController;
import net.dogpoo.api.core.weaver.service.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class WeaverSessionControllerTest {
 @Test void webCookiePersistsFor24HoursAndRequiresOriginAndProof() throws Exception {
  var service=mock(WeaverSessionService.class);
  when(service.login(any(),eq("web"),any())).thenAnswer(i->new HashMap<>(Map.of("id","id","email","a@example.com","expiresIn",1800,"cookieToken","private-cookie")));
  var mvc=MockMvcBuilders.standaloneSetup(new WeaverSessionController(service,new WeaverAuthRateLimit(),mock(WeaverKakaoGateway.class),mock(WeaverSocialAccountService.class),mock(WeaverOAuthService.class),"https://weaver.dogpoo.net")).build();
  String route="/weaver/api/auth/session/web/login", body="{\"email\":\"a@example.com\",\"password\":\"long-password\"}";
  mvc.perform(post(route).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post(route).header("Origin","https://evil.dogpoo.net").contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post(route).header("Origin","https://weaver.dogpoo.net").header("X-Weaver-Tab","a".repeat(32)).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
  var result=mvc.perform(post(route).header("Origin","https://weaver.dogpoo.net").header("X-Weaver-Tab","a".repeat(32)).header("X-Weaver-Proof","b".repeat(43)).contentType("application/json").content(body))
    .andExpect(status().isOk()).andExpect(jsonPath("$.cookieToken").doesNotExist()).andExpect(jsonPath("$.refreshToken").doesNotExist()).andExpect(header().string("Cache-Control","no-store")).andReturn();
  String cookie=result.getResponse().getHeader("Set-Cookie");
  assertTrue(cookie.startsWith("__Host-weaver-"));assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("SameSite=Strict"));
  assertTrue(cookie.contains("Max-Age=86400"));assertFalse(cookie.contains("Domain="));
 }
 @Test void invitationTabReceivesIndependentCookieOnlyAfterSourceAuthentication() throws Exception {
  var service=mock(WeaverSessionService.class);
  String source="a".repeat(32), target="c".repeat(32), proof="b".repeat(43), targetProof="d".repeat(43);
  when(service.attachWeb("source-cookie",proof,targetProof)).thenAnswer(i->new HashMap<>(Map.of("id","account","cookieToken","new-cookie")));
  var mvc=MockMvcBuilders.standaloneSetup(new WeaverSessionController(service,new WeaverAuthRateLimit(),mock(WeaverKakaoGateway.class),mock(WeaverSocialAccountService.class),mock(WeaverOAuthService.class),"https://weaver.dogpoo.net")).build();
  String route="/weaver/api/auth/session/web/attach";
  String body="{\"tabId\":\""+target+"\",\"proof\":\""+targetProof+"\"}";
  mvc.perform(post(route).header("Origin","https://evil.example").contentType("application/json").content(body)).andExpect(status().isForbidden());
  verifyNoInteractions(service);
  var result=mvc.perform(post(route).header("Origin","https://weaver.dogpoo.net").header("X-Weaver-Tab",source).header("X-Weaver-Proof",proof).cookie(new jakarta.servlet.http.Cookie("__Host-weaver-"+source,"source-cookie")).contentType("application/json").content(body))
   .andExpect(status().isOk()).andExpect(jsonPath("$.cookieToken").doesNotExist()).andReturn();
  String cookie=result.getResponse().getHeader("Set-Cookie");
  assertTrue(cookie.startsWith("__Host-weaver-"+target+"=new-cookie"));
  assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("SameSite=Strict"));assertTrue(cookie.contains("Max-Age=86400"));
  verify(service).attachWeb("source-cookie",proof,targetProof);
  mvc.perform(post(route).header("Origin","https://weaver.dogpoo.net").header("X-Weaver-Tab",source).header("X-Weaver-Proof",proof).contentType("application/json").content(body.replace(target,source))).andExpect(status().isBadRequest());
  when(service.attachWeb(null,proof,targetProof)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
  mvc.perform(post(route).header("Origin","https://weaver.dogpoo.net").header("X-Weaver-Tab",source).header("X-Weaver-Proof",proof).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
 }
}
