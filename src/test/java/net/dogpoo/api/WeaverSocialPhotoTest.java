package net.dogpoo.api;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
import net.dogpoo.api.core.weaver.service.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
class WeaverSocialPhotoTest {
 @Test void choosesStoredProviderPhotoAndNeverAcceptsAnArbitraryUrl(){
  var mapper=mock(WeaverProfileMapper.class);
  var service=new WeaverProfileService(mapper,mock(WeaverSessionMapper.class),mock(WeaverVerificationMail.class),mock(PasswordEncoder.class),"/tmp/profile-test");
  when(mapper.lock("owner")).thenReturn(new WeaverProfile(null,null,true,0));
  String photo="https://lh3.googleusercontent.com/avatar";
  when(mapper.originalSocialAvatar("owner")).thenReturn(photo);
  service.socialPhoto("owner");verify(mapper).selectSocialAvatar("owner",photo);
  clearInvocations(mapper);
  when(mapper.originalSocialAvatar("owner")).thenReturn(null);
  assertThrows(ResponseStatusException.class,()->service.socialPhoto("owner"));
  when(mapper.originalSocialAvatar("owner")).thenReturn("https://untrusted.example/avatar");
  assertThrows(ResponseStatusException.class,()->service.socialPhoto("owner"));
  verify(mapper,never()).selectSocialAvatar(anyString(),anyString());
 }
}
