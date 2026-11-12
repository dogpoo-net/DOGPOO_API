package net.dogpoo.api.core.weaver.controller;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.*;
import net.dogpoo.api.core.weaver.model.*;
@RestController @RequestMapping("/weaver/api/auth/session/{platform:web|native}/profile")
public class WeaverProfileController {
 @org.springframework.beans.factory.annotation.Autowired private WeaverWithdrawalService withdrawal;
 @org.springframework.beans.factory.annotation.Autowired private WeaverErasureService erasure;
 private final WeaverSessionService sessions;private final WeaverProfileService profiles;private final WeaverAuthRateLimit limits;private final String origin;
 public WeaverProfileController(WeaverSessionService sessions,WeaverProfileService profiles,WeaverAuthRateLimit limits,@Value("${WEAVER_WEB_ORIGIN:https://weaver.dogpoo.net}") String origin){this.sessions=sessions;this.profiles=profiles;this.limits=limits;this.origin=origin;}
 private Map<String,Object> account(String platform,HttpServletRequest r){
  if("native".equals(platform))return sessions.me(r.getHeader("Authorization"));
  if(!origin.equals(r.getHeader("Origin")))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  String tab=r.getHeader("X-Weaver-Tab"),proof=r.getHeader("X-Weaver-Proof"),token=null;
  if(tab==null || !tab.matches("[a-f0-9]{32}") || proof==null || !proof.matches("[A-Za-z0-9_-]{43}"))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  if(r.getCookies()!=null)for(var cookie:r.getCookies())if(cookie.getName().equals("__Host-weaver-"+tab))token=cookie.getValue();
  return sessions.webResume(token,proof);
 }

 @ModelAttribute public void headers(HttpServletResponse r){r.setHeader("Cache-Control","no-store");}
 @PostMapping("/read") public Map<String,Object> read(@PathVariable String platform,HttpServletRequest r){return profiles.read((String)account(platform,r).get("id"));}
 @PostMapping({"/name","/photo","/password/request","/password/change"}) public Map<String,Object> update(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverProfileRequest request){
  String id=(String)account(platform,r).get("id");String path=r.getRequestURI();limits.check("profile:"+id,30);
  if(path.endsWith("/name"))profiles.name(id,request.name);
  else if(path.endsWith("/photo"))profiles.photo(id,request.image);
  else if(path.endsWith("/password/request")){limits.check("password-mail:"+id,3);profiles.requestPassword(id);}
  else{limits.check("password-change:"+id,10);profiles.changePassword(id,request);}
  return Map.of("updated",true);
 }
 @PostMapping("/withdrawal/preview") public Map<String,Object> withdrawalPreview(@PathVariable String platform,HttpServletRequest r){return withdrawal.preview((String)account(platform,r).get("id"));}
 @PostMapping("/withdrawal/request") public Map<String,Object> withdrawalRequest(@PathVariable String platform,HttpServletRequest r){String id=(String)account(platform,r).get("id");limits.check("withdraw-mail:"+id,3);withdrawal.request(id);return Map.of("sent",true);}
 @PostMapping("/withdrawal/confirm") public Map<String,Object> withdraw(@PathVariable String platform,HttpServletRequest r,HttpServletResponse response,@RequestBody WeaverWithdrawalRequest request){
  String id=(String)account(platform,r).get("id");limits.check("withdraw:"+id,10);
  if(!withdrawal.verify(id,request.code))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"인증코드가 올바르지 않거나 만료되었습니다.");
  withdrawal.erase(id,request);
  if("web".equals(platform))response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("__Host-weaver-"+r.getHeader("X-Weaver-Tab"),"").httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(0).build().toString());
  try{erasure.drain();}catch(RuntimeException e){/* Durable jobs retry independently of the deleted account. */}
  return Map.of("deleted",true);
 }
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"로그인을 확인해주세요.":e.getReason()));}
}
