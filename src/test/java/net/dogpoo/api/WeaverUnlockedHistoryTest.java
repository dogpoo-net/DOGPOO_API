package net.dogpoo.api;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
import net.dogpoo.api.core.weaver.service.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WeaverUnlockedHistoryTest {
 static String hash(String text) throws Exception { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
 @Test void storedHistoryUsesServerLockState() throws Exception {
  for(boolean shared:List.of(false,true)){
   var mapper=mock(WeaverWorkspaceMapper.class);var locks=mock(WeaverDocumentLockMapper.class);var history=mock(WeaverHistoryMapper.class);
   var service=new WeaverWorkspaceService(mapper,mock(WeaverTestDataMapper.class),10000000,"/tmp/weaver-test");
   ReflectionTestUtils.setField(service,"history",new WeaverHistoryService(history));
   ReflectionTestUtils.setField(service,"permissions",mock(WeaverSharingService.class));
   ReflectionTestUtils.setField(service,"documentLocks",locks);
   when(mapper.migrated("workspace")).thenReturn(true);
   String before="{\"project\":{\"id\":\"doc\",\"document\":\"이전\"}}";
   when(mapper.item(eq("workspace"),anyString())).thenReturn(new WeaverWorkspaceItem("list:notes","note",before,false,1,null,null,null));
   String token="a".repeat(64);
   when(locks.active(eq("workspace"),anyString())).thenReturn(Map.of("userId","author","clientHash",hash(token)));
   if(shared)when(locks.active(eq("workspace"),anyString())).thenReturn(Map.of("sharedEditing",true));
   var change=new WeaverWorkspaceRequest.Change();change.kind="list:notes";change.id="note";change.baseRevision=1;
   change.value=new ObjectMapper().readTree("{\"project\":{\"id\":\"doc\",\"document\":\"새 문장\"}}");
   var request=new WeaverWorkspaceRequest();request.changes=List.of(change);request.resourceId="doc";request.releaseAfterSave=true;request.documentLeases=Map.of("doc",token);
   service.save("workspace","test@example.invalid","author",request,false);
   @SuppressWarnings("unchecked") var captured=org.mockito.ArgumentCaptor.forClass(Map.class);verify(history).insert(captured.capture());
   assertEquals(shared?"unlocked_edit":"edit",captured.getValue().get("action"));
   verify(locks).release("workspace",hash("doc"),"author",hash(token));
   assertEquals("이전",captured.getValue().get("before"));assertEquals("새 문장",captured.getValue().get("after"));
   clearInvocations(locks);request.releaseAfterSave=false;service.save("workspace","test@example.invalid","author",request,false);
   verify(locks,never()).release(anyString(),anyString(),anyString(),anyString());
  }
 }
}
