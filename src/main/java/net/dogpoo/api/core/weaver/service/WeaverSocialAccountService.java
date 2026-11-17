package net.dogpoo.api.core.weaver.service;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
@Service
public class WeaverSocialAccountService {
 @org.springframework.beans.factory.annotation.Autowired private WeaverWorkspacePolicyService policy;
 @org.springframework.beans.factory.annotation.Autowired private WeaverProfileMapper profiles;
 private final WeaverAuthMapper accounts;
 private final WeaverSessionMapper sessions;
 private final WeaverSocialMapper social;
 private final PasswordEncoder encoder;
 public WeaverSocialAccountService(WeaverAuthMapper accounts,WeaverSessionMapper sessions,WeaverSocialMapper social,PasswordEncoder encoder){this.accounts=accounts;this.sessions=sessions;this.social=social;this.encoder=encoder;}
 @Transactional public WeaverAccount resolve(WeaverSocialIdentity profile,String linkTo){
  accounts.lockIdentityWrites();
  String email=WeaverAuthService.email(profile.email());
  WeaverAccount known=social.find(profile.provider(),profile.subject());
  if(known!=null){
   if(linkTo!=null && !known.id().equals(linkTo))throw new ResponseStatusException(HttpStatus.CONFLICT,"다른 Weaver 계정에 이미 연결된 소셜 계정입니다.");
   checkEmail(email,known.id());social.email(profile.provider(),profile.subject(),email);
   avatar(known.id(),profile);return known;
  }
  if(linkTo!=null){
   WeaverAccount account=sessions.account(linkTo);
   if(account==null || account.verifiedAt()==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다.");
   checkEmail(email,linkTo);
   if(profiles.providers(linkTo).contains(profile.provider()))throw new ResponseStatusException(HttpStatus.CONFLICT,"이 제공자의 계정이 이미 연결되어 있습니다. 먼저 추가 연결을 해제해주세요.");
   social.link(profile.provider(),profile.subject(),linkTo);social.email(profile.provider(),profile.subject(),email);avatar(linkTo,profile);return account;
  }
  if(accounts.emailUsed(email,"")>0 || accounts.lockAccount(email)!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"같은 이메일의 계정이 있습니다. 기존 로그인 방식으로 로그인한 뒤 프로필에서 소셜 계정을 연결해주세요.");
  String id=UUID.randomUUID().toString();
  // A social-only account has an unknowable password; no provider token is stored.
  accounts.insertAccount(id,profile.email(),encoder.encode(UUID.randomUUID()+UUID.randomUUID().toString()),profile.name());
  accounts.signupProvider(id,profile.provider());
  if(policy!=null)policy.initializeAccount(id);
  profiles.ensure(id);profiles.email(id,false);
  social.verified(id);social.link(profile.provider(),profile.subject(),id);social.email(profile.provider(),profile.subject(),email);avatar(id,profile);return sessions.account(id);
 }
 private void checkEmail(String email,String id){
  if(accounts.emailUsed(email,id)>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"다른 계정에서 사용 중인 이메일입니다. 해당 계정으로 로그인한 뒤 연결해주세요.");
 }
 @Transactional public void unlink(String id,String provider){
  if(provider==null || !java.util.Set.of("google","kakao","naver","github","gitlab").contains(provider))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"지원하지 않는 소셜 계정입니다.");
  accounts.lockIdentityWrites();
  String initial=profiles.signupProvider(id);
  if(initial==null || initial.equals("unknown") || initial.equals(provider))throw new ResponseStatusException(HttpStatus.CONFLICT,"최초 가입에 사용한 계정은 연결 해제할 수 없습니다.");
  if(!initial.equals("email") && !profiles.providers(id).contains(initial))throw new ResponseStatusException(HttpStatus.CONFLICT,"최초 로그인 수단을 확인할 수 없습니다.");
  social.unlink(id,provider);
 }
 private void avatar(String id,WeaverSocialIdentity profile){
  profiles.ensure(id);
  String url=WeaverProfileService.safeAvatar(profile.avatarUrl());
  if(url!=null){social.avatar(id,profile.provider(),profile.subject(),url);profiles.socialAvatar(id,url);}
 }
}
