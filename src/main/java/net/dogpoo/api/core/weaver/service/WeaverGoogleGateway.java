package net.dogpoo.api.core.weaver.service;
import java.net.*;import java.net.http.*;import java.time.*;import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.model.WeaverSocialIdentity;
@Service public class WeaverGoogleGateway {
 private final String audience;private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 public WeaverGoogleGateway(@Value("${WEAVER_GOOGLE_CLIENT_ID:905636849457-6ohbrffa2dalid9vbl479jat2rr7njoe.apps.googleusercontent.com}") String audience){this.audience=audience;}
 public WeaverSocialIdentity verify(String token){
  if(token==null || token.length()>12000 || token.isBlank())throw denied();
  try {
   var response=client.send(HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token="+URLEncoder.encode(token,StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=200)throw denied();return identity(new ObjectMapper().readTree(response.body()),audience,Instant.now().getEpochSecond());
  }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw denied();}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Google 인증 서버에 연결하지 못했습니다.");}
 }
 public WeaverSocialIdentity verifyAccessToken(String token){
  if(token==null || token.length()>12000 || token.isBlank())throw denied();
  try {
   var info=client.send(HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/tokeninfo?access_token="+URLEncoder.encode(token,StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofString());
   if(info.statusCode()!=200)throw denied();
   var claims=new ObjectMapper().readTree(info.body());
   validateAccess(claims,audience,Instant.now().getEpochSecond());
   var profile=client.send(HttpRequest.newBuilder(URI.create("https://openidconnect.googleapis.com/v1/userinfo")).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
   if(profile.statusCode()!=200)throw denied();
   return accessIdentity(claims,new ObjectMapper().readTree(profile.body()),audience,Instant.now().getEpochSecond());
  }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw denied();}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Google 인증 서버에 연결하지 못했습니다.");}
 }
 static void validateAccess(JsonNode claims,String audience,long now){
  if(!audience.equals(claims.path("aud").asText()) ||
     (claims.hasNonNull("azp") && !audience.equals(claims.path("azp").asText())) ||
     claims.path("exp").asLong()<=now || claims.path("sub").asText().isBlank() || claims.path("sub").asText().length()>128)throw denied();
 }
 static WeaverSocialIdentity accessIdentity(JsonNode claims,JsonNode profile,String audience,long now){
  validateAccess(claims,audience,now);
  if(!claims.path("sub").asText().equals(profile.path("sub").asText()) || !profile.path("email_verified").asBoolean())throw denied();
  String email=WeaverAuthService.email(profile.path("email").asText());
  String name=profile.path("name").asText();if(name.isBlank())name=email.split("@")[0];
  return new WeaverSocialIdentity("google",profile.path("sub").asText(),email,WeaverAuthService.socialName(name,email),profile.path("picture").asText(null));
 }
 static WeaverSocialIdentity identity(JsonNode p,String audience,long now){
  if(!audience.equals(p.path("aud").asText()) || !("https://accounts.google.com".equals(p.path("iss").asText()) || "accounts.google.com".equals(p.path("iss").asText())) || p.path("exp").asLong()<=now || !p.path("email_verified").asBoolean())throw denied();
  String sub=p.path("sub").asText();if(sub.isBlank() || sub.length()>128)throw denied();
  String email=WeaverAuthService.email(p.path("email").asText());String name=p.path("name").asText();if(name.isBlank())name=email.split("@")[0];
  return new WeaverSocialIdentity("google",sub,email,WeaverAuthService.socialName(name,email),p.path("picture").asText(null));
 }
 private static ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Google 인증 정보를 확인하지 못했습니다. 다시 로그인해주세요.");}
}
