package net.dogpoo.api;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
import net.dogpoo.api.core.weaver.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WeaverDocumentCompareSaveTest {
 @Test void staleVersionReturnsLatestWithoutWritingAndMatchingVersionSavesWithoutLease()throws Exception{
  var mapper=mock(WeaverWorkspaceMapper.class);var locks=mock(WeaverDocumentLockMapper.class);
  var service=new WeaverWorkspaceService(mapper,mock(WeaverTestDataMapper.class),1000000,"/tmp/document-cas");
  ReflectionTestUtils.setField(service,"permissions",mock(WeaverSharingService.class));ReflectionTestUtils.setField(service,"history",mock(WeaverHistoryService.class));ReflectionTestUtils.setField(service,"documentLocks",locks);
  when(mapper.migrated("w")).thenReturn(true);
  var old=new WeaverWorkspaceItem("list:notes","n","{\"project\":{\"id\":\"p\",\"document\":\"remote\"}}",false,7,null,null,null);
  when(mapper.item(anyString(),anyString())).thenReturn(old);
  var request=new WeaverWorkspaceRequest();request.resourceId="p";request.documentMerge=true;
  var c=new WeaverWorkspaceRequest.Change();c.kind="list:notes";c.id="n";c.value=new ObjectMapper().readTree("{\"project\":{\"id\":\"p\",\"document\":\"local\"}}");c.baseRevision=6;request.changes=List.of(c);
  assertEquals(old,service.save("w","e","actor",request,false).get("documentConflict"));verify(mapper,never()).advance(anyString());verifyNoInteractions(locks);
  c.baseRevision=7;service.save("w","e","actor",request,false);verify(mapper).advance("w");verifyNoInteractions(locks);
 }
 @Test void deletedDocumentCannotBeResurrectedByAnOpenEditor()throws Exception{
  var mapper=mock(WeaverWorkspaceMapper.class);var service=new WeaverWorkspaceService(mapper,mock(WeaverTestDataMapper.class),1000000,"/tmp/document-cas");
  ReflectionTestUtils.setField(service,"permissions",mock(WeaverSharingService.class));when(mapper.migrated("w")).thenReturn(true);
  var deleted=new WeaverWorkspaceItem("list:notes","n",null,true,8,null,null,null);when(mapper.item(anyString(),anyString())).thenReturn(deleted);
  var request=new WeaverWorkspaceRequest();request.resourceId="p";request.documentMerge=true;var c=new WeaverWorkspaceRequest.Change();c.kind="list:notes";c.id="n";c.value=new ObjectMapper().readTree("{\"project\":{\"id\":\"p\"}}");c.baseRevision=8;request.changes=List.of(c);
  assertEquals(deleted,service.save("w","e","actor",request,false).get("documentConflict"));verify(mapper,never()).advance(anyString());
 }
}
