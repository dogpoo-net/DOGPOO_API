package net.dogpoo.api;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
import net.dogpoo.api.core.weaver.service.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WeaverLazyLeaseTest {
 static String hash(String text) throws Exception { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
 @Test void observeDoesNotAcquireAndSharedOverrideKeepsEveryoneWritable() throws Exception {
  var mapper=mock(WeaverWorkspaceMapper.class);var locks=mock(WeaverDocumentLockMapper.class);
  var service=new WeaverWorkspaceService(mapper,mock(WeaverTestDataMapper.class),1000000,"/tmp/weaver-test");
  ReflectionTestUtils.setField(service,"permissions",mock(WeaverSharingService.class));ReflectionTestUtils.setField(service,"documentLocks",locks);
  when(mapper.migrated("workspace")).thenReturn(true);when(locks.active(anyString(),anyString())).thenReturn(null);
  var request=new WeaverWorkspaceRequest();request.documentId="doc";request.leaseToken="a".repeat(64);request.observeOnly=true;
  assertEquals(true,service.documentLease("workspace","a@example.invalid","a",request).get("available"));verify(locks,never()).renew(anyString(),anyString(),anyString(),anyString());
  request.observeOnly=false;assertEquals(true,service.documentLease("workspace","a@example.invalid","a",request).get("acquired"));
  verify(locks).renew("workspace",hash("doc"),"a",hash(request.leaseToken));
  when(locks.active(anyString(),anyString())).thenReturn(Map.of("userId","a","clientHash",hash(request.leaseToken),"name","A"));
  var other=new WeaverWorkspaceRequest();other.documentId="doc";other.leaseToken="b".repeat(64);
  assertEquals(false,service.documentLease("workspace","b@example.invalid","b",other).get("acquired"));
  other.takeover=true;assertEquals(true,service.documentLease("workspace","b@example.invalid","b",other).get("shared"));
  when(locks.active(anyString(),anyString())).thenReturn(Map.of("sharedEditing",true));
  assertEquals(true,service.documentLease("workspace","a@example.invalid","a",request).get("acquired"));
  other.takeover=false;assertEquals(true,service.documentLease("workspace","b@example.invalid","b",other).get("acquired"));
 }
}
