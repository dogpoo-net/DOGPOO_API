package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.security.SecureRandom;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;import net.dogpoo.api.core.weaver.model.*;
@Service public class WeaverSharingService {
 private final WeaverWorkspaceMapper workspaces;private final WeaverSharingMapper members;private final WeaverSessionMapper accounts;
 public WeaverSharingService(WeaverWorkspaceMapper workspaces,WeaverSharingMapper members,WeaverSessionMapper accounts){this.workspaces=workspaces;this.members=members;this.accounts=accounts;}
 public record Access(String id,String email,String role){}
 public Access access(String user,String workspace,boolean write){
  String target=workspace==null || workspace.isBlank()?user:workspace;
  String role=target.equals(user)?"owner":members.role(target,user);
  if(role==null || (write && role.equals("viewer")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"이 워크스페이스를 편집할 권한이 없습니다.");
  var owner=accounts.account(target);if(owner==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);return new Access(target,owner.email(),role);
 }
 private void lockOwner(String user,String workspace){if(!user.equals(workspace))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"소유자만 참여자를 관리할 수 있습니다.");workspaces.ensure(workspace);workspaces.lock(workspace);}
 private String role(String role){if(!List.of("editor","viewer").contains(role==null?"":role))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"편집자 또는 읽기 전용 역할을 선택해주세요.");return role;}
 private void available(String workspace){if(1+members.editors(workspace)+members.reserved(workspace)>=2)throw new ResponseStatusException(HttpStatus.CONFLICT,"무료 워크스페이스는 소유자 포함 편집자 2명까지입니다. 대기 중인 편집자 초대도 자리를 예약합니다.");}
 public Map<String,Object> catalog(String user,String email){
  var list=new ArrayList<Map<String,Object>>();list.add(Map.of("workspaceId",user,"role","owner","name","내 워크스페이스"));list.addAll(members.memberships(user));return Map.of("workspaces",list,"invitations",members.received(email));
 }
 public Map<String,Object> list(String user,String workspace){var target=access(user,workspace,false);var owner=accounts.account(target.id());var list=new ArrayList<Map<String,Object>>();var item=new HashMap<String,Object>();item.put("userId",target.id());item.put("role","owner");item.put("name",owner.displayName()==null?"소유자":owner.displayName());item.put("email",owner.email());list.add(item);list.addAll(members.members(target.id()));return Map.of("members",list,"invitations",target.role().equals("owner")?members.invitations(target.id()):List.of(),"editorLimit",2);}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> invite(String user,String workspace,String email,String role){
  lockOwner(user,workspace);role=role(role);email=WeaverAuthService.email(email);if(accounts.account(user).email().equals(email))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"자신은 이미 소유자입니다.");
  for(var m:members.members(workspace))if(email.equals(m.get("email")))throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 참여한 사용자입니다.");
  for(var pending:members.invitations(workspace))if(email.equals(pending.get("email")))throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 대기 중인 초대가 있습니다. 취소 후 다시 초대해주세요.");
  if(role.equals("editor"))available(workspace);
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),id=UUID.randomUUID().toString();
  members.create(id,workspace,email,role,WeaverAuthService.hash(token));return Map.of("inviteId",id,"token",token,"email",email,"role",role);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> accept(String user,String email,String token,String inviteId){
  String id=token==null?inviteId:members.token(WeaverAuthService.hash(token));if(id==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"초대를 찾을 수 없습니다.");
  var invite=members.invite(id);if(invite==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  String workspace=(String)invite.get("workspaceId");workspaces.ensure(workspace);workspaces.lock(workspace);invite=members.invite(id);
  if(!email.equals(invite.get("email")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"초대받은 이메일 계정으로 로그인해주세요.");
  if("accepted".equals(invite.get("status")) && members.role(workspace,user)!=null)return Map.of("workspaceId",workspace);
  if(!"pending".equals(invite.get("status")) || !(Boolean.TRUE.equals(invite.get("valid")) || (invite.get("valid") instanceof Number n && n.intValue()==1)))throw new ResponseStatusException(HttpStatus.GONE,"만료되었거나 취소된 초대입니다.");
  if("editor".equals(invite.get("role")) && 1+members.editors(workspace)>=2)throw new ResponseStatusException(HttpStatus.CONFLICT,"편집자 자리가 가득 찼습니다.");
  if(members.role(workspace,user)==null)members.join(workspace,user,(String)invite.get("role"));members.status(id,"accepted");return Map.of("workspaceId",workspace);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void revoke(String user,String workspace,String inviteId){lockOwner(user,workspace);var invite=members.invite(inviteId);if(invite==null || !workspace.equals(invite.get("workspaceId")))throw new ResponseStatusException(HttpStatus.NOT_FOUND);members.status(inviteId,"revoked");}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void remove(String user,String workspace,String member){lockOwner(user,workspace);if(user.equals(member))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"소유자는 제거할 수 없습니다.");members.remove(workspace,member);}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void setRole(String user,String workspace,String member,String role){lockOwner(user,workspace);role=role(role);String old=members.role(workspace,member);if(old==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);if(role.equals("editor") && !role.equals(old))available(workspace);members.setRole(workspace,member,role);}
}
