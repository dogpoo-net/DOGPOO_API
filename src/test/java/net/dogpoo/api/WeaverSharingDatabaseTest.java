package net.dogpoo.api;
import java.util.*;import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.*;import net.dogpoo.api.core.weaver.model.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="WEAVER_AUTH_DB_TEST",matches="true")
@SpringBootTest(properties={"weaver.scheduling.enabled=false","WEAVER_RESOURCE_DIR=/tmp/weaver-workspace-media-tests"})
class WeaverSharingDatabaseTest {
 @Autowired WeaverSharingService sharing;@Autowired WeaverWorkspaceService data;@Autowired WeaverHistoryService history;@Autowired JdbcTemplate jdbc;
 String a=UUID.randomUUID().toString(),b=UUID.randomUUID().toString(),c=UUID.randomUUID().toString();ObjectMapper json=new ObjectMapper();
 String email(String id){return "sharing-"+id+"@example.invalid";}
 @BeforeEach void setup(){for(String id:List.of(a,b,c))jdbc.update("INSERT INTO WEAVER_USER_ACCOUNT(id,email,password_hash,display_name,verified_at) VALUES(?,?,?,'Test',UTC_TIMESTAMP(6))",id,email(id),"unused-test-hash");}
 @AfterEach void cleanup(){jdbc.update("DELETE FROM WEAVER_USER_ACCOUNT WHERE id IN (?,?,?)",a,b,c);}
 void join(String person,String role){var invite=sharing.invite(a,a,email(person),role);sharing.accept(person,email(person),(String)invite.get("token"),null);}
 WeaverWorkspaceRequest doc(String text,long base){var r=new WeaverWorkspaceRequest();var change=new WeaverWorkspaceRequest.Change();change.kind="list:notes";change.id="note-1";change.baseRevision=base;change.value=json.valueToTree(Map.of("id","note-1","project",Map.of("id","doc-1","document",text)));r.changes=List.of(change);return r;}
 @Test void pendingEditorsReserveSeatsAndViewersDoNot(){
  var first=sharing.invite(a,a,email(b),"editor");assertThrows(ResponseStatusException.class,()->sharing.invite(a,a,email(c),"editor"));join(c,"viewer");
  sharing.accept(b,email(b),(String)first.get("token"),null);assertEquals("editor",sharing.access(b,a,true).role());assertEquals("viewer",sharing.access(c,a,false).role());
  assertThrows(ResponseStatusException.class,()->sharing.setRole(a,a,c,"editor"));assertEquals(2,((List<?>)sharing.catalog(b,email(b)).get("workspaces")).size());
  sharing.remove(a,a,b);sharing.setRole(a,a,c,"editor");assertEquals("editor",sharing.access(c,a,true).role());
 }
 @Test void invitationIdentityRevocationAndOwnerManagementAreEnforced(){
  var invite=sharing.invite(a,a,email(b),"editor");String token=(String)invite.get("token"),id=(String)invite.get("inviteId");
  assertThrows(ResponseStatusException.class,()->sharing.accept(c,email(c),token,null));assertThrows(ResponseStatusException.class,()->sharing.revoke(b,a,id));sharing.revoke(a,a,id);assertThrows(ResponseStatusException.class,()->sharing.accept(b,email(b),token,null));
 }
 @Test void viewerCannotWriteMediaOrDocumentsAndRemovedMembersCannotRead(){
  join(b,"viewer");data.read(a,email(a),b,0);assertThrows(ResponseStatusException.class,()->data.save(a,email(a),b,doc("blocked",0),false));
  var media=new WeaverWorkspaceRequest();media.mediaId="test";media.mediaType="image";media.data="AQID";assertThrows(ResponseStatusException.class,()->data.mediaWrite(a,email(a),b,media));
  sharing.remove(a,a,b);assertThrows(ResponseStatusException.class,()->data.read(a,email(a),b,0));assertTrue(((List<?>)data.read(b,email(b),0).get("changes")).isEmpty());
 }
 @Test void historyHasServerActorUnicodeDiffAndConflictsDoNotCreateVersions(){
  join(b,"editor");data.save(a,email(a),a,doc("가😀\n둘",0),false);data.save(a,email(a),b,doc("가😀!\n둘",1),false);
  var rows=history.list(a,"doc-1",Long.MAX_VALUE);assertEquals(2,rows.size());assertEquals(b,rows.get(0).get("actorId"));assertEquals(1,((Number)rows.get(0).get("added")).intValue());assertEquals(0,((Number)rows.get(0).get("removed")).intValue());assertTrue(rows.get(0).get("createdAt").toString().endsWith("Z"));
  assertThrows(ResponseStatusException.class,()->data.save(a,email(a),a,doc("stale",1),false));assertEquals(2,history.list(a,"doc-1",Long.MAX_VALUE).size());
  data.save(a,email(a),b,doc("가😀!\n둘",2),false);assertEquals(2,history.list(a,"doc-1",Long.MAX_VALUE).size());
 }
 @Test void documentLeaseIsExclusiveExpiresAndRequiresMatchingEditorToken(){
  join(b,"editor");var lock=new WeaverWorkspaceRequest();lock.documentId="doc-1";lock.leaseToken="a".repeat(64);
  assertEquals(true,data.documentLease(a,email(a),a,lock).get("acquired"));var other=new WeaverWorkspaceRequest();other.documentId="doc-1";other.leaseToken="b".repeat(64);
  assertEquals(false,data.documentLease(a,email(a),b,other).get("acquired"));assertThrows(ResponseStatusException.class,()->data.save(a,email(a),b,doc("blocked",0),false));
  var edit=doc("mine",0);edit.documentLeases=Map.of("doc-1",lock.leaseToken);data.save(a,email(a),a,edit,false);
  jdbc.update("UPDATE WEAVER_DOCUMENT_LOCK SET expires_at=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 SECOND) WHERE workspace_id=?",a);
  var stale=doc("expired",1);stale.documentLeases=edit.documentLeases;assertThrows(ResponseStatusException.class,()->data.save(a,email(a),a,stale,false));
  assertEquals(true,data.documentLease(a,email(a),b,other).get("acquired"));other.release=true;data.documentLease(a,email(a),b,other);assertEquals(true,data.documentLease(a,email(a),a,lock).get("acquired"));
 }
}
