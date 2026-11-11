package net.dogpoo.api.core.weaver.controller;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.*;
import net.dogpoo.api.core.weaver.model.*;
@RestController @RequestMapping("/weaver/api/auth/session/{platform:web|native}/workspace")
public class WeaverWorkspaceController {
 private final WeaverSessionService sessions; private final WeaverWorkspaceService workspaces; private final String origin;
 public WeaverWorkspaceController(WeaverSessionService sessions,WeaverWorkspaceService workspaces,@Value("${WEAVER_WEB_ORIGIN:https://weaver.dogpoo.net}") String origin){this.sessions=sessions;this.workspaces=workspaces;this.origin=origin;}
 private Map<String,Object> account(String platform,HttpServletRequest r){
  if("native".equals(platform))return sessions.me(r.getHeader("Authorization"));
  if(!origin.equals(r.getHeader("Origin")))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  String tab=r.getHeader("X-Weaver-Tab"),proof=r.getHeader("X-Weaver-Proof"),token=null;
  if(tab==null || !tab.matches("[a-f0-9]{32}") || proof==null || !proof.matches("[A-Za-z0-9_-]{43}"))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  if(r.getCookies()!=null)for(var cookie:r.getCookies())if(cookie.getName().equals("__Host-weaver-"+tab))token=cookie.getValue();
  return sessions.webResume(token,proof);
 }
 @ModelAttribute public void headers(HttpServletResponse r){r.setHeader("Cache-Control","no-store");}
 @PostMapping("/read") public Map<String,Object> read(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var user=account(platform,r);return workspaces.read((String)user.get("id"),(String)user.get("email"),request.since);}
 @PostMapping({"/save","/claim"}) public Map<String,Object> save(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var user=account(platform,r);return workspaces.save((String)user.get("id"),(String)user.get("email"),request,r.getRequestURI().endsWith("/claim"));}
 @PostMapping("/media/write") public Map<String,Object> mediaWrite(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var u=account(platform,r);return workspaces.mediaWrite((String)u.get("id"),(String)u.get("email"),request);}
 @PostMapping("/media/read") public Map<String,Object> mediaRead(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var u=account(platform,r);return workspaces.mediaRead((String)u.get("id"),(String)u.get("email"),request);}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"인증된 계정의 자료만 사용할 수 있습니다.":e.getReason()));}
 @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class) public ResponseEntity<Map<String,String>> duplicate(){return ResponseEntity.status(409).body(Map.of("message","이미 처리 중인 가져오기입니다. 같은 계정으로 다시 시도해주세요."));}
}
