package net.dogpoo.api.core.weaver.controller;

import java.time.Duration;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.*;
import net.dogpoo.api.core.weaver.model.*;

@RestController
@RequestMapping("/weaver/api/auth/session")
public class WeaverSessionController {
 private final WeaverSessionService sessions;
 private final WeaverAuthRateLimit limits;
 private final String origin;
 private final WeaverKakaoGateway kakaoGateway;
 private final WeaverSocialAccountService kakao;
 private final WeaverOAuthService oauth;
 @org.springframework.beans.factory.annotation.Autowired private WeaverGoogleGateway google;
 public WeaverSessionController(WeaverSessionService sessions,WeaverAuthRateLimit limits,WeaverKakaoGateway kakaoGateway,WeaverSocialAccountService kakao,WeaverOAuthService oauth,
  @Value("${WEAVER_WEB_ORIGIN:https://weaver.dogpoo.net}") String origin){this.sessions=sessions;this.limits=limits;this.origin=origin;this.kakaoGateway=kakaoGateway;this.kakao=kakao;this.oauth=oauth;}
 @ModelAttribute public void guard(HttpServletRequest request,HttpServletResponse response){
  response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");
  limits.check("session:"+request.getRemoteAddr(),1000);
  if(request.getRequestURI().contains("/session/web/")){
   if(!origin.equals(request.getHeader("Origin")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"허용되지 않은 웹 출처입니다.");
   tab(request);
   String proof=request.getHeader("X-Weaver-Proof");
   if(proof==null || !proof.matches("[A-Za-z0-9_-]{43}"))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"탭 세션이 필요합니다.");
  }
 }
 private String tab(HttpServletRequest r){
  String tab=r.getHeader("X-Weaver-Tab");
  if(tab==null || !tab.matches("[a-f0-9]{32}"))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"탭 세션이 필요합니다.");
  return tab;
 }
 private String cookieName(HttpServletRequest r){return "__Host-weaver-"+tab(r);}
 private String cookie(HttpServletRequest r){
  String name=cookieName(r);if(r.getCookies()!=null)for(Cookie c:r.getCookies())if(name.equals(c.getName()))return c.getValue();return null;
 }
 private void cookie(HttpServletRequest r,HttpServletResponse response,String value,boolean delete){
  var builder=ResponseCookie.from(cookieName(r),value).httpOnly(true).secure(true).sameSite("Strict").path("/");
  builder.maxAge(delete ? Duration.ZERO : Duration.ofHours(24));
  response.addHeader(HttpHeaders.SET_COOKIE,builder.build().toString());
 }
 private void credentials(WeaverAuthRequest request){WeaverAuthService.validateCredentials(request,false);limits.check("login:"+WeaverAuthService.email(request.email),5);}
 private WeaverSocialIdentity googleIdentity(WeaverSessionRequest request){
  return request.googleAccessToken!=null?google.verifyAccessToken(request.googleAccessToken):google.verify(request.idToken);
 }
 private String provider(HttpServletRequest r){return r.getRequestURI().contains("/github/")?"github":r.getRequestURI().contains("/gitlab/")?"gitlab":"naver";}
 @PostMapping({"/native/naver/start","/native/github/start","/native/gitlab/start"}) public Map<String,Object> nativeOAuthStart(@RequestBody WeaverSessionRequest request,HttpServletRequest r,@RequestHeader(value="Authorization",required=false) String bearer){
  String link=request.link?(String)sessions.me(bearer).get("id"):null;return oauth.start(provider(r),"native",request.proof,link);
 }
 @PostMapping({"/web/naver/start","/web/github/start","/web/gitlab/start"}) public Map<String,Object> webOAuthStart(@RequestBody WeaverSessionRequest request,HttpServletRequest r){
  String link=request.link?(String)sessions.webResume(cookie(r),r.getHeader("X-Weaver-Proof")).get("id"):null;return oauth.start(provider(r),"web",r.getHeader("X-Weaver-Proof"),link);
 }
 @PostMapping({"/native/naver/finish","/native/github/finish","/native/gitlab/finish"}) public Map<String,Object> nativeOAuthFinish(@RequestBody WeaverSessionRequest request,HttpServletRequest r,@RequestHeader(value="Authorization",required=false) String bearer){return oauth.finish(provider(r),request.flow,"native",request.proof,bearer,null,null);}
 @PostMapping({"/web/naver/finish","/web/github/finish","/web/gitlab/finish"}) public Map<String,Object> webOAuthFinish(@RequestBody WeaverSessionRequest request,HttpServletRequest r,HttpServletResponse response){
  var result=oauth.finish(provider(r),request.flow,"web",r.getHeader("X-Weaver-Proof"),null,cookie(r),r.getHeader("X-Weaver-Proof"));
  if(result.containsKey("cookieToken"))cookie(r,response,(String)result.remove("cookieToken"),false);return result;
 }
 @PostMapping("/native/google") public Map<String,Object> nativeGoogle(@RequestBody WeaverSessionRequest request){return sessions.issue(kakao.resolve(googleIdentity(request),null),"native",null);}
 @PostMapping("/web/google") public Map<String,Object> webGoogle(@RequestBody WeaverSessionRequest request,HttpServletRequest r,HttpServletResponse response){
  var result=sessions.issue(kakao.resolve(googleIdentity(request),null),"web",r.getHeader("X-Weaver-Proof"));cookie(r,response,(String)result.remove("cookieToken"),false);return result;
 }
 @PostMapping("/native/kakao") public Map<String,Object> nativeKakao(@RequestBody WeaverSessionRequest request){
  var account=kakao.resolve(kakaoGateway.verify(request.kakaoToken),null);return sessions.issue(account,"native",null);
 }
 @PostMapping("/web/kakao") public Map<String,Object> webKakao(@RequestBody WeaverSessionRequest request,HttpServletRequest r,HttpServletResponse response){
  var account=kakao.resolve(kakaoGateway.verify(request.kakaoToken),null);
  var result=sessions.issue(account,"web",r.getHeader("X-Weaver-Proof"));cookie(r,response,(String)result.remove("cookieToken"),false);return result;
 }
 @PostMapping("/native/google/link") public Map<String,String> nativeGoogleLink(@RequestBody WeaverSessionRequest request,@RequestHeader(value="Authorization",required=false) String bearer){
  kakao.resolve(googleIdentity(request),(String)sessions.me(bearer).get("id"));return Map.of("message","Google 계정을 연결했습니다.");
 }
 @PostMapping("/web/google/link") public Map<String,String> webGoogleLink(@RequestBody WeaverSessionRequest request,HttpServletRequest r){
  kakao.resolve(googleIdentity(request),(String)sessions.webResume(cookie(r),r.getHeader("X-Weaver-Proof")).get("id"));return Map.of("message","Google 계정을 연결했습니다.");
 }
 @PostMapping("/native/kakao/link") public Map<String,String> nativeKakaoLink(@RequestBody WeaverSessionRequest request,@RequestHeader(value="Authorization",required=false) String bearer){
  String id=(String)sessions.me(bearer).get("id");kakao.resolve(kakaoGateway.verify(request.kakaoToken),id);return Map.of("message","카카오 계정을 연결했습니다.");
 }
 @PostMapping("/web/kakao/link") public Map<String,String> webKakaoLink(@RequestBody WeaverSessionRequest request,HttpServletRequest r){
  String id=(String)sessions.webResume(cookie(r),r.getHeader("X-Weaver-Proof")).get("id");kakao.resolve(kakaoGateway.verify(request.kakaoToken),id);return Map.of("message","카카오 계정을 연결했습니다.");
 }
 @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class) public ResponseEntity<Map<String,String>> duplicate(){return ResponseEntity.status(409).body(Map.of("message","이미 연결된 계정입니다. 로그인 상태를 확인해주세요."));}
 @PostMapping("/native/login") public Map<String,Object> nativeLogin(@RequestBody WeaverSessionRequest request){credentials(request);return sessions.login(request,"native",null);}
 @PostMapping("/native/refresh") public Map<String,Object> refresh(@RequestBody WeaverSessionRequest request){return sessions.refresh(request.refreshToken,request.refreshId);}
 @GetMapping("/native/me") public Map<String,Object> me(@RequestHeader(value="Authorization",required=false) String bearer){return sessions.me(bearer);}
 @PostMapping("/native/logout") public Map<String,String> logout(@RequestBody WeaverSessionRequest request){sessions.logout(request.refreshToken,false);return Map.of("message","이 기기에서 로그아웃했습니다.");}
 @PostMapping("/native/logout-all") public Map<String,String> logoutAll(@RequestBody WeaverSessionRequest request){sessions.logout(request.refreshToken,true);return Map.of("message","모든 기기에서 로그아웃했습니다.");}
 @PostMapping("/web/login") public Map<String,Object> webLogin(@RequestBody WeaverSessionRequest request,HttpServletRequest r,HttpServletResponse response){
  credentials(request);var result=sessions.login(request,"web",r.getHeader("X-Weaver-Proof"));
  cookie(r,response,(String)result.remove("cookieToken"),false);return result;
 }
 @PostMapping("/web/attach") public Map<String,Object> attachWeb(@RequestBody WeaverSessionRequest request,HttpServletRequest r,HttpServletResponse response){
  if(request.tabId==null || !request.tabId.matches("[a-f0-9]{32}") || request.tabId.equals(tab(r)) || request.proof==null || !request.proof.matches("[A-Za-z0-9_-]{43}"))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"새 탭 정보가 올바르지 않습니다.");
  limits.check("attach:"+tab(r),10);
  var result=sessions.attachWeb(cookie(r),r.getHeader("X-Weaver-Proof"),request.proof);
  response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("__Host-weaver-"+request.tabId,(String)result.remove("cookieToken")).httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(Duration.ofHours(24)).build().toString());
  return result;
 }
 @PostMapping("/web/refresh") public Map<String,Object> webRefresh(HttpServletRequest r,HttpServletResponse response){var result=sessions.webResume(cookie(r),r.getHeader("X-Weaver-Proof"));cookie(r,response,cookie(r),false);return result;}
 @PostMapping("/web/logout") public Map<String,String> webLogout(HttpServletRequest r,HttpServletResponse response){return webEnd(r,response,false);}
 @PostMapping("/web/logout-all") public Map<String,String> webLogoutAll(HttpServletRequest r,HttpServletResponse response){return webEnd(r,response,true);}
 private Map<String,String> webEnd(HttpServletRequest r,HttpServletResponse response,boolean all){
  try{sessions.webLogout(cookie(r),r.getHeader("X-Weaver-Proof"),all);}
  catch(ResponseStatusException e){if(all || e.getStatusCode().value()!=401)throw e;}
  cookie(r,response,"",true);return Map.of("message",all?"모든 기기에서 로그아웃했습니다.":"로그아웃했습니다.");
 }
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).header("Cache-Control","no-store").body(Map.of("message",e.getReason()==null?"인증 요청을 처리하지 못했습니다.":e.getReason()));}
}
