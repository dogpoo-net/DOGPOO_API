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
}
