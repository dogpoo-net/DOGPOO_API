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
 @org.springframework.beans.factory.annotation.Autowired private WeaverProfileMapper profiles;
 private final WeaverAuthMapper accounts;
 private final WeaverSessionMapper sessions;
 private final WeaverSocialMapper social;
 private final PasswordEncoder encoder;
 public WeaverSocialAccountService(WeaverAuthMapper accounts,WeaverSessionMapper sessions,WeaverSocialMapper social,PasswordEncoder encoder){this.accounts=accounts;this.sessions=sessions;this.social=social;this.encoder=encoder;}
 @Transactional public WeaverAccount resolve(WeaverSocialIdentity profile,String linkTo){
  WeaverAccount known=social.find(profile.provider(),profile.subject());
  if(known!=null){
   if(linkTo!=null && !known.id().equals(linkTo))throw new ResponseStatusException(HttpStatus.CONFLICT,"다른 Weaver 계정에 이미 연결된 소셜 계정입니다.");
   avatar(known.id(),profile);return known;
  }
  if(linkTo!=null){
   WeaverAccount account=sessions.account(linkTo);
   if(account==null || account.verifiedAt()==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다.");
   social.link(profile.provider(),profile.subject(),linkTo);avatar(linkTo,profile);return account;
  }
  if(accounts.lockAccount(profile.email())!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"같은 이메일의 계정이 있습니다. 기존 로그인 방식으로 로그인한 뒤 프로필에서 소셜 계정을 연결해주세요.");
  String id=UUID.randomUUID().toString();
  // A social-only account has an unknowable password; no provider token is stored.
  accounts.insertAccount(id,profile.email(),encoder.encode(UUID.randomUUID()+UUID.randomUUID().toString()),profile.name());
  profiles.ensure(id);profiles.email(id,false);avatar(id,profile);
  social.verified(id);social.link(profile.provider(),profile.subject(),id);return sessions.account(id);
 }
 private void avatar(String id,WeaverSocialIdentity profile){
  profiles.ensure(id);
  String url=WeaverProfileService.safeAvatar(profile.avatarUrl());
  if(url!=null)profiles.socialAvatar(id,url);
 }
}
