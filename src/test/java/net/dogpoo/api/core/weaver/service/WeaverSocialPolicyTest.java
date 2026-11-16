package net.dogpoo.api.core.weaver.service;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
class WeaverSocialPolicyTest {
 WeaverAuthMapper accounts=mock(WeaverAuthMapper.class);WeaverSessionMapper sessions=mock(WeaverSessionMapper.class);WeaverSocialMapper social=mock(WeaverSocialMapper.class);WeaverProfileMapper profiles=mock(WeaverProfileMapper.class);
 WeaverSocialAccountService service=new WeaverSocialAccountService(accounts,sessions,social,mock(PasswordEncoder.class));
 WeaverSocialIdentity identity=new WeaverSocialIdentity("google","subject","same@example.com","Person");
 @BeforeEach void setup(){ReflectionTestUtils.setField(service,"profiles",profiles);when(sessions.account("own")).thenReturn(new WeaverAccount("own","same@example.com","hash",LocalDateTime.now()));when(profiles.providers("own")).thenReturn(List.of());}
 @Test void sameEmailOnSameAccountCanLink(){service.resolve(identity,"own");verify(accounts).emailUsed("same@example.com","own");verify(social).link("google","subject","own");verify(social).email("google","subject","same@example.com");}
 @Test void otherAccountsSocialEmailBlocksLinkAndSignup(){when(accounts.emailUsed("same@example.com","own")).thenReturn(1);assertThrows(ResponseStatusException.class,()->service.resolve(identity,"own"));verify(social,never()).link(any(),any(),any());when(accounts.emailUsed("same@example.com","")).thenReturn(1);assertThrows(ResponseStatusException.class,()->service.resolve(identity,null));verify(accounts,never()).insertAccount(any(),any(),any(),any());}
 @Test void initialSocialCannotUnlinkButAdditionalCan(){when(profiles.signupProvider("own")).thenReturn("kakao");when(profiles.providers("own")).thenReturn(List.of("kakao","google"));assertThrows(ResponseStatusException.class,()->service.unlink("own","kakao"));service.unlink("own","google");verify(social).unlink("own","google");verify(social,never()).unlink("own","kakao");}
 @Test void emailSignupCanUnlinkEverySocial(){when(profiles.signupProvider("own")).thenReturn("email");for(String provider:List.of("google","kakao","naver","github","gitlab")){service.unlink("own",provider);verify(social).unlink("own",provider);}}
 @Test void knownSocialCannotMoveBetweenAccounts(){when(social.find("google","subject")).thenReturn(new WeaverAccount("other","other@example.com","hash",LocalDateTime.now()));assertThrows(ResponseStatusException.class,()->service.resolve(identity,"own"));verify(social,never()).email(any(),any(),any());}
 @Test void emailSignupChecksLinkedEmailBeforeCreatingAccount(){var mail=mock(WeaverVerificationMail.class);var encoder=mock(PasswordEncoder.class);var auth=new WeaverAuthService(accounts,encoder,mail);var r=new WeaverAuthRequest();r.email="same@example.com";r.name="Name";r.password="valid-long-password";r.passwordConfirmation=r.password;when(accounts.emailUsed("same@example.com","")).thenReturn(1);auth.signup(r);verify(accounts).lockIdentityWrites();verify(accounts,never()).insertAccount(any(),any(),any(),any());verifyNoInteractions(mail);}
}
