package net.dogpoo.api.core.weaver.service;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
@Service
public class WeaverOAuthService {
 private final WeaverOAuthMapper flows;
 private final WeaverNaverGateway gateway;
 private final WeaverGithubGateway github;
 private final WeaverGitlabGateway gitlab;
 private final WeaverSocialAccountService accounts;
 private final WeaverSessionService sessions;
 private final SecureRandom random=new SecureRandom();
 public WeaverOAuthService(WeaverOAuthMapper flows,WeaverNaverGateway gateway,WeaverGithubGateway github,WeaverGitlabGateway gitlab,WeaverSocialAccountService accounts,WeaverSessionService sessions){this.flows=flows;this.gateway=gateway;this.github=github;this.gitlab=gitlab;this.accounts=accounts;this.sessions=sessions;}
 private String random(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 private String hash(String s){return WeaverAuthService.hash(s);}
 private void check(String s){if(s==null || !s.matches("[A-Za-z0-9_-]{43}"))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인 요청을 확인할 수 없습니다.");}
 @Transactional public Map<String,Object> start(String provider,String platform,String proof,String link){
  check(proof);String id=random(),state=random();
  String url=switch(provider){case "naver"->gateway.authorize(state);case "github"->github.authorize(state);case "gitlab"->gitlab.authorize(state);default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST);};
  flows.cleanup();flows.insert(hash(id),hash(state),hash(proof),platform,provider,link);return Map.of("flow",id,"authorizeUrl",url);
 }
 public void callback(String provider,String code,String state,String error){
  check(state);String stateHash=hash(state);
  if(flows.claim(stateHash,provider)!=1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"만료되었거나 이미 처리한 로그인 요청입니다.");
  try {
   if(error!=null || code==null || code.isBlank() || code.length()>2048)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"소셜 로그인이 취소되었습니다.");
   flows.complete(stateHash,(switch(provider){case "github"->github.exchange(code,state);case "gitlab"->gitlab.exchange(code,state);default->gateway.exchange(code,state);}));
  }catch(ResponseStatusException e){flows.fail(stateHash,e.getStatusCode().value());throw e;}
 }
 @Transactional public Map<String,Object> finish(String provider,String id,String platform,String proof,String bearer,String cookie,String webProof){
  check(id);check(proof);String key=hash(id);WeaverOAuthFlow flow=flows.lock(key);
  if(flow==null || !provider.equals(flow.provider()) || !platform.equals(flow.platform()) || !MessageDigest.isEqual(hash(proof).getBytes(StandardCharsets.US_ASCII),flow.proofHash().getBytes(StandardCharsets.US_ASCII)))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인 요청이 만료되었거나 일치하지 않습니다.");
  if("failed".equals(flow.status()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"소셜 로그인이 취소되었거나 실패했습니다. 이름·이메일 동의 설정을 확인해주세요.");
  if(!"ready".equals(flow.status()))return Map.of("pending",true);
  if(flow.linkUserId()!=null){
   String current=(String)("web".equals(platform)?sessions.webResume(cookie,webProof):sessions.me(bearer)).get("id");
   if(!current.equals(flow.linkUserId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"연결을 시작한 계정으로 로그인해주세요.");
  }
  var account=accounts.resolve(new WeaverSocialIdentity(provider,flow.subject(),flow.email(),flow.displayName()),flow.linkUserId());
  Map<String,Object> result=flow.linkUserId()==null?sessions.issue(account,platform,webProof):Map.of("linked",true);
  flows.consume(key);return result;
 }
}
