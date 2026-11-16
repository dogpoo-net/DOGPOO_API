package net.dogpoo.api.core.weaver.service;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.*;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.WeaverProfile;
class WeaverDefaultPhotoTest {
 @TempDir Path root;
 @Test void defaultChangesAndPreviousUploadIsRemovedOnlyAfterCommit() throws Exception {
  var mapper=mock(WeaverProfileMapper.class);
  var service=new WeaverProfileService(mapper,mock(WeaverSessionMapper.class),mock(WeaverVerificationMail.class),mock(PasswordEncoder.class),root.toString());
  Path image=root.resolve("profile/images/user/photo.png");Files.createDirectories(image.getParent());Files.write(image,new byte[]{1});
  when(mapper.lock("user")).thenReturn(new WeaverProfile(null,"user/photo.png",true,2));
  TransactionSynchronizationManager.initSynchronization();
  try {
   service.defaultPhoto("user");
   verify(mapper).defaultPhoto(eq("user"),intThat(face->face>=0&&face<6&&face!=2));
   assertTrue(Files.exists(image));
   for(var callback:TransactionSynchronizationManager.getSynchronizations())callback.afterCommit();
   assertFalse(Files.exists(image));
  }finally{TransactionSynchronizationManager.clearSynchronization();}
 }
 @Test void replacingSocialPhotoNeedsNoFilesystemChanges(){
  var mapper=mock(WeaverProfileMapper.class);
  var service=new WeaverProfileService(mapper,mock(WeaverSessionMapper.class),mock(WeaverVerificationMail.class),mock(PasswordEncoder.class),root.toString());
  when(mapper.lock("user")).thenReturn(new WeaverProfile("https://example.com/photo",null,false,5));
  service.defaultPhoto("user");verify(mapper).defaultPhoto(eq("user"),intThat(face->face>=0&&face<5));
 }
}
