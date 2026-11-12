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
public class WeaverGithubGateway {
 private final String clientId,secret,callback;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 private final ObjectMapper json=new ObjectMapper();
 public WeaverGithubGateway(@Value("${WEAVER_GITHUB_CLIENT_ID:}") String clientId,@Value("${WEAVER_GITHUB_CLIENT_SECRET:}") String secret,
  @Value("${WEAVER_GITHUB_CALLBACK_URL:https://api.dogpoo.net/dogpoo/weaver/api/auth/github/callback}") String callback){this.clientId=clientId;this.secret=secret;this.callback=callback;}
 private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
 public String authorize(String state){
  if(clientId.isBlank() || secret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"GitHub 로그인 서버 설정이 필요합니다.");
  return "https://github.com/login/oauth/authorize?client_id="+encode(clientId)+"&redirect_uri="+encode(callback)+"&scope="+encode("read:user user:email")+"&state="+encode(state);
 }
 public WeaverSocialIdentity exchange(String code,String state){
  try {
   String body="client_id="+encode(clientId)+"&client_secret="+encode(secret)+"&code="+encode(code)+"&redirect_uri="+encode(callback);
   var tokenResponse=client.send(HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token")).timeout(Duration.ofSeconds(10)).header("Accept","application/json").header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
   if(tokenResponse.statusCode()!=200)throw denied();
   String token=json.readTree(tokenResponse.body()).path("access_token").asText();if(token.isBlank())throw denied();
   return identity(get("/user",token),get("/user/emails",token));
  }catch(ResponseStatusException e){throw e;}
  catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"GitHub 인증 요청이 중단되었습니다.");}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"GitHub 인증 서버에 연결하지 못했습니다.");}
 }
 private JsonNode get(String path,String token) throws Exception {
  var request=HttpRequest.newBuilder(URI.create("https://api.github.com"+path)).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+token).header("User-Agent","WEAVER-Auth").header("Accept","application/vnd.github+json").GET().build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofString());if(response.statusCode()!=200)throw denied();return json.readTree(response.body());
 }
 static WeaverSocialIdentity identity(JsonNode profile,JsonNode emails){
  if(profile.path("id").asLong(0)<=0 || !emails.isArray())throw denied();
  String email=null;
  for(JsonNode candidate:emails)if(candidate.path("verified").asBoolean()){
   if(email==null || candidate.path("primary").asBoolean())email=candidate.path("email").asText();
   if(candidate.path("primary").asBoolean())break;
  }
  if(email==null)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"GitHub에 인증된 이메일을 등록하고 이메일 제공 권한을 허용해주세요.");
  String name=profile.path("name").asText("");if(name.isBlank())name=profile.path("login").asText();
  return new WeaverSocialIdentity("github",profile.path("id").asText(),WeaverAuthService.email(email),WeaverAuthService.socialName(name,email),profile.path("avatar_url").asText(null));
 }
 private static ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"GitHub 인증 정보를 확인하지 못했습니다. 다시 시도해주세요.");}
}
