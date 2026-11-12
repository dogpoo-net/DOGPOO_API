package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.security.SecureRandom;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;import net.dogpoo.api.core.weaver.model.*;
@Service public class WeaverWithdrawalService {
 private final WeaverWithdrawalMapper deletion;private final WeaverWorkspaceMapper workspaces;private final WeaverSharingMapper members;private final WeaverSharingService sharing;private final WeaverSessionMapper accounts;private final WeaverVerificationMail mail;
 public WeaverWithdrawalService(WeaverWithdrawalMapper deletion,WeaverWorkspaceMapper workspaces,WeaverSharingMapper members,WeaverSharingService sharing,WeaverSessionMapper accounts,WeaverVerificationMail mail){this.deletion=deletion;this.workspaces=workspaces;this.members=members;this.sharing=sharing;this.accounts=accounts;this.mail=mail;}
 public Map<String,Object> preview(String id){
  var owned=new ArrayList<Map<String,Object>>();
  for(var w:workspaces.owned(id)){var item=new HashMap<String,Object>(w);String key=(String)w.get("workspaceId");item.put("members",members.members(key));item.put("bytes",workspaces.itemBytes(key)+workspaces.mediaBytes(key));owned.add(item);}
  return Map.of("workspaces",owned,"retention","현재 별도 법정 보관 정보는 없습니다. 계정과 운영 DB 자료는 탈퇴 시 삭제합니다. 첨부파일 삭제는 즉시 시작하며 실패 시 자동 재시도합니다. 현재 DB 백업은 운영하지 않습니다.","sharedNotice","이전한 워크스페이스와 다른 사람 소유의 공동 작성물은 유지됩니다. 회원 정보와 작성자 계정 연결은 삭제됩니다.");
 }
 @Transactional public void request(String id){
  workspaces.lockAccountId(id);var account=accounts.account(id);if(account==null)throw denied();
  if(deletion.recent(id)>0)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"1분 후 다시 요청해주세요.");
  String code=String.format(Locale.ROOT,"%08d",new SecureRandom().nextInt(100000000));
  deletion.challenge(id,WeaverAuthService.hash("withdraw:"+id+":"+code));mail.withdrawal(account.email(),code);
 }
 // A separate transaction retains failed-attempt counters without partially committing deletion.
 @Transactional public boolean verify(String id,String code){
  if(deletion.attempt(id)!=1)return false;
  return code!=null && code.matches("[0-9]{8}") && deletion.verify(id,WeaverAuthService.hash("withdraw:"+id+":"+code))==1;
 }
 @Transactional public void erase(String id,WeaverWithdrawalRequest request){
  workspaces.lockAccountId(id);var account=accounts.account(id);if(account==null)throw denied();
  if(!request.confirmed || request.code==null || deletion.verify(id,WeaverAuthService.hash("withdraw:"+id+":"+request.code))!=1)throw denied();
  var owned=workspaces.owned(id);var ids=new HashSet<String>();for(var w:owned)ids.add((String)w.get("workspaceId"));
  var transfers=request.transfers==null?Map.<String,String>of():request.transfers;
  var deletes=new HashSet<>(request.deleteWorkspaces==null?List.<String>of():request.deleteWorkspaces);
  var supplied=new HashSet<>(transfers.keySet());supplied.addAll(deletes);
  if(!ids.equals(supplied) || !Collections.disjoint(transfers.keySet(),deletes))throw new ResponseStatusException(HttpStatus.CONFLICT,"소유 워크스페이스 목록이 변경되었습니다. 삭제 또는 이전할 대상을 다시 확인해주세요.");
  for(String workspace:ids.stream().sorted().toList()){workspaces.lock(workspace);if(!id.equals(workspaces.owner(workspace)))throw new ResponseStatusException(HttpStatus.CONFLICT,"워크스페이스 소유자가 변경되었습니다. 다시 확인해주세요.");}
  for(var transfer:transfers.entrySet())sharing.transferForWithdrawal(id,transfer.getKey(),transfer.getValue());
  for(String workspace:deletes){deletion.enqueue(workspace,"workspace");workspaces.deleteWorkspace(workspace);}
  deletion.enqueue(id,"profile");deletion.deleteFlows(id,account.email());deletion.deleteReceived(account.email());deletion.deleteLegacy(account.email());
  // Cascades remove credentials, social links, membership, claims and challenges.
  // Authorship in other owners' workspaces becomes NULL via foreign keys.
  if(deletion.deleteAccount(id)!=1)throw denied();
 }
 private static ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"탈퇴 인증코드와 확인 항목을 다시 확인해주세요.");}
}
