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
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"로그인을 확인해주세요.":e.getReason()));}
}
