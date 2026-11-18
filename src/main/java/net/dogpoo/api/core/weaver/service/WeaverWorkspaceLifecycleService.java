package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.security.SecureRandom;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
@Service public class WeaverWorkspaceLifecycleService {
 @org.springframework.beans.factory.annotation.Autowired private WeaverWorkspacePolicyService policy;
 private final WeaverWorkspaceMapper spaces;private final WeaverWorkspaceLifecycleMapper lifecycle;private final WeaverSessionMapper accounts;private final WeaverWithdrawalMapper deletion;private final WeaverVerificationMail mail;
 public WeaverWorkspaceLifecycleService(WeaverWorkspaceMapper spaces,WeaverWorkspaceLifecycleMapper lifecycle,WeaverSessionMapper accounts,WeaverWithdrawalMapper deletion,WeaverVerificationMail mail){this.spaces=spaces;this.lifecycle=lifecycle;this.accounts=accounts;this.deletion=deletion;this.mail=mail;}
 private void owner(String user,String id,boolean archived){
  if(id==null || spaces.revision(id)==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  spaces.lock(id);
  if(!user.equals(spaces.owner(id)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"소유자만 삭제·복원할 수 있습니다.");
  if(!archived && spaces.archived(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 삭제된 워크스페이스입니다.");
 }
 private String hash(String user,String id,String code){var account=accounts.account(user);if(account==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return WeaverAuthService.hash("workspace-delete:"+id+":"+user+":"+account.email()+":"+code);}
 @Transactional public Map<String,Object> preview(String user,String id){owner(user,id,false);boolean used=lifecycle.used(id);return Map.of("requiresEmail",used,"retentionDays",used?30:0);}
 @Transactional public void request(String user,String id){
  owner(user,id,false);if(!lifecycle.used(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"작업 이력이 없어 이메일 인증 없이 삭제할 수 있습니다.");
  if(lifecycle.recent(id)>0)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"1분 후 다시 요청해주세요.");
  String code=String.format(Locale.ROOT,"%08d",new SecureRandom().nextInt(100000000));
  lifecycle.issue(id,user,hash(user,id,code));mail.workspaceDeletion(accounts.account(user).email(),code);
 }
 // Return failure instead of throwing, so failed-attempt counters commit.
 @Transactional public boolean verify(String user,String id,String code){owner(user,id,false);if(lifecycle.attempt(id,user)!=1)return false;return code!=null && code.matches("[0-9]{8}") && lifecycle.valid(id,user,hash(user,id,code))==1;}
 @Transactional public Map<String,Object> delete(String user,String id,String code){
  spaces.lockAccountId(user);owner(user,id,false);
  if(!lifecycle.used(id)){
   lifecycle.clear(id);erase(id);return Map.of("archived",false,"deleted",true,"retentionDays",0);
  }
  {
   if(code==null || !code.matches("[0-9]{8}") || lifecycle.valid(id,user,hash(user,id,code))!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"소유자의 이메일 인증이 필요합니다. 삭제를 다시 요청해주세요.");
  }
  lifecycle.archive(id);lifecycle.clear(id);return Map.of("archived",true,"retentionDays",30);
 }
 public Map<String,Object> archived(String user){var rows=lifecycle.archived(user);for(var row:rows)row.put("canRestore",policy==null||policy.paid((String)row.get("workspaceId")));return Map.of("workspaces",rows);}
 @Transactional public void restore(String user,String id){spaces.lockAccountId(user);owner(user,id,true);if(policy!=null)policy.validateRestore(user,id);if(lifecycle.restore(id)!=1)throw new ResponseStatusException(HttpStatus.GONE,"복원 기간이 지났거나 복원할 수 없는 워크스페이스입니다.");lifecycle.clear(id);}
 @Transactional public void purge(String id){if(spaces.revision(id)==null)return;spaces.lock(id);if(lifecycle.expiredOne(id)==1)erase(id);}
 private void erase(String id){spaces.tombstone(id);deletion.enqueue(id,"workspace");spaces.deleteWorkspace(id);}
}
