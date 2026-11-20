package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.security.SecureRandom;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;import net.dogpoo.api.core.weaver.model.*;
@Service public class WeaverSharingService {
 @org.springframework.beans.factory.annotation.Autowired private WeaverWorkspacePolicyService policy;
 private final WeaverWorkspaceMapper workspaces;private final WeaverSharingMapper members;private final WeaverSessionMapper accounts;
 public WeaverSharingService(WeaverWorkspaceMapper workspaces,WeaverSharingMapper members,WeaverSessionMapper accounts){this.workspaces=workspaces;this.members=members;this.accounts=accounts;}
 public record Access(String id,String email,String role){}
 @Transactional public Access access(String user,String workspace,boolean write){return access(user,workspace,write,false);}
 private Access access(String user,String workspace,boolean write,boolean metadata){
  String target=workspace==null || workspace.isBlank()?primary(user):workspace;
  if(target==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"이용 가능한 워크스페이스가 없습니다.");
  if(workspaces.archived(target))throw new ResponseStatusException(HttpStatus.GONE,"삭제된 워크스페이스입니다.");
  String role=user.equals(workspaces.owner(target))?"owner":members.role(target,user);
  if(!metadata && policy!=null && !user.equals(workspaces.owner(target)) && !policy.online(target))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"소유자의 Free 공간은 공유가 중단된 상태입니다.");
  if("editor".equals(role) && policy!=null && !policy.editorAllowed(target,user))role="viewer";
  if(role==null || (write && role.equals("viewer")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"이 워크스페이스를 편집할 권한이 없습니다.");
  var owner=accounts.account(workspaces.owner(target));if(owner==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);return new Access(target,owner.email(),role);
 }
 private void lockOwner(String user,String workspace){workspaces.ensure(workspace);workspaces.lock(workspace);if(workspaces.archived(workspace))throw new ResponseStatusException(HttpStatus.GONE);if(!user.equals(workspaces.owner(workspace)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"소유자만 참여자를 관리할 수 있습니다.");}
 private String role(String role){if(!List.of("editor","viewer").contains(role==null?"":role))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"편집자 또는 읽기 전용 역할을 선택해주세요.");return role;}
 private void available(String workspace){if(policy!=null){policy.editorSeat(workspace,1);return;}if(1+members.editors(workspace)+members.reserved(workspace)>=2)throw new ResponseStatusException(HttpStatus.CONFLICT,"무료 워크스페이스는 소유자 포함 편집자 2명까지입니다. 대기 중인 편집자 초대도 자리를 예약합니다.");}
 @Transactional public String primary(String user){
  var owned=workspaces.owned(user);if(!owned.isEmpty())return (String)owned.getFirst().get("workspaceId");
  var joined=members.memberships(user);return joined.isEmpty()?null:(String)joined.getFirst().get("workspaceId");
 }
 @Transactional public Map<String,Object> catalog(String user,String email){
  var list=new ArrayList<Map<String,Object>>(workspaces.owned(user));list.addAll(members.memberships(user));
  var result=new HashMap<String,Object>();
  if(policy!=null)list.replaceAll(row->{var decorated=policy.decorate(row);if("editor".equals(decorated.get("role"))&&!policy.editorAllowed((String)row.get("workspaceId"),user))decorated.put("role","viewer");return decorated;});
  list.removeIf(row->"owner".equals(row.get("role")) && "free".equals(row.get("plan")) && Boolean.TRUE.equals(row.get("quotaReadOnly")));
  result.put("workspaces",list);result.put("defaultWorkspaceId",list.stream().filter(row->!Boolean.TRUE.equals(row.get("sharingSuspended"))).map(row->row.get("workspaceId")).findFirst().orElse(null));result.put("invitations",members.received(email));result.put("isAdmin",policy!=null&&policy.admin(user));if(policy!=null)result.put("accountPolicy",policy.accountStatus(user));return result;
 }
 private String workspaceName(String value){
  String name=value==null?"":value.strip();
  if(name.isEmpty() || name.codePointCount(0,name.length())>50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"워크스페이스 이름은 1~50자로 입력해주세요.");
  return name;
 }
 @Transactional public Map<String,Object> createWorkspace(String user,String name){
  name=workspaceName(name);workspaces.lockAccountId(user);if(policy!=null)policy.creation(user);
  String id=UUID.randomUUID().toString();workspaces.create(id,user);workspaces.initializeName(id,name);if(policy!=null)policy.created(user);
  return Map.of("workspaceId",id);
 }
 private void existingOwner(String user,String workspace){
  if(workspace==null || workspaces.revision(workspace)==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"워크스페이스를 찾을 수 없습니다.");
  workspaces.lock(workspace);
  if(workspaces.archived(workspace))throw new ResponseStatusException(HttpStatus.GONE);
  if(!user.equals(workspaces.owner(workspace)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"소유자만 관리할 수 있습니다.");
 }
 @Transactional public void renameWorkspace(String user,String workspace,String name){
  name=workspaceName(name);existingOwner(user,workspace);if(policy!=null&&!policy.paidAccount(user))throw WeaverWorkspacePolicyService.deny("무료 플랜의 기본 공간은 변경할 수 없습니다.");workspaces.rename(workspace,name);
 }
 @Transactional public void leaveWorkspace(String user,String workspace){
  if(workspace==null || workspaces.revision(workspace)==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  workspaces.lock(workspace);
  if(user.equals(workspaces.owner(workspace)))throw new ResponseStatusException(HttpStatus.CONFLICT,"소유자는 소유권을 이전하거나 워크스페이스를 삭제해주세요.");
  if(members.role(workspace,user)==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  // Remove membership only: account, authorship metadata, comments and history remain.
  members.remove(workspace,user);
 }
 @Transactional public void transfer(String user,String workspace,String recipient){transferInternal(user,workspace,recipient,true);}
 @Transactional public void transferForWithdrawal(String user,String workspace,String recipient){transferInternal(user,workspace,recipient,false);}
 public boolean canOfferTransfer(String owner,String workspace,String recipient){return !workspaces.archived(workspace)&&policy.paidAccount(owner)&&policy.activeCount(owner)>=2&&eligibleRecipient(workspace,recipient);}
 public boolean eligibleRecipient(String workspace,String recipient){
  return recipient!=null && "editor".equals(members.role(workspace,recipient)) && policy.editorAllowed(workspace,recipient) && policy.paidAccount(recipient) && policy.hasSlot(recipient);
 }
 public void validateTransfer(String user,String workspace,String recipient){
  policy.requireSpare(user);
  if(workspaces.archived(workspace)||user.equals(recipient)||!eligibleRecipient(workspace,recipient))throw WeaverWorkspacePolicyService.deny("소유 공간 한도에 여유가 있는 유료 플랜 편집자에게만 이전할 수 있습니다.");
 }
 private void transferInternal(String user,String workspace,String recipient,boolean keepFormerOwner){
  workspaces.lockAccountId(user);workspaces.lockAccountId(recipient);validateTransfer(user,workspace,recipient);
  if(keepFormerOwner){lockOwner(user,workspace);}else{
   workspaces.lock(workspace);if(!user.equals(workspaces.owner(workspace)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }
  members.remove(workspace,recipient);if(keepFormerOwner)members.join(workspace,user,"editor");workspaces.transfer(workspace,recipient);
 }
 public Map<String,Object> list(String user,String workspace){var target=access(user,workspace,false,true);var owner=accounts.account(workspaces.owner(target.id()));var list=new ArrayList<Map<String,Object>>();var item=new HashMap<String,Object>();item.put("userId",owner.id());item.put("role","owner");item.put("name",owner.displayName()==null?"소유자":owner.displayName());item.put("email",owner.email());list.add(item);list.addAll(members.members(target.id()));if(policy!=null)for(var row:list){row.put("configuredRole",row.get("role"));if("editor".equals(row.get("role"))&&!policy.editorAllowed(target.id(),(String)row.get("userId")))row.put("role","viewer");}
 if(target.role().equals("owner") && policy!=null && policy.paidAccount(user) && policy.activeCount(user)>=2)for(var row:list)if("editor".equals(row.get("role")))row.put("canReceiveOwnership",eligibleRecipient(workspace,(String)row.get("userId")));
 var result=new HashMap<String,Object>();result.put("members",list);result.put("invitations",target.role().equals("owner")?members.invitations(target.id()):List.of());result.put("editorLimit",policy==null?Long.valueOf(2):policy.editorLimit(target.id()));return result;}
 public List<String> emailSuggestions(String user,String workspace,String query){
  var target=access(user,workspace,false);
  if(!"owner".equals(target.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  if(policy!=null)policy.requireOnline(target.id());
  String prefix=query==null?"":query.strip().toLowerCase(Locale.ROOT);
  if(prefix.length()<2 || prefix.length()>254)return List.of();
  return members.emailSuggestions(prefix.replace("!","!!").replace("%","!%").replace("_","!_"),target.id(),user);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> invite(String user,String workspace,String email,String role){
  lockOwner(user,workspace);if(policy!=null)policy.requireOnline(workspace);role=role(role);email=WeaverAuthService.email(email);if(accounts.account(workspaces.owner(workspace)).email().equals(email))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"자신은 이미 소유자입니다.");
  if(!members.registeredEmail(email))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"가입된 계정의 이메일을 입력해주세요.");
  for(var m:members.members(workspace))if(email.equals(m.get("email")))throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 참여한 사용자입니다.");
  for(var pending:members.invitations(workspace))if(email.equals(pending.get("email")))throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 대기 중인 초대가 있습니다. 취소 후 다시 초대해주세요.");
  if(role.equals("editor"))available(workspace);
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),id=UUID.randomUUID().toString();
  members.create(id,workspace,email,role,WeaverAuthService.hash(token));return Map.of("inviteId",id,"token",token,"email",email,"role",role);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> accept(String user,String email,String token,String inviteId){
  String id=token==null?inviteId:members.token(WeaverAuthService.hash(token));if(id==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"초대를 찾을 수 없습니다.");
  var invite=members.invite(id);if(invite==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  String workspace=(String)invite.get("workspaceId");if(policy!=null)policy.requireOnline(workspace);workspaces.ensure(workspace);workspaces.lock(workspace);if(workspaces.archived(workspace))throw new ResponseStatusException(HttpStatus.GONE);invite=members.invite(id);
  if(!email.equals(invite.get("email")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"초대받은 이메일 계정으로 로그인해주세요.");
  if("accepted".equals(invite.get("status")) && members.role(workspace,user)!=null)return Map.of("workspaceId",workspace);
  if(!"pending".equals(invite.get("status")) || !(Boolean.TRUE.equals(invite.get("valid")) || (invite.get("valid") instanceof Number n && n.intValue()==1)))throw new ResponseStatusException(HttpStatus.GONE,"만료되었거나 취소된 초대입니다.");
  if("editor".equals(invite.get("role")) && (policy==null?1+members.editors(workspace)>=2:policy.editorLimit(workspace)!=null && 1+members.editors(workspace)>=policy.editorLimit(workspace)))throw new ResponseStatusException(HttpStatus.CONFLICT,"편집자 자리가 가득 찼습니다.");
  if(members.role(workspace,user)==null)members.join(workspace,user,(String)invite.get("role"));members.status(id,"accepted");return Map.of("workspaceId",workspace);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void revoke(String user,String workspace,String inviteId){lockOwner(user,workspace);var invite=members.invite(inviteId);if(invite==null || !workspace.equals(invite.get("workspaceId")))throw new ResponseStatusException(HttpStatus.NOT_FOUND);members.status(inviteId,"revoked");}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void remove(String user,String workspace,String member){lockOwner(user,workspace);if(user.equals(member))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"소유자는 제거할 수 없습니다.");members.remove(workspace,member);}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void setRole(String user,String workspace,String member,String role){lockOwner(user,workspace);role=role(role);String old=members.role(workspace,member);if(old==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);if(role.equals("editor") && !role.equals(old))available(workspace);members.setRole(workspace,member,role);}
}
