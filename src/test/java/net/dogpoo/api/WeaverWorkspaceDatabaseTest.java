package net.dogpoo.api;
import java.util.*;import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.*;import net.dogpoo.api.core.weaver.model.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="WEAVER_AUTH_DB_TEST",matches="true")
@SpringBootTest(properties={"weaver.scheduling.enabled=false","WEAVER_RESOURCE_DIR=/tmp/weaver-workspace-media-tests"})
class WeaverWorkspaceDatabaseTest {
 @Autowired WeaverWorkspaceService service;@Autowired JdbcTemplate jdbc;
 String a=UUID.randomUUID().toString(),b=UUID.randomUUID().toString();ObjectMapper json=new ObjectMapper();
 String email(String id){return "owned-"+id+"@example.invalid";}
 @BeforeEach void setup(){for(String id:List.of(a,b))jdbc.update("INSERT INTO WEAVER_USER_ACCOUNT(id,email,password_hash,display_name,verified_at) VALUES(?,?,?,'Test',UTC_TIMESTAMP(6))",id,email(id),"unused-test-hash");}
 @AfterEach void cleanup(){jdbc.update("DELETE FROM WEAVER_USER_ACCOUNT WHERE id IN (?,?)",a,b);}
 WeaverWorkspaceRequest request(String id,String text,long base) throws Exception {
  var r=new WeaverWorkspaceRequest();var c=new WeaverWorkspaceRequest.Change();c.kind="list:elements";c.id=id;c.value=json.readTree("{\"id\":\""+id+"\",\"text\":\""+text+"\"}");c.baseRevision=base;r.changes=List.of(c);return r;
 }
 @Test void accountIsolationIncrementalVersionsAndAtomicConflicts() throws Exception {
  var first=service.save(a,email(a),request("one","first",0),false);assertEquals(1L,first.get("revision"));
  assertTrue(((List<?>)service.read(b,email(b),0).get("changes")).isEmpty());
  service.save(a,email(a),request("two","second",0),false);
  assertEquals(1,((List<?>)service.read(a,email(a),1).get("changes")).size());
  assertThrows(ResponseStatusException.class,()->service.save(a,email(a),request("one","stale",0),false));
  assertEquals(2L,service.read(a,email(a),0).get("revision"));
 }
 @Test void guestClaimCannotBeReusedByAnotherAccountAndRetryDoesNotOverwriteNewEdits() throws Exception {
  var r=request("guest","original",0);r.guestId=UUID.randomUUID().toString().replace("-","").repeat(2);r.sourceHash="b".repeat(64);
  service.save(a,email(a),r,true);service.save(a,email(a),request("guest","newer",1),false);
  var retry=service.save(a,email(a),r,true);assertEquals(2L,retry.get("revision"));
  assertThrows(ResponseStatusException.class,()->service.save(b,email(b),r,true));
  assertTrue(((List<?>)service.read(b,email(b),0).get("changes")).isEmpty());
 }
 @Test void mediaIsIsolatedAndIdsCannotOverwriteDifferentBytes(){
  var r=new WeaverWorkspaceRequest();r.mediaId="photo";r.mediaType="image";r.data=Base64.getEncoder().encodeToString(new byte[]{1,2,3});
  service.mediaWrite(a,email(a),r);assertEquals(r.data,service.mediaRead(a,email(a),r).get("data"));
  assertThrows(ResponseStatusException.class,()->service.mediaRead(b,email(b),r));
  r.data=Base64.getEncoder().encodeToString(new byte[]{4});assertThrows(ResponseStatusException.class,()->service.mediaWrite(a,email(a),r));
 }
 @Test void movingToTrashKeepsMediaButPermanentDeletionRemovesUnreferencedFiles()throws Exception{
  var media=new WeaverWorkspaceRequest();media.mediaId="cleanup-photo";media.mediaType="image";media.data=Base64.getEncoder().encodeToString(new byte[]{1,2,3});service.mediaWrite(a,email(a),media);
  var record=request("one","with photo",0);((com.fasterxml.jackson.databind.node.ObjectNode)record.changes.get(0).value).putArray("photos").addObject().put("id",media.mediaId).put("name","photo.jpg");service.save(a,email(a),record,false);
  var move=new WeaverWorkspaceRequest();var deleted=new WeaverWorkspaceRequest.Change();deleted.kind="list:elements";deleted.id="one";deleted.deleted=true;deleted.baseRevision=1;
  var trash=new WeaverWorkspaceRequest.Change();trash.kind="list:trash";trash.id="one";trash.value=record.changes.get(0).value;move.changes=List.of(deleted,trash);service.save(a,email(a),move,false);
  assertEquals(media.data,service.mediaRead(a,email(a),media).get("data"));
  trash.deleted=true;trash.baseRevision=2;trash.value=null;move.changes=List.of(trash);service.save(a,email(a),move,false);
  assertEquals(404,assertThrows(ResponseStatusException.class,()->service.mediaRead(a,email(a),media)).getStatusCode().value());
  try(var files=java.nio.file.Files.list(java.nio.file.Path.of("/tmp/weaver-workspace-media-tests/contents/note/image",a))){assertEquals(0,files.count());}
 }
 @Test void concurrentCanvasPatchesAndDocumentCompareSave()throws Exception{
  service.read(a,email(a),0);jdbc.update("INSERT INTO WEAVER_WORKSPACE_MEMBER(workspace_id,user_id,role) VALUES(?,?,'editor')",a,b);
  for(String scope:List.of("canvas:diagram","canvas:boxes","project-test")){
   boolean canvas=scope.startsWith("canvas:");var save=new WeaverWorkspaceRequest();
   if(canvas)save.canvasOperationId=UUID.randomUUID().toString().replace("-","").repeat(2);else{save.resourceId=scope;save.documentMerge=true;}
   var c=new WeaverWorkspaceRequest.Change();c.kind=canvas?"layout":"list:notes";
   c.id=scope.equals("canvas:boxes")?"[\"page\",\"blocks\",\"positions\",\"one\"]":scope.equals("canvas:diagram")?"[\"page\",\"positions\",\"one\"]":"doc";
   c.value=json.readTree(canvas?"[1,2]":"{\"project\":{\"id\":\"project-test\",\"document\":\"first\"}}");save.changes=List.of(c);
   var first=service.save(a,email(a),a,save,false);
   if(canvas)save.canvasOperationId=UUID.randomUUID().toString().replace("-","").repeat(2);
   c.value=json.readTree(canvas?"[8,9]":"{\"project\":{\"id\":\"project-test\",\"document\":\"last\"}}");
   var second=service.save(a,email(a),b,save,false);
   if(!canvas){assertTrue(second.containsKey("documentConflict"));c.baseRevision=((Number)first.get("revision")).longValue();service.save(a,email(a),b,save,false);}
   var rows=(List<WeaverWorkspaceItem>)service.read(a,email(a),0).get("changes");assertEquals(c.value.toString(),rows.stream().filter(r->r.id().equals(c.id)).findFirst().orElseThrow().payload());
  }
 }
}
