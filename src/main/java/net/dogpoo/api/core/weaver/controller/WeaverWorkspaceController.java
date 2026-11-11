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
 @org.springframework.beans.factory.annotation.Autowired private WeaverSharingService sharing;
 @org.springframework.beans.factory.annotation.Autowired private WeaverHistoryService history;
 @org.springframework.beans.factory.annotation.Autowired private WeaverVerificationMail mail;
 @org.springframework.beans.factory.annotation.Autowired private WeaverAuthRateLimit limits;
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
 @PostMapping("/read") public Map<String,Object> read(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var user=account(platform,r);var target=sharing.access((String)user.get("id"),request.workspaceId,false);return workspaces.read(target.id(),target.email(),(String)user.get("id"),request.since);}
 @PostMapping({"/save","/claim"}) public Map<String,Object> save(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var user=account(platform,r);var target=sharing.access((String)user.get("id"),request.workspaceId,true);return workspaces.save(target.id(),target.email(),(String)user.get("id"),request,r.getRequestURI().endsWith("/claim"));}
 @PostMapping("/media/write") public Map<String,Object> mediaWrite(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,true);return workspaces.mediaWrite(target.id(),target.email(),(String)u.get("id"),request);}
 @PostMapping("/media/read") public Map<String,Object> mediaRead(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,false);return workspaces.mediaRead(target.id(),target.email(),(String)u.get("id"),request);}
 @PostMapping("/document-lease") public Map<String,Object> documentLease(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,true);return workspaces.documentLease(target.id(),target.email(),(String)u.get("id"),request);}
 @PostMapping("/catalog") public Map<String,Object> catalog(@PathVariable String platform,HttpServletRequest r){var u=account(platform,r);return sharing.catalog((String)u.get("id"),(String)u.get("email"));}
 @PostMapping("/members") public Map<String,Object> members(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);String user=(String)u.get("id");return sharing.list(user,request.workspaceId==null?user:request.workspaceId);}
 @PostMapping("/invite") public Map<String,Object> invite(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);String user=(String)u.get("id");limits.check("invite:"+user,5);var result=new HashMap<>(sharing.invite(user,request.workspaceId==null?user:request.workspaceId,request.email,request.role));boolean sent=true;try{mail.invite((String)result.get("email"),(String)result.get("token"),(String)result.get("role"));}catch(ResponseStatusException e){sent=false;}result.put("mailSent",sent);return result;}
 @PostMapping("/accept") public Map<String,Object> accept(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);return sharing.accept((String)u.get("id"),(String)u.get("email"),request.token,request.inviteId);}
 @PostMapping({"/revoke","/remove-member","/member-role"}) public Map<String,Object> change(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);String user=(String)u.get("id"),workspace=request.workspaceId==null?user:request.workspaceId;
  if(r.getRequestURI().endsWith("/revoke"))sharing.revoke(user,workspace,request.inviteId);else if(r.getRequestURI().endsWith("/remove-member"))sharing.remove(user,workspace,request.userId);else sharing.setRole(user,workspace,request.userId,request.role);return Map.of("updated",true);
 }
 @PostMapping("/history") public Map<String,Object> history(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,false);return Map.of("versions",history.list(target.id(),request.documentId,request.beforeVersion));}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"인증된 계정의 자료만 사용할 수 있습니다.":e.getReason()));}
 @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class) public ResponseEntity<Map<String,String>> duplicate(){return ResponseEntity.status(409).body(Map.of("message","이미 처리 중인 가져오기입니다. 같은 계정으로 다시 시도해주세요."));}
}
