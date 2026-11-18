package net.dogpoo.api.core.weaver.service;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
class WeaverWorkspaceReplacementTest {
 WeaverWorkspaceMapper store=mock(WeaverWorkspaceMapper.class);
 WeaverWorkspaceService service=new WeaverWorkspaceService(store,mock(WeaverTestDataMapper.class),67108864,"/tmp/weaver-replace-test");
 @BeforeEach void setup(){when(store.lock("target")).thenReturn(7L);when(store.migrated("target")).thenReturn(true);when(store.owner("target")).thenReturn("owner");ReflectionTestUtils.setField(service,"permissions",mock(WeaverSharingService.class));}
 WeaverWorkspaceRequest request(){var r=new WeaverWorkspaceRequest();r.replaceContents=true;r.since=7;r.changes=new ArrayList<>();return r;}
 @Test void offlineReconcileRejectsNewServerChangesBeforeBoxMerge(){var r=request();r.replaceContents=false;r.offlineReconcile=true;r.since=6;var e=assertThrows(ResponseStatusException.class,()->service.save("target","email","owner",r,false));assertEquals(409,e.getStatusCode().value());verify(store,never()).advance(anyString());verify(store,never()).changes(anyString(),anyLong());}
 @Test void offlineReconcileIsOwnerOnly(){var r=request();r.replaceContents=false;r.offlineReconcile=true;var e=assertThrows(ResponseStatusException.class,()->service.save("target","email","editor",r,false));assertEquals(403,e.getStatusCode().value());verify(store,never()).advance(anyString());}
 @Test void staleImportCannotOverwriteConcurrentSave(){var r=request();r.since=6;var e=assertThrows(ResponseStatusException.class,()->service.save("target","email","owner",r,false));assertEquals(409,e.getStatusCode().value());verify(store,never()).advance(anyString());}
 @Test void editorCannotReplaceWholeWorkspace(){var e=assertThrows(ResponseStatusException.class,()->service.save("target","email","editor",request(),false));assertEquals(403,e.getStatusCode().value());verify(store,never()).advance(anyString());}
 @Test void incompleteReplacementCannotSilentlyLeaveOldItems(){when(store.changes("target",0)).thenReturn(List.of(new WeaverWorkspaceItem("list:elements","memo","{}",false,7,"","owner",null)));var e=assertThrows(ResponseStatusException.class,()->service.save("target","email","owner",request(),false));assertEquals(400,e.getStatusCode().value());verify(store,never()).advance(anyString());verify(store,never()).changes(eq("other"),anyLong());}
}
