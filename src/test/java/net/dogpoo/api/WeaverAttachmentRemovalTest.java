package net.dogpoo.api;
import java.util.*;
import java.nio.file.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
import net.dogpoo.api.core.weaver.service.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WeaverAttachmentRemovalTest {
 @TempDir Path root;
 @Test void replacementDeletesOnlyUnreferencedFilesAfterCommit() throws Exception {
  for(boolean shared:List.of(false,true)){
   var mapper=mock(WeaverWorkspaceMapper.class);
   var service=new WeaverWorkspaceService(mapper,mock(WeaverTestDataMapper.class),1000000,root.toString());
   ReflectionTestUtils.setField(service,"permissions",mock(WeaverSharingService.class));
   ReflectionTestUtils.setField(service,"history",mock(WeaverHistoryService.class));
   var old="{\"photos\":[{\"id\":\"photo\"}],\"recordings\":[{\"id\":\"old-audio\"}]}";
   var after="{\"photos\":[],\"recordings\":[{\"id\":\"new-audio\"}]}";
   when(mapper.migrated("workspace")).thenReturn(true);
   when(mapper.item(anyString(),anyString())).thenReturn(new WeaverWorkspaceItem("list:elements","memo",old,false,1,null,null,null));
   when(mapper.attachmentRecords("workspace")).thenReturn(shared?List.of(old,after):List.of(after));
   Path file=root.resolve("contents/note/audio/workspace/old.wav");Files.createDirectories(file.getParent());Files.write(file,new byte[]{1});
   when(mapper.media(anyString(),anyString())).thenReturn(new WeaverMediaFile("contents/note/audio/workspace/old.wav","audio",1,"hash"));
   var request=new WeaverWorkspaceRequest();var change=new WeaverWorkspaceRequest.Change();change.kind="list:elements";change.id="memo";change.baseRevision=1;change.value=new ObjectMapper().readTree(after);request.changes=List.of(change);
   TransactionSynchronizationManager.initSynchronization();
   try{
    service.save("workspace","user@example.invalid",request,false);
    assertTrue(Files.exists(file));
    if(shared)verify(mapper,never()).removeMedia(anyString(),anyString());
    else verify(mapper,times(2)).removeMedia(anyString(),anyString());
    for(var callback:TransactionSynchronizationManager.getSynchronizations())callback.afterCommit();
    assertEquals(shared,Files.exists(file));
   }finally{TransactionSynchronizationManager.clearSynchronization();}
  }
 }
}
