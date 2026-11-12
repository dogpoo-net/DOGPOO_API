package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.nio.file.*;import java.security.*;import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;import net.dogpoo.api.core.weaver.model.*;
@Service public class WeaverWorkspaceService {
 @org.springframework.beans.factory.annotation.Autowired private WeaverHistoryService history;
 @org.springframework.beans.factory.annotation.Autowired private WeaverSharingService permissions;
 @org.springframework.beans.factory.annotation.Autowired private WeaverDocumentLockMapper documentLocks;
 private final WeaverWorkspaceMapper mapper;private final WeaverTestDataMapper legacy;private final ObjectMapper json=new ObjectMapper();private final long maxBytes;private final Path resourceRoot;
 public WeaverWorkspaceService(WeaverWorkspaceMapper mapper,WeaverTestDataMapper legacy,@Value("${WEAVER_WORKSPACE_MAX_BYTES:67108864}") long maxBytes,@Value("${WEAVER_RESOURCE_DIR:/home/ocaptain/resource}") String resourceRoot){this.mapper=mapper;this.legacy=legacy;this.maxBytes=maxBytes;this.resourceRoot=Path.of(resourceRoot).toAbsolutePath().normalize();}
 private String key(String kind,String id){return WeaverAuthService.hash(kind+"\n"+id);}
 private void valid(String kind,String id){if(kind==null || id==null || kind.length()>64 || id.length()>512 || id.isEmpty() || !kind.matches("(list|map):[A-Za-z][A-Za-z0-9]*|scalar|layout"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"잘못된 항목 식별자입니다.");}
 private void quota(String id){if(mapper.itemBytes(id)+mapper.mediaBytes(id)+history.bytes(id)>maxBytes)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"워크스페이스 저장 한도를 초과했습니다.");}
 private long lock(String id,String email){
  mapper.ensure(id);long revision=mapper.lock(id);
  if(!mapper.migrated(id)){
   var args=new HashMap<String,Object>();args.put("account",email);String raw=legacy.getWeaverTestData(args);
   if(raw!=null && !raw.isBlank()){
    try {JsonNode root=json.readTree(raw);if(root.isTextual())root=json.readTree(root.asText());JsonNode workspace=root.has("workspace")?root.get("workspace"):root;
     if(workspace.path("version").asInt()!=1 || !workspace.path("elements").isArray())throw new IllegalArgumentException();
     mapper.advance(id);revision++;
     for(var f:workspace.properties()){
      String name=f.getKey();JsonNode node=f.getValue();
      if(name.equals("graphLayout") && node.isObject()){legacyLayout(id,"page",node,new ArrayList<>(),revision);continue;}
      if(node.isArray()){int index=0;for(JsonNode value:node){String itemId=value.isObject()?value.path("id").asText(value.path("createdAt").asText(value.path("name").asText("legacy-"+index))):value.asText();if(name.equals("notes") && !value.hasNonNull("id"))itemId="legacy-note-"+value.path("createdAt").asText()+"-"+index;
       if(name.equals("memoBoxes") && value.isObject()){var copy=((com.fasterxml.jackson.databind.node.ObjectNode)value).deepCopy();var layout=copy.remove("layout");if(layout!=null && layout.isObject())legacyLayout(id,"box:"+itemId,layout,new ArrayList<>(),revision);putLegacy(id,"list:"+name,itemId,copy,revision);}else putLegacy(id,"list:"+name,itemId,value,revision);index++;}}
      else if(node.isObject()){for(var entry:node.properties())putLegacy(id,"map:"+name,entry.getKey(),entry.getValue(),revision);}
      else putLegacy(id,"scalar",name,node,revision);
     }
     if(root.path("media").isObject())for(var file:root.path("media").properties())storeFile(id,file.getKey(),legacyType(workspace,file.getKey()),Base64.getDecoder().decode(file.getValue().asText()));
    }catch(Exception e){throw new ResponseStatusException(HttpStatus.CONFLICT,"기존 서버 자료를 확인해야 합니다. 자료는 보존되었습니다.");}
   }
   mapper.migratedDone(id);
  }
  return revision;
 }
 private void legacyLayout(String user,String scope,JsonNode value,List<String> path,long revision) throws Exception {
  for(var entry:value.properties()){var next=new ArrayList<>(path);next.add(entry.getKey());if(entry.getValue().isObject())legacyLayout(user,scope,entry.getValue(),next,revision);else{var id=new ArrayList<String>();id.add(scope);id.addAll(next);putLegacy(user,"layout",json.writeValueAsString(id),entry.getValue(),revision);}}
 }
 private void putLegacy(String user,String kind,String id,JsonNode value,long revision){valid(kind,id);mapper.put(user,key(kind,id),kind,id,value.toString(),false,revision);history.record(user,user,kind,id,null,value.toString(),"migration");}
 private Map<String,Object> result(String id,long since,long revision){return Map.of("revision",revision,"changes",mapper.changes(id,since));}
 @Transactional public Map<String,Object> read(String id,String email,long since){return read(id,email,id,since);}
 @Transactional public Map<String,Object> read(String id,String email,String actor,long since){long revision=lock(id,email);permissions.access(actor,id,false);if(since<0 || since>revision)throw new ResponseStatusException(HttpStatus.CONFLICT,"동기화 버전을 확인해주세요.");return result(id,since,revision);}
 @Transactional public Map<String,Object> save(String id,String email,WeaverWorkspaceRequest request,boolean importing){return save(id,email,id,request,importing);}
 @Transactional public Map<String,Object> save(String id,String email,String actor,WeaverWorkspaceRequest request,boolean importing){
  long revision=lock(id,email);permissions.access(actor,id,true);
  if(request.changes==null || request.changes.size()>20000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"항목 수가 너무 많습니다.");
  if(importing){
   if(request.guestId==null || !request.guestId.matches("[a-f0-9]{64}") || request.sourceHash==null || !request.sourceHash.matches("[a-f0-9]{64}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
   String owner=mapper.claimed(request.guestId);
   if(owner!=null){if(!owner.equals(id) || !request.sourceHash.equals(mapper.sourceHash(request.guestId)))throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 귀속되었거나 내용이 다른 비회원 자료입니다.");return result(id,0,revision);}
  }
  if(request.resourceId!=null){
   if(importing || request.resourceId.isBlank() || request.resourceId.length()>512)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
   checkEditLease(id,actor,request.resourceId,request);
   var supplied=new HashSet<String>();var expanded=new ArrayList<>(request.changes);
   for(var c:request.changes){var old=mapper.item(id,key(c.kind,c.id));if(!request.resourceId.equals(editResource(c.kind,c.id,c.value,old==null?null:old.payload())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"다른 편집 영역의 데이터입니다.");supplied.add(key(c.kind,c.id));}
   for(var old:mapper.changes(id,0)){
    if(old.deleted() || supplied.contains(key(old.kind(),old.id())))continue;
    if(request.resourceId.equals(editResource(old.kind(),old.id(),null,old.payload()))){var c=new WeaverWorkspaceRequest.Change();c.kind=old.kind();c.id=old.id();c.deleted=true;c.baseRevision=old.revision();expanded.add(c);}
   }
   request.changes=expanded;
  }
  var removedMedia=new HashSet<String>();
  var seen=new HashSet<String>();
  for(var change:request.changes){valid(change.kind,change.id);String key=key(change.kind,change.id);if(!seen.add(key))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"중복 항목입니다.");
   var old=mapper.item(id,key);
   if(change.deleted && old!=null && !old.deleted() && Set.of("list:elements","list:trash").contains(change.kind))removedMedia.addAll(attachmentIds(old.payload()));
   long current=old==null?0:old.revision();
   String payload=change.deleted?null:change.value==null?null:change.value.toString();
   if(!change.deleted && (payload==null || payload.length()>10000000))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"항목 내용이 올바르지 않습니다.");
   String resource=editResource(change.kind,change.id,change.value,old==null?null:old.payload());
   if(resource!=null)checkEditLease(id,actor,resource,request);
   boolean identical=old!=null && old.deleted()==change.deleted && Objects.equals(old.payload(),payload);
   if(!identical && request.resourceId==null && current!=change.baseRevision)throw new ResponseStatusException(HttpStatus.CONFLICT,"다른 기기에서 같은 항목을 수정했습니다. 로컬 수정은 보관됩니다.");
  }
  if(importing)mapper.claim(request.guestId,id,request.sourceHash);
  if(!request.changes.isEmpty()){mapper.advance(id);revision++;}
  for(var c:request.changes){var old=mapper.item(id,key(c.kind,c.id));String after=c.deleted?null:c.value.toString();history.record(id,actor,c.kind,c.id,old==null||old.deleted()?null:old.payload(),after,importing?"import":"edit");mapper.put(id,key(c.kind,c.id),c.kind,c.id,after,c.deleted,revision);if((c.kind.equals("list:notes")||c.kind.equals("list:elements")) && (old==null || !Objects.equals(old.payload(),after)))mapper.touch(id,key(c.kind,c.id),old==null?actor:null,actor);}
  removeUnreferencedMedia(id,removedMedia);
  quota(id);if(importing)return result(id,0,revision);
  return Map.of("revision",revision,"changes",request.changes.stream().map(c->mapper.item(id,key(c.kind,c.id))).toList());
 }
 private Set<String> attachmentIds(String payload){
  var ids=new HashSet<String>();if(payload==null)return ids;
  try{var record=json.readTree(payload);for(String field:List.of("photos","recordings"))for(var item:record.path(field)){String id=item.path("id").asText("");if(!id.isEmpty())ids.add(id);}}
  catch(Exception ignored){/* Invalid legacy data must not authorize file deletion. */}return ids;
 }
 private void removeUnreferencedMedia(String user,Set<String> candidates){
  if(candidates.isEmpty())return;
  for(String record:mapper.attachmentRecords(user))candidates.removeAll(attachmentIds(record));
  for(String id:candidates){String key=WeaverAuthService.hash(id);var media=mapper.media(user,key);if(media==null)continue;
   Path file=resourceRoot.resolve(media.path()).normalize();if(!file.startsWith(resourceRoot.resolve("contents/note").resolve(media.type()).resolve(user)))continue;
   mapper.removeMedia(user,key);
   org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCommit(){try{Files.deleteIfExists(file);}catch(java.io.IOException ignored){/* Metadata is revoked; a filesystem failure must not roll back a committed deletion. */}}});
  }
 }
 @Transactional public Map<String,Object> documentLease(String id,String email,String actor,WeaverWorkspaceRequest request){
  lock(id,email);permissions.access(actor,id,true);
  if(request.documentId==null || request.documentId.isBlank() || request.documentId.length()>512 || request.leaseToken==null || !request.leaseToken.matches("[a-f0-9]{64}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  String key=WeaverAuthService.hash(request.documentId),hash=WeaverAuthService.hash(request.leaseToken);
  if(request.release){documentLocks.release(id,key,actor,hash);return Map.of("released",true);}
  var existing=documentLocks.active(id,key);
  if(request.takeover){documentLocks.openEditing(id,key,actor,hash);return Map.of("acquired",true,"shared",true);}
  if(sharedEditing(existing))return Map.of("acquired",true,"shared",true);
  if(request.renewOnly && existing==null)return Map.of("acquired",false);
  if(!request.takeover && existing!=null && (!actor.equals(existing.get("userId")) || !hash.equals(existing.get("clientHash"))))return Map.of("acquired",false,"holder",Objects.toString(existing.get("name"),"다른 사용자"));
  documentLocks.renew(id,key,actor,hash);return Map.of("acquired",true,"expiresIn",120);
 }
 private boolean sharedEditing(Map<String,Object> lease){return lease!=null && (Boolean.TRUE.equals(lease.get("sharedEditing")) || lease.get("sharedEditing") instanceof Number n && n.intValue()!=0);}
 private void checkEditLease(String id,String actor,String resource,WeaverWorkspaceRequest request){
  var lease=documentLocks.active(id,WeaverAuthService.hash(resource));if(sharedEditing(lease))return;
  String token=request.documentLeases==null?null:request.documentLeases.get(resource);
  if(lease!=null && (!actor.equals(lease.get("userId")) || token==null || !WeaverAuthService.hash(token).equals(lease.get("clientHash"))))throw new ResponseStatusException(HttpStatus.LOCKED,"다른 편집 화면에서 사용 중입니다. 잠금을 해제해주세요.");
  if(token!=null && lease==null)throw new ResponseStatusException(HttpStatus.LOCKED,"편집 잠금이 만료되었습니다. 잠금을 확인해주세요.");
 }
 public String editResource(String kind,String itemId,JsonNode value,String previous){
  try{
   if("layout".equals(kind)){var path=json.readTree(itemId);if(!path.isArray() || path.size()<2)throw new IllegalArgumentException();return "blocks".equals(path.get(1).asText())?"canvas:boxes":"canvas:diagram";}
   if("list:notes".equals(kind)){var before=previous==null?json.nullNode():json.readTree(previous);String id=value==null?"":value.path("project").path("id").asText("");if(id.isEmpty())id=before.path("project").path("id").asText("");return id.isEmpty()?null:id;}
   return null;
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"편집 항목이 올바르지 않습니다.");}
 }
 @Transactional public Map<String,Object> status(String id,String actor,WeaverWorkspaceRequest request){
  boolean canEdit=!"viewer".equals(permissions.access(actor,id,false).role());
  if(request.documentLeases==null || request.documentLeases.size()>32)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var states=new HashMap<String,Object>();
  for(var entry:request.documentLeases.entrySet()){
   if(entry.getKey().length()>512 || entry.getValue()==null || !entry.getValue().matches("[a-f0-9]{64}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
   var lease=documentLocks.active(id,WeaverAuthService.hash(entry.getKey()));
   states.put(entry.getKey(),Map.of("acquired",canEdit && (sharedEditing(lease) || (lease!=null && actor.equals(lease.get("userId")) && WeaverAuthService.hash(entry.getValue()).equals(lease.get("clientHash")))),"holder",lease==null?"":Objects.toString(lease.get("name"),"다른 사용자")));
  }
  Long revision=mapper.revision(id);return Map.of("revision",revision==null?0:revision,"leases",states);
 }
 private String legacyType(JsonNode workspace,String id){for(String group:List.of("elements","trash"))for(JsonNode record:workspace.path(group))for(JsonNode clip:record.path("recordings"))if(id.equals(clip.path("id").asText()))return "audio";return "image";}
 private void storeFile(String user,String id,String type,byte[] data){
  if(!List.of("image","audio").contains(type))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"첨부 종류가 필요합니다.");
  try {
   String key=WeaverAuthService.hash(id),hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
   var old=mapper.media(user,key);
   if(old!=null){if(!old.hash().equals(hash) || !old.type().equals(type))throw new ResponseStatusException(HttpStatus.CONFLICT,"첨부파일 식별자가 충돌했습니다.");return;}
   if(mapper.itemBytes(user)+mapper.mediaBytes(user)+history.bytes(user)+data.length>maxBytes)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"워크스페이스 저장 한도를 초과했습니다.");
   Path directory=resourceRoot.resolve("contents/note").resolve(type).resolve(user);Files.createDirectories(directory);
   Path file=directory.resolve(key+"-"+UUID.randomUUID()+".bin"),temp=Files.createTempFile(directory,".upload-",".tmp");
   try {Files.write(temp,data);Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
   org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(file);}catch(java.io.IOException ignored){}}});
   mapper.mediaPut(user,key,id,resourceRoot.relativize(file).toString(),type,data.length,hash);
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"첨부파일을 저장하지 못했습니다.");}
 }
 @Transactional public Map<String,Object> mediaWrite(String id,String email,WeaverWorkspaceRequest request){return mediaWrite(id,email,id,request);}
 @Transactional public Map<String,Object> mediaWrite(String id,String email,String actor,WeaverWorkspaceRequest request){
  lock(id,email);permissions.access(actor,id,true);if(request.mediaId==null || request.mediaId.isEmpty() || request.mediaId.length()>512 || request.data==null || request.data.length()>maxBytes*4/3+4)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  byte[] data;try{data=Base64.getDecoder().decode(request.data);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST);}
  storeFile(id,request.mediaId,request.mediaType,data);return Map.of("stored",true);
 }
 @Transactional public Map<String,Object> mediaRead(String id,String email,WeaverWorkspaceRequest request){return mediaRead(id,email,id,request);}
 @Transactional public Map<String,Object> mediaRead(String id,String email,String actor,WeaverWorkspaceRequest request){
  lock(id,email);permissions.access(actor,id,false);if(request.mediaId==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);var metadata=mapper.media(id,WeaverAuthService.hash(request.mediaId));if(metadata==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  try {Path file=resourceRoot.resolve(metadata.path()).normalize();if(!file.startsWith(resourceRoot.resolve("contents/note").resolve(metadata.type()).resolve(id)))throw new IllegalArgumentException();
   byte[] data=Files.readAllBytes(file);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));if(!hash.equals(metadata.hash()))throw new IllegalArgumentException();
   return Map.of("data",Base64.getEncoder().encodeToString(data),"mediaType",metadata.type());
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"첨부파일을 읽지 못했습니다.");}
 }
}
