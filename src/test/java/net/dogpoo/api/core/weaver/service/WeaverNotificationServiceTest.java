package net.dogpoo.api.core.weaver.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import net.dogpoo.api.core.weaver.mapper.WeaverNotificationMapper;
import org.springframework.web.server.ResponseStatusException;
class WeaverNotificationServiceTest {
 @Test void inboxAndUnreadAreScopedToAuthenticatedAccount(){
  var mapper=mock(WeaverNotificationMapper.class);var service=new WeaverNotificationService(mapper);
  when(mapper.list("alice",99)).thenReturn(List.of(Map.of("id",5L)));when(mapper.unread("alice")).thenReturn(2L);
  var result=service.list("alice",99,false);assertEquals(2L,result.get("unread"));
  verify(mapper).invitations("alice");verify(mapper).expired("alice");verify(mapper).list("alice",99);verify(mapper).unread("alice");
 }
 @Test void badgeDoesNotFetchBodiesAndReadAllUsesSnapshotBoundary(){
  var mapper=mock(WeaverNotificationMapper.class);var service=new WeaverNotificationService(mapper);
  assertEquals(List.of(),service.list("alice",Long.MAX_VALUE,true).get("items"));verify(mapper,never()).list(anyString(),anyLong());
  service.read("alice",7,false);verify(mapper).read("alice",7);
  service.read("alice",10,true);verify(mapper).readAll("alice",10);
  assertThrows(ResponseStatusException.class,()->service.read("alice",0,true));
  assertThrows(ResponseStatusException.class,()->service.list("alice",0,false));
 }
 @Test void retriedCommentsUseStableEventIdentity(){
  var mapper=mock(WeaverNotificationMapper.class);var service=new WeaverNotificationService(mapper);
  service.comment("workspace","memo","author","same-request");service.comment("workspace","memo","author","same-request");
  verify(mapper,times(2)).comment("workspace","memo","author","same-request");
 }
}
