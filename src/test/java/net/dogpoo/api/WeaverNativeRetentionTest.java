package net.dogpoo.api;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import net.dogpoo.api.core.weaver.service.*;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
import org.springframework.web.server.ResponseStatusException;
class WeaverNativeRetentionTest {
 @Test void oldNativeSessionRenewsWithoutAbsoluteDeadlineButIdleExpiryStillApplies(){
  var now=LocalDateTime.of(2026,11,13,0,0);var mapper=mock(WeaverSessionMapper.class);
  var service=new WeaverSessionService(mapper,mock(WeaverAuthService.class),Clock.fixed(now.toInstant(ZoneOffset.UTC),ZoneOffset.UTC),"test");
  when(mapper.account("user")).thenReturn(new WeaverAccount("user","test@example.invalid","",now));
  when(mapper.access(anyString(),any())).thenReturn(new WeaverDeviceSession("session","user","native","hash",null,null,null,now.minusYears(2),now.minusDays(10),now.plusDays(80),null));
  assertEquals("user",service.me("Bearer "+"a".repeat(43)).get("id"));
  verify(mapper).touch("session",now,now.plusDays(90));
  when(mapper.access(anyString(),any())).thenReturn(new WeaverDeviceSession("session","user","native","hash",null,null,null,now.minusYears(2),now.minusDays(90),now,null));
  assertThrows(ResponseStatusException.class,()->service.me("Bearer "+"a".repeat(43)));
 }
}
