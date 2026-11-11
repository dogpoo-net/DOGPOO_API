package net.dogpoo.api.core.weaver.service;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.model.WeaverSocialIdentity;
@Service
public class WeaverKakaoGateway {
 private final long appId;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 private final ObjectMapper json=new ObjectMapper();
 public WeaverKakaoGateway(@Value("${KAKAO_LOGIN_APP_ID:0}") long appId){this.appId=appId;}
 public WeaverSocialIdentity verify(String token){
  if(appId<=0)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"카카오 로그인 서버 설정이 필요합니다.");
  if(token==null || token.isBlank() || token.length()>4096 || token.chars().anyMatch(Character::isWhitespace))throw denied();
  var info=get("/v1/user/access_token_info",token);
  if(info.path("app_id").asLong(-1)!=appId || info.path("expires_in").asLong(0)<=0)throw denied();
  var profile=get("/v2/user/me",token);
  return identity(info,profile,appId);
 }
 static WeaverSocialIdentity identity(JsonNode info,JsonNode profile,long expectedAppId){
  if(info.path("app_id").asLong(-1)!=expectedAppId || info.path("expires_in").asLong(0)<=0 ||
    !info.path("id").asText().equals(profile.path("id").asText()) || profile.path("id").asLong(0)<=0)throw denied();
  var account=profile.path("kakao_account");
  if(!account.path("is_email_valid").asBoolean() || !account.path("is_email_verified").asBoolean())
   throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"카카오 계정의 인증된 이메일 제공 동의가 필요합니다.");
  String email=WeaverAuthService.email(account.path("email").asText());
  String name=WeaverAuthService.name(account.path("profile").path("nickname").asText());
  return new WeaverSocialIdentity("kakao",profile.path("id").asText(),email,name);
 }
 private JsonNode get(String path,String token){
  try {
   var request=HttpRequest.newBuilder(URI.create("https://kapi.kakao.com"+path)).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+token).GET().build();
   var response=client.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()==401 || response.statusCode()==403)throw denied();
   if(response.statusCode()!=200)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"카카오 인증 서버에 연결하지 못했습니다.");
   return json.readTree(response.body());
  }catch(ResponseStatusException e){throw e;}
  catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"카카오 인증 요청이 중단되었습니다.");}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"카카오 인증 서버에 연결하지 못했습니다.");}
 }
 private static ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"카카오 인증 정보를 확인하지 못했습니다. 다시 로그인해주세요.");}
}
