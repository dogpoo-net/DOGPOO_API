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
public class WeaverNaverGateway {
 private final String clientId,secret,callback;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 private final ObjectMapper json=new ObjectMapper();
 public WeaverNaverGateway(@Value("${NAVER_CLIENT_ID:}") String clientId,@Value("${NAVER_CLIENT_SECRET:}") String secret,
  @Value("${NAVER_CALLBACK_URL:https://api.dogpoo.net/dogpoo/weaver/api/auth/naver/callback}") String callback){this.clientId=clientId;this.secret=secret;this.callback=callback;}
 private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
 public String authorize(String state){
  if(clientId.isBlank() || secret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"네이버 로그인 서버 설정이 필요합니다.");
  return "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id="+encode(clientId)+"&redirect_uri="+encode(callback)+"&state="+encode(state);
 }
 public WeaverSocialIdentity exchange(String code,String state){
  try {
   String body="grant_type=authorization_code&client_id="+encode(clientId)+"&client_secret="+encode(secret)+"&code="+encode(code)+"&state="+encode(state);
   var tokenResponse=client.send(HttpRequest.newBuilder(URI.create("https://nid.naver.com/oauth2.0/token")).timeout(Duration.ofSeconds(10)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
   if(tokenResponse.statusCode()!=200)throw denied();
   String token=json.readTree(tokenResponse.body()).path("access_token").asText();if(token.isBlank())throw denied();
   var profileResponse=client.send(HttpRequest.newBuilder(URI.create("https://openapi.naver.com/v1/nid/me")).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
   if(profileResponse.statusCode()!=200)throw denied();
   return identity(json.readTree(profileResponse.body()));
  }catch(ResponseStatusException e){throw e;}
  catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"네이버 인증 요청이 중단되었습니다.");}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"네이버 인증 서버에 연결하지 못했습니다.");}
 }
 static WeaverSocialIdentity identity(JsonNode data){
  if(!"00".equals(data.path("resultcode").asText()))throw denied();
  var profile=data.path("response");String id=profile.path("id").asText();
  if(id.isBlank() || id.length()>128)throw denied();
  return new WeaverSocialIdentity("naver",id,WeaverAuthService.email(profile.path("email").asText()),WeaverAuthService.socialName(profile.path("name").asText(),profile.path("email").asText()),profile.path("profile_image").asText(null));
 }
 private static ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"네이버 인증 정보를 확인하지 못했습니다. 다시 시도해주세요.");}
}
