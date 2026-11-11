package net.dogpoo.api.core.weaver.controller;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.model.WeaverAuthRequest;
import net.dogpoo.api.core.weaver.service.*;

@RestController
@RequestMapping("/weaver/api/auth")
public class WeaverAuthController {
 private final WeaverAuthService service;
 private final WeaverAuthRateLimit limits;
 public WeaverAuthController(WeaverAuthService service,WeaverAuthRateLimit limits){this.service=service;this.limits=limits;}
 private void limit(HttpServletRequest r,WeaverAuthRequest body) {
  limits.check("ip:"+r.getRemoteAddr(),100);
  if(body.email!=null) {
   if(body.email.length()>254) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"올바른 이메일 주소를 입력해주세요.");
   limits.check("email:"+body.email.strip().toLowerCase(java.util.Locale.ROOT),5);
  }
 }
 private Map<String,String> sent(){return Map.of("message","가입 가능한 이메일이면 인증메일을 보냈습니다. 받은편지함과 스팸함을 확인해주세요. 기존 계정은 로그인하거나 인증메일을 다시 요청해주세요.");}
 @PostMapping("/signup") public Map<String,String> signup(@RequestBody WeaverAuthRequest body,HttpServletRequest r){limit(r,body);WeaverAuthService.validateCredentials(body,true);try{service.signup(body);}catch(DuplicateKeyException ignored){}return sent();}
 @PostMapping("/resend") public Map<String,String> resend(@RequestBody WeaverAuthRequest body,HttpServletRequest r){limit(r,body);service.resend(body.email);return sent();}
 @PostMapping("/verify") public Map<String,String> verify(@RequestBody WeaverAuthRequest body,HttpServletRequest r){limit(r,body);service.verify(body.token);return Map.of("message","이메일 인증이 완료되었습니다. 로그인해주세요.");}
 @PostMapping("/login") public Map<String,Object> login(@RequestBody WeaverAuthRequest body,HttpServletRequest r){limit(r,body);WeaverAuthService.validateCredentials(body,false);return service.login(body);}
 @GetMapping("/me") public Map<String,Object> me(@RequestHeader(value="Authorization",required=false) String auth){return service.me(auth);}
 @PostMapping("/logout") public Map<String,String> logout(@RequestHeader(value="Authorization",required=false) String auth){service.logout(auth);return Map.of("message","로그아웃되었습니다.");}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).header("Cache-Control","no-store").body(Map.of("message",e.getReason()==null?"요청을 처리하지 못했습니다.":e.getReason()));}
 @ModelAttribute public void noCache(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");}
}
