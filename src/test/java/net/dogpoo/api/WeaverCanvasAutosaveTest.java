package net.dogpoo.api;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
import net.dogpoo.api.core.weaver.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WeaverCanvasAutosaveTest {
 final ObjectMapper json=new ObjectMapper();
 final WeaverWorkspaceMapper mapper=mock(WeaverWorkspaceMapper.class);
 final WeaverDocumentLockMapper locks=mock(WeaverDocumentLockMapper.class);
 WeaverWorkspaceService service(){
  var service=new WeaverWorkspaceService(mapper,mock(WeaverTestDataMapper.class),10000000,"/tmp/canvas-test");
  ReflectionTestUtils.setField(service,"permissions",mock(WeaverSharingService.class));
  ReflectionTestUtils.setField(service,"history",mock(WeaverHistoryService.class));ReflectionTestUtils.setField(service,"documentLocks",locks);
  when(mapper.migrated("w")).thenReturn(true);return service;
 }
 WeaverWorkspaceRequest request(String kind,String id,String value)throws Exception{
  var r=new WeaverWorkspaceRequest();r.canvasOperationId="a".repeat(64);var c=new WeaverWorkspaceRequest.Change();c.kind=kind;c.id=id;c.value=json.readTree(value);r.changes=List.of(c);return r;
 }
 @Test void staleGeometryIsLastWriteWithoutLockOrDeletingOtherGeometry()throws Exception{
  var service=service();var r=request("layout","[\"page\",\"positions\",\"a\"]","[20,30]");
  when(mapper.item(anyString(),anyString())).thenReturn(new WeaverWorkspaceItem("layout",r.changes.getFirst().id,"[1,2]",false,40,null,null,null));
  service.save("w","e","actor",r,false);
  verify(mapper).put(eq("w"),anyString(),eq("layout"),eq(r.changes.getFirst().id),eq("[20,30]"),eq(false),anyLong());
  verifyNoInteractions(locks);verify(mapper,never()).changes(anyString(),anyLong());verify(mapper).canvasReceiptPut(eq("w"),eq(r.canvasOperationId),anyString());
 }
 @Test void duplicateReceiptDoesNotReplayOldCoordinates()throws Exception{
  var service=service();var r=request("layout","[\"page\",\"positions\",\"a\"]","[20,30]");
  when(mapper.canvasReceipt("w",r.canvasOperationId)).thenReturn(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.valueToTree(r.changes).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  var latest=new WeaverWorkspaceItem("layout",r.changes.getFirst().id,"[90,100]",false,42,null,null,null);
  when(mapper.item(anyString(),anyString())).thenReturn(latest);
  assertEquals(List.of(latest),service.save("w","e","actor",r,false).get("changes"));
  verify(mapper,never()).advance(anyString());verify(mapper,never()).put(anyString(),anyString(),anyString(),anyString(),any(),anyBoolean(),anyLong());
 }
 @Test void canvasProtocolCannotBypassDocumentLock()throws Exception{
  var service=service();assertThrows(ResponseStatusException.class,()->service.save("w","e","actor",request("list:notes","doc","{}"),false));
 }
 @Test void simultaneousBoxAdditionsMergeInsteadOfReplacingIds()throws Exception{
  var service=service();var r=request("list:memoBoxes","box","{\"name\":\"box\",\"ids\":[\"a\",\"c\"]}");r.canvasOperationId=null;
  var c=r.changes.getFirst();c.previousValue=json.readTree("{\"name\":\"box\",\"ids\":[\"a\"]}");
  var old=new WeaverWorkspaceItem("list:memoBoxes","box","{\"name\":\"box\",\"ids\":[\"a\",\"b\"]}",false,5,null,null,null);
  when(mapper.item(anyString(),anyString())).thenReturn(old);when(mapper.changes("w",0)).thenReturn(List.of(old));
  service.save("w","e","actor",r,false);
  assertEquals(json.readTree("[\"a\",\"b\",\"c\"]"),c.value.path("ids"));assertEquals(5,c.baseRevision);
 }
}
