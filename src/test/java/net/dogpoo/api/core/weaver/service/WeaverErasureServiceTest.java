package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.nio.file.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import net.dogpoo.api.core.weaver.mapper.WeaverWithdrawalMapper;
class WeaverErasureServiceTest {
 @TempDir Path root;
 @Test void removesOnlyQueuedAccountPathsWithoutFollowingSymlinks()throws Exception{
  String id=UUID.randomUUID().toString();var mapper=mock(WeaverWithdrawalMapper.class);when(mapper.pending()).thenReturn(List.of(Map.of("id",id,"kind","workspace"),Map.of("id",id,"kind","profile")));
  Path outside=root.resolve("keep.bin");Files.writeString(outside,"keep");
  for(String folder:List.of("contents/note/image","contents/note/video","contents/note/audio","contents/doc/image","profile/images")){Path dir=root.resolve(folder).resolve(id);Files.createDirectories(dir);Files.writeString(dir.resolve("private.bin"),"private");}
  Files.createSymbolicLink(root.resolve("contents/note/image").resolve(id).resolve("link"),outside);
  new WeaverErasureService(mapper,root.toString()).drain();assertTrue(Files.exists(outside));assertFalse(Files.exists(root.resolve("contents/note/image").resolve(id)));assertFalse(Files.exists(root.resolve("profile/images").resolve(id)));for(String folder:List.of("contents/note/video","contents/note/audio","contents/doc/image"))assertFalse(Files.exists(root.resolve(folder).resolve(id)));verify(mapper).complete(id,"workspace");verify(mapper).complete(id,"profile");
 }
 @Test void invalidJobNeverEscapesResourceRoot(){var mapper=mock(WeaverWithdrawalMapper.class);when(mapper.pending()).thenReturn(List.of(Map.of("id","../../etc","kind","workspace")));new WeaverErasureService(mapper,root.toString()).drain();verify(mapper,never()).complete(anyString(),anyString());}
}
