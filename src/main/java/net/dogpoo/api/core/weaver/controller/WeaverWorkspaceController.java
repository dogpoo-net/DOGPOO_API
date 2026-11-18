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
 @org.springframework.beans.factory.annotation.Autowired private WeaverCommentService comments;
 @org.springframework.beans.factory.annotation.Autowired private WeaverWorkspaceEvents events;
 @org.springframework.beans.factory.annotation.Autowired private WeaverWorkspaceLifecycleService lifecycle;
 @org.springframework.beans.factory.annotation.Autowired private WeaverWorkspacePolicyService policy;
 @org.springframework.beans.factory.annotation.Autowired private WeaverAudioTranscriptionService audio;
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
 @PostMapping(value="/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
 public org.springframework.web.servlet.mvc.method.annotation.SseEmitter events(@PathVariable String platform,HttpServletRequest r,HttpServletResponse response,@RequestBody WeaverWorkspaceRequest request){
  var user=account(platform,r);String actor=(String)user.get("id");var target=sharing.access(actor,request.workspaceId,false);
  String authorization=r.getHeader("Authorization"),proof=r.getHeader("X-Weaver-Proof"),cookieName="__Host-weaver-"+r.getHeader("X-Weaver-Tab");
  String token=null;if(r.getCookies()!=null)for(var cookie:r.getCookies())if(cookie.getName().equals(cookieName))token=cookie.getValue();
  final String sessionToken=token;
  response.setHeader("X-Accel-Buffering","no");response.setHeader("Cache-Control","no-cache, no-store, no-transform");
  return events.subscribe(target.id(),()->{
   var current="native".equals(platform)?sessions.me(authorization):sessions.webResume(sessionToken,proof);
   if(!actor.equals(current.get("id")))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
   sharing.access(actor,target.id(),false);
  });
 }
 @PostMapping("/status") public Map<String,Object> status(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var user=account(platform,r);var target=sharing.access((String)user.get("id"),request.workspaceId,false);var result=new HashMap<String,Object>(workspaces.status(target.id(),(String)user.get("id"),request));result.put("commentCounts",comments.counts(target.id()));return result;}
 @PostMapping("/read") public Map<String,Object> read(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var user=account(platform,r);var target=sharing.access((String)user.get("id"),request.workspaceId,false);var result=new HashMap<String,Object>(workspaces.read(target.id(),target.email(),(String)user.get("id"),request.since));result.put("commentCounts",comments.counts(target.id()));return result;}
 @PostMapping({"/save","/claim"}) public Map<String,Object> save(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var user=account(platform,r);var target=sharing.access((String)user.get("id"),request.workspaceId,true);return workspaces.save(target.id(),target.email(),(String)user.get("id"),request,r.getRequestURI().endsWith("/claim"));}
 @PostMapping("/media/write") public Map<String,Object> mediaWrite(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,true);return workspaces.mediaWrite(target.id(),target.email(),(String)u.get("id"),request);}
 @PostMapping("/media/read") public Map<String,Object> mediaRead(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,false);return workspaces.mediaRead(target.id(),target.email(),(String)u.get("id"),request);}
 @PostMapping("/document-lease") public Map<String,Object> documentLease(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,true);return workspaces.documentLease(target.id(),target.email(),(String)u.get("id"),request);}
 @PostMapping("/catalog") public Map<String,Object> catalog(@PathVariable String platform,HttpServletRequest r){var u=account(platform,r);return sharing.catalog((String)u.get("id"),(String)u.get("email"));}
 @PostMapping("/create-workspace") public Map<String,Object> createWorkspace(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){String user=(String)account(platform,r).get("id");limits.check("create-workspace:"+user,10);return sharing.createWorkspace(user,request.name);}
 @PostMapping({"/rename-workspace","/delete-workspace","/leave-workspace"}) public Map<String,Object> manageWorkspace(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){String user=(String)account(platform,r).get("id");
  if(r.getRequestURI().endsWith("/rename-workspace"))sharing.renameWorkspace(user,request.workspaceId,request.name);
  else if(r.getRequestURI().endsWith("/delete-workspace")){
   if(request.code!=null && !lifecycle.verify(user,request.workspaceId,request.code))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"인증코드가 올바르지 않거나 만료되었습니다.");
   return lifecycle.delete(user,request.workspaceId,request.code);
  }
  else sharing.leaveWorkspace(user,request.workspaceId);
  return Map.of("updated",true);
 }
 @PostMapping("/delete-workspace-preview") public Map<String,Object> deletionPreview(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){return lifecycle.preview((String)account(platform,r).get("id"),request.workspaceId);}
 @PostMapping("/delete-workspace-email") public Map<String,Object> deletionEmail(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){String user=(String)account(platform,r).get("id");limits.check("workspace-delete-mail:"+user,3);lifecycle.request(user,request.workspaceId);return Map.of("sent",true);}
 @PostMapping("/deleted-workspaces") public Map<String,Object> deletedWorkspaces(@PathVariable String platform,HttpServletRequest r){return lifecycle.archived((String)account(platform,r).get("id"));}
 @PostMapping("/restore-workspace") public Map<String,Object> restoreWorkspace(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){lifecycle.restore((String)account(platform,r).get("id"),request.workspaceId);return Map.of("restored",true);}
 @PostMapping("/members") public Map<String,Object> members(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);String user=(String)u.get("id");return sharing.list(user,request.workspaceId==null?sharing.primary(user):request.workspaceId);}
 @PostMapping("/invite-emails") public Map<String,Object> inviteEmails(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);String user=(String)u.get("id");limits.check("invite-search:"+user,120);return Map.of("emails",sharing.emailSuggestions(user,request.workspaceId,request.email));}
 @PostMapping("/invite") public Map<String,Object> invite(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);String user=(String)u.get("id");limits.check("invite:"+user,5);var result=new HashMap<>(sharing.invite(user,request.workspaceId==null?sharing.primary(user):request.workspaceId,request.email,request.role));boolean sent=true;try{mail.invite((String)result.get("email"),(String)result.get("token"),(String)result.get("role"));}catch(ResponseStatusException e){sent=false;}result.put("mailSent",sent);return result;}
 @PostMapping("/accept") public Map<String,Object> accept(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);return sharing.accept((String)u.get("id"),(String)u.get("email"),request.token,request.inviteId);}
 @PostMapping({"/revoke","/remove-member","/member-role"}) public Map<String,Object> change(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);String user=(String)u.get("id"),workspace=request.workspaceId==null?sharing.primary(user):request.workspaceId;
  if(r.getRequestURI().endsWith("/revoke"))sharing.revoke(user,workspace,request.inviteId);else if(r.getRequestURI().endsWith("/remove-member"))sharing.remove(user,workspace,request.userId);else sharing.setRole(user,workspace,request.userId,request.role);return Map.of("updated",true);
 }
 @PostMapping("/transfer") public Map<String,Object> transfer(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,true);workspaces.read(target.id(),target.email(),(String)u.get("id"),0);sharing.transfer((String)u.get("id"),request.workspaceId,request.userId);return Map.of("updated",true);}
 @PostMapping("/history") public Map<String,Object> history(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var u=account(platform,r);var target=sharing.access((String)u.get("id"),request.workspaceId,false);return Map.of("versions",history.list(target.id(),request.documentId,request.beforeVersion));}
 @PostMapping({"/comments/read","/comments/write"}) public Map<String,Object> comments(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverCommentRequest request){
  String actor=(String)account(platform,r).get("id");boolean write=r.getRequestURI().endsWith("/write");limits.check((write?"comments-write:":"comments-read:")+actor,write?30:180);return comments.handle(actor,request,write);
 }
 @PostMapping("/account-policy") public Map<String,Object> accountPolicy(@PathVariable String platform,HttpServletRequest r){return policy.accountStatus((String)account(platform,r).get("id"));}
 @PostMapping("/subscription") public Map<String,Object> subscription(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){var target=sharing.access((String)account(platform,r).get("id"),request.workspaceId,false);return policy.status(target.id());}
 @PostMapping("/subscription-cancel") public Map<String,Object> subscriptionCancel(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){return policy.cancel((String)account(platform,r).get("id"),request.workspaceId,request.cancel);}
 @PostMapping("/select-active-workspaces") public Map<String,Object> selectSpaces(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){policy.chooseSpaces((String)account(platform,r).get("id"),request.ids);return Map.of("updated",true);}
 @PostMapping("/select-editors") public Map<String,Object> selectEditors(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){policy.chooseEditors((String)account(platform,r).get("id"),request.workspaceId,request.ids);return Map.of("updated",true);}
 @PostMapping("/admin/users") public Map<String,Object> adminUsers(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){String actor=(String)account(platform,r).get("id");limits.check("admin-search:"+actor,120);return policy.adminUsers(actor,request.query);}
 @PostMapping("/admin/account") public Map<String,Object> adminAccount(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){return policy.adminAccount((String)account(platform,r).get("id"),request.userId);}
 @PostMapping("/admin/account-role") public Map<String,Object> adminRole(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){policy.adminRole((String)account(platform,r).get("id"),request.userId,request.role);return Map.of("updated",true);}
 @PostMapping("/admin/subscription") public Map<String,Object> adminSubscription(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){return policy.adminSubscription((String)account(platform,r).get("id"),request.userId,request.plan,request.periodEnd,request.cancel);}
 @PostMapping("/admin/members") public Map<String,Object> adminMembers(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){return Map.of("members",policy.adminMembers((String)account(platform,r).get("id"),request.workspaceId));}
 @PostMapping("/admin/member-role") public Map<String,Object> adminMember(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){policy.adminMember((String)account(platform,r).get("id"),request.workspaceId,request.userId,request.role);return Map.of("updated",true);}
 @PostMapping("/admin/audit") public Map<String,Object> adminAudit(@PathVariable String platform,HttpServletRequest r){return Map.of("events",policy.audit((String)account(platform,r).get("id")));}
 @PostMapping("/audio/transcribe") public Map<String,Object> transcribe(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverWorkspaceRequest request){String actor=(String)account(platform,r).get("id");var target=sharing.access(actor,request.workspaceId,true);limits.check("transcribe:"+actor,3);return audio.transcribe(target.id(),request.data);}
 @PostMapping("/admin/audio-settings") public Map<String,Object> audioSettings(@PathVariable String platform,HttpServletRequest r){return audio.settings((String)account(platform,r).get("id"));}
 @PostMapping("/admin/audio-configure") public Map<String,Object> audioConfigure(@PathVariable String platform,HttpServletRequest r,@RequestBody WeaverSharingRequest request){return audio.configure((String)account(platform,r).get("id"),request.provider,request.model,request.enabled);}
 @ExceptionHandler(RuntimeException.class) public ResponseEntity<Map<String,String>> unexpected(RuntimeException e){
  org.slf4j.LoggerFactory.getLogger(WeaverWorkspaceController.class).error("Workspace request failed",e);
  return ResponseEntity.status(500).header("Cache-Control","no-store").body(Map.of("message","서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요. (500)"));
 }
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"인증된 계정의 자료만 사용할 수 있습니다.":e.getReason()));}
 @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class) public ResponseEntity<Map<String,String>> duplicate(){return ResponseEntity.status(409).body(Map.of("message","이미 처리 중인 가져오기입니다. 같은 계정으로 다시 시도해주세요."));}
}
