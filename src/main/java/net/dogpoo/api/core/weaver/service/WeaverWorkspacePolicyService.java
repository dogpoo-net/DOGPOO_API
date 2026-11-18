package net.dogpoo.api.core.weaver.service;
import java.util.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.WeaverWorkspaceRequest;
@Service public class WeaverWorkspacePolicyService {
 private final WeaverPolicyMapper store;private final WeaverWorkspaceMapper spaces;private final WeaverSharingMapper members;
 public WeaverWorkspacePolicyService(WeaverPolicyMapper store,WeaverWorkspaceMapper spaces,WeaverSharingMapper members){this.store=store;this.spaces=spaces;this.members=members;}
 static ResponseStatusException deny(String text){return new ResponseStatusException(HttpStatus.valueOf(422),text);}
 public static Long limit(Map<String,Object> plan,String key){Object n=plan.get(key);return n instanceof Number number?number.longValue():null;}
 public Map<String,Object> plan(String id){var p=store.plan(id);if(p==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"워크스페이스가 없습니다.");return p;}
 @Transactional public void initializeAccount(String user){spaces.lockAccountId(user);if(store.provisioned(user)==0){spaces.ensure(user);store.provision(user);store.created(user);}}
 public boolean paid(String id){return !"free".equals(plan(id).get("code"));}
 public Long editorLimit(String id){return limit(plan(id),"editor_limit");}
 public boolean editorAllowed(String id,String user){Long n=editorLimit(id);return n==null || store.editors(id).indexOf(user)>=0 && store.editors(id).indexOf(user)<n-1;}
 public void editorSeat(String id,int additional){Long n=editorLimit(id);if(n!=null && 1+members.editors(id)+members.reserved(id)+additional>n)throw deny("워크스페이스의 편집자 한도를 초과합니다. 참여자 또는 대기 중인 초대를 정리해주세요.");}
 public Map<String,Object> accountPlan(String user){var p=store.accountPlan(user);if(p==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"계정이 없습니다.");return p;}
 public Long ownedLimit(String user){return limit(accountPlan(user),"owned_limit");}
 public boolean online(String id){return paid(id);}
 public void requireOnline(String id){if(!online(id))throw deny("Free 소유 공간은 오프라인 모드입니다. 서버 동기화와 공유는 Standard부터 사용할 수 있습니다.");}
 public void creation(String user){admission(user);if("free".equals(accountPlan(user).get("code"))&&store.coolingDown(user)>0)throw deny("마지막 워크스페이스 생성 후 24시간이 지나야 다시 만들 수 있습니다. 다음 생성 가능 시각: "+store.nextCreation(user));}
 public void created(String user){store.created(user);}
 public Map<String,Object> accountStatus(String user){var result=new HashMap<String,Object>();result.put("plan",accountPlan(user));result.put("subscription",Optional.ofNullable(store.subscription(user)).orElse(Map.of()));result.put("nextCreationAt",store.nextCreation(user));result.put("billingConnected",false);return result;}
 public void admission(String user){spaces.lockAccountId(user);Long n=ownedLimit(user);long count=store.owned(user).stream().filter(r->r.get("archivedAt")==null).count();if(n!=null && count>=n)throw deny("소유한 활성 워크스페이스 한도를 초과합니다. 기존 공간을 정리하거나 플랜을 변경해주세요.");}
 public boolean selectedForWrite(String id){String owner=spaces.owner(id);Long n=ownedLimit(owner);if(n==null)return true;var active=store.owned(owner).stream().filter(r->r.get("archivedAt")==null).toList();for(int i=0;i<active.size();i++)if(id.equals(active.get(i).get("workspaceId")))return i<n;return false;}
 public void requireWrite(String id){requireOnline(id);if(!selectedForWrite(id))throw deny("소유 한도를 초과하여 조회 전용입니다. 사용할 워크스페이스를 선택해주세요.");}
 public void validateSave(String id,List<WeaverWorkspaceRequest.Change> changes,long beforeBytes,long beforeDocuments){
  requireOnline(id);
  boolean onlyDelete=changes.stream().allMatch(c->c.deleted);
  if(!selectedForWrite(id)&&!onlyDelete)throw deny("소유 한도를 초과한 공간은 조회·삭제만 가능합니다.");
  var p=plan(id);Long capacity=limit(p,"storage_limit"),documents=limit(p,"document_limit");
  if(capacity!=null && store.bytes(id)>capacity && store.bytes(id)>beforeBytes)throw deny("저장 용량을 초과했습니다. 용량을 늘리는 저장은 불가능합니다.");
  if(documents!=null && store.documents(id)>documents && store.documents(id)>beforeDocuments)throw deny("문서 수를 초과했습니다. 기존 문서는 수정할 수 있지만 새 문서는 추가할 수 없습니다.");
 }
 public void validateMedia(String id,long additional){requireWrite(id);Long capacity=limit(plan(id),"storage_limit");if(capacity!=null && store.bytes(id)+additional>capacity)throw deny("저장 용량을 초과하여 첨부파일을 추가할 수 없습니다.");}
 public long bytes(String id){return store.bytes(id);}public long documents(String id){return store.documents(id);}
 public void validateRestore(String user,String id){if(!paid(id))throw deny("복원은 유료 플랜에서 가능합니다. 남은 보관 기간 안에 구독을 활성화해주세요.");admission(user);var p=plan(id);Long bytes=limit(p,"storage_limit"),docs=limit(p,"document_limit");if(bytes!=null&&store.bytes(id)>bytes || docs!=null&&store.documents(id)>docs)throw deny("복원할 공간의 자료가 현재 플랜 한도를 초과합니다.");}
 public String sharingSuspensionReason(String id){
  if(online(id))return null;
  var subscription=store.subscription(spaces.owner(id));
  if(subscription==null || subscription.isEmpty())return "free";
  Object cancelled=subscription.get("cancelAtEnd");
  return Boolean.TRUE.equals(cancelled) || cancelled instanceof Number n && n.intValue()!=0 ? "cancelled" : "expired";
 }
 public Map<String,Object> decorate(Map<String,Object> original){var row=new HashMap<>(original);String id=(String)row.get("workspaceId");var p=plan(id);row.put("plan",p.get("code"));row.put("limits",p);row.put("offline",!online(id));if(!"owner".equals(row.get("role"))){String reason=sharingSuspensionReason(id);row.put("sharingSuspended",reason!=null);if(reason!=null)row.put("subscriptionEndReason",reason);}row.put("quotaReadOnly",!selectedForWrite(id));return row;}
 public Map<String,Object> status(String id){var result=new HashMap<String,Object>();result.put("plan",plan(id));result.put("subscription",Optional.ofNullable(store.subscription(spaces.owner(id))).orElse(Map.of()));result.put("bytes",store.bytes(id));result.put("documents",store.documents(id));result.put("quotaReadOnly",!selectedForWrite(id));result.put("billingConnected",false);return result;}
 private void owner(String user,String id){if(id==null||!user.equals(spaces.owner(id)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);spaces.lock(id);}
 @Transactional public Map<String,Object> cancel(String user,String id,boolean cancel){owner(user,id);if(store.cancel(user,cancel)!=1)throw deny("유효한 유료 구독이 없습니다.");store.audit(user,cancel?"cancel_at_end":"resume_subscription",user,"{}");return status(id);}
 @Transactional public void chooseSpaces(String user,List<String> ids){spaces.lockAccountId(user);if(ids==null||new HashSet<>(ids).size()!=ids.size())throw deny("워크스페이스 선택을 확인해주세요.");Long n=ownedLimit(user);if(n!=null&&ids.size()>n)throw deny("선택한 공간이 소유 한도를 초과합니다.");var all=store.owned(user).stream().filter(r->r.get("archivedAt")==null).toList();var valid=all.stream().map(r->(String)r.get("workspaceId")).toList();if(!valid.containsAll(ids))throw new ResponseStatusException(HttpStatus.FORBIDDEN);for(var id:valid)store.priority(id,user,0);for(int i=0;i<ids.size();i++)store.priority(ids.get(i),user,ids.size()-i);store.audit(user,"select_active_spaces",user,ids.toString());}
 @Transactional public void chooseEditors(String user,String id,List<String> ids){owner(user,id);if(ids==null||new HashSet<>(ids).size()!=ids.size()||ids.contains(user))throw deny("편집자 선택을 확인해주세요.");Long n=editorLimit(id);if(n!=null&&ids.size()+1>n)throw deny("편집자 한도를 초과합니다.");var current=members.members(id);var users=current.stream().map(r->(String)r.get("userId")).toList();if(!users.containsAll(ids))throw deny("현재 참여자만 선택할 수 있습니다.");for(var member:users)members.setRole(id,member,ids.contains(member)?"editor":"viewer");store.audit(user,"select_editors",id,ids.toString());}
 public void requireAdmin(String user){if(!"admin".equals(store.platformRole(user)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"관리자 권한이 필요합니다.");}
 public boolean admin(String user){return "admin".equals(store.platformRole(user));}
 public Map<String,Object> adminUsers(String user,String query){requireAdmin(user);String q=query==null?"":query.strip();if(q.length()>254)throw deny("검색어가 너무 깁니다.");return Map.of("users",store.users(q.replace("!","!!").replace("%","!%").replace("_","!_")),"plans",store.plans());}
 public Map<String,Object> adminAccount(String actor,String user){requireAdmin(actor);var account=store.user(user);if(account==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);var rows=new ArrayList<Map<String,Object>>();for(var owned:store.owned(user)){var row=new HashMap<>(owned);row.put("subscription",Optional.ofNullable(store.subscription(user)).orElse(Map.of()));rows.add(row);}return Map.of("account",account,"workspaces",rows,"accountPolicy",accountStatus(user));}
 @Transactional public void adminRole(String actor,String user,String role){requireAdmin(actor);store.lockAdmin();requireAdmin(actor);if(!Set.of("user","admin").contains(role==null?"":role))throw deny("지원하지 않는 권한입니다.");String before=store.platformRole(user);if(before==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);if("admin".equals(before)&&"user".equals(role)&&store.admins()<=1)throw deny("마지막 관리자 권한은 해제할 수 없습니다.");store.platformRoleSet(user,role);store.audit(actor,"account_role",user,before+" -> "+role);}
 @Transactional public Map<String,Object> adminSubscription(String actor,String id,String plan,String until,boolean cancel){requireAdmin(actor);if(id==null||store.user(id)==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);spaces.lockAccountId(id);if(!Set.of("free","standard","pro").contains(plan==null?"":plan))throw deny("플랜을 확인해주세요.");var before=store.subscription(id);
  if("free".equals(plan))store.free(id);else{Instant end;try{end=Instant.parse(until);}catch(Exception e){throw deny("종료일은 시간대가 포함된 날짜로 입력해주세요.");}if(!end.isAfter(Instant.now()))throw deny("종료일은 미래여야 합니다.");store.subscribe(id,plan,LocalDateTime.ofInstant(end,ZoneOffset.UTC),cancel);}
  store.audit(actor,"account_subscription",id,String.valueOf(before)+" -> "+String.valueOf(store.subscription(id)));return accountStatus(id);
 }
 @Transactional public void adminMember(String actor,String id,String user,String role){requireAdmin(actor);if(id==null||spaces.revision(id)==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);spaces.lock(id);if(user==null)throw deny("참여자 계정을 선택해주세요.");if(user.equals(spaces.owner(id)))throw deny("소유자는 멤버 역할 변경 대신 소유권 이전을 사용해주세요.");if(!Set.of("editor","viewer").contains(role==null?"":role)||members.role(id,user)==null)throw deny("현재 참여자의 역할을 확인해주세요.");if("editor".equals(role)&&!"editor".equals(members.role(id,user)))editorSeat(id,1);String before=members.role(id,user);members.setRole(id,user,role);store.audit(actor,"member_role",id,user+":"+before+" -> "+role);}
 public List<Map<String,Object>> adminMembers(String actor,String id){requireAdmin(actor);return members.members(id);}
 public List<Map<String,Object>> audit(String actor){requireAdmin(actor);return store.auditList();}
}
