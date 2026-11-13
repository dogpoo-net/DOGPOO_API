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
   when(locks.active(eq("workspace"),anyString())).thenReturn(null);
   if(shared)when(locks.active(eq("workspace"),anyString())).thenReturn(Map.of("sharedEditing",true));
   var change=new WeaverWorkspaceRequest.Change();change.kind="list:notes";change.id="note";change.baseRevision=1;
   change.value=new ObjectMapper().readTree("{\"project\":{\"id\":\"doc\",\"document\":\"새 문장\"}}");
   var request=new WeaverWorkspaceRequest();request.changes=List.of(change);
   service.save("workspace","test@example.invalid","author",request,false);
   @SuppressWarnings("unchecked") var captured=org.mockito.ArgumentCaptor.forClass(Map.class);verify(history).insert(captured.capture());
   assertEquals(shared?"unlocked_edit":"edit",captured.getValue().get("action"));
   assertEquals("이전",captured.getValue().get("before"));assertEquals("새 문장",captured.getValue().get("after"));
  }
 }
}
