package net.dogpoo.api.core.weaver.service;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.model.WeaverSocialIdentity;
@Service
public class WeaverGitlabGateway {
 private final String clientId,secret,callback,base;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 private final ObjectMapper json=new ObjectMapper();
 public WeaverGitlabGateway(@Value("${WEAVER_GITLAB_CLIENT_ID:}") String clientId,@Value("${WEAVER_GITLAB_CLIENT_SECRET:}") String secret,
  @Value("${WEAVER_GITLAB_CALLBACK_URL:https://api.dogpoo.net/dogpoo/weaver/api/auth/gitlab/callback}") String callback,
  @Value("${WEAVER_GITLAB_BASE_URL:https://gitlab.com}") String base){this.clientId=clientId;this.secret=secret;this.callback=callback;this.base=base.replaceAll("/+$","");}
 private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
 public String authorize(String state){
  URI url=URI.create(base);
  if(clientId.isBlank() || secret.isBlank() || !"https".equals(url.getScheme()) || url.getHost()==null || url.getUserInfo()!=null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"GitLab 로그인 서버 설정이 필요합니다.");
  return base+"/oauth/authorize?response_type=code&client_id="+encode(clientId)+"&redirect_uri="+encode(callback)+"&scope="+encode("openid profile email")+"&state="+encode(state);
 }
 public WeaverSocialIdentity exchange(String code,String state){
  try {
   String body="grant_type=authorization_code&client_id="+encode(clientId)+"&client_secret="+encode(secret)+"&code="+encode(code)+"&redirect_uri="+encode(callback);
   var tokenResponse=client.send(HttpRequest.newBuilder(URI.create(base+"/oauth/token")).timeout(Duration.ofSeconds(10)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
   if(tokenResponse.statusCode()!=200)throw denied();
   String token=json.readTree(tokenResponse.body()).path("access_token").asText();if(token.isBlank())throw denied();
   var profileResponse=client.send(HttpRequest.newBuilder(URI.create(base+"/oauth/userinfo")).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
   if(profileResponse.statusCode()!=200)throw denied();return identity(json.readTree(profileResponse.body()));
  }catch(ResponseStatusException e){throw e;}
  catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"GitLab 인증 요청이 중단되었습니다.");}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"GitLab 인증 서버에 연결하지 못했습니다.");}
 }
 static WeaverSocialIdentity identity(JsonNode profile){
  String id=profile.path("sub").asText();if(id.isBlank() || id.length()>128)throw denied();
  if(!profile.path("email_verified").asBoolean())throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"GitLab 계정의 이메일 인증이 필요합니다.");
  String name=profile.path("name").asText();if(name.isBlank())name=profile.path("preferred_username").asText();
  return new WeaverSocialIdentity("gitlab",id,WeaverAuthService.email(profile.path("email").asText()),WeaverAuthService.name(name));
 }
 private static ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"GitLab 인증 정보를 확인하지 못했습니다. 다시 시도해주세요.");}
}
