package net.dogpoo.api.core.weaver.service;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.model.WeaverSocialIdentity;
@Service
public class WeaverBrowserIdentityGateway {
 private final String googleId,googleSecret,kakaoId,kakaoSecret,origin;
 private final WeaverGoogleGateway google;
 private final WeaverKakaoGateway kakao;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 public WeaverBrowserIdentityGateway(
 @Value("${WEAVER_GOOGLE_CLIENT_ID:905636849457-6ohbrffa2dalid9vbl479jat2rr7njoe.apps.googleusercontent.com}") String googleId,
 @Value("${WEAVER_GOOGLE_CLIENT_SECRET:}") String googleSecret,
 @Value("${KAKAO_REST_API_KEY:}") String kakaoId,
 @Value("${KAKAO_CLIENT_SECRET:}") String kakaoSecret,
 @Value("${WEAVER_OAUTH_CALLBACK_ORIGIN:https://api.dogpoo.net/dogpoo/weaver/api/auth}") String origin,
 WeaverGoogleGateway google,WeaverKakaoGateway kakao){this.googleId=googleId;this.googleSecret=googleSecret;this.kakaoId=kakaoId;this.kakaoSecret=kakaoSecret;this.origin=origin;this.google=google;this.kakao=kakao;}
 private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
 private boolean isGoogle(String provider){if(!provider.equals("google")&&!provider.equals("kakao"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return provider.equals("google");}
 public String authorize(String provider,String state){
 boolean g=isGoogle(provider);
 if((g?googleId:kakaoId).isBlank() || (g?googleSecret:kakaoSecret).isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"브라우저 로그인 서버 설정이 필요합니다.");
 return (g?"https://accounts.google.com/o/oauth2/v2/auth":"https://kauth.kakao.com/oauth/authorize")+"?response_type=code&client_id="+enc(g?googleId:kakaoId)+"&redirect_uri="+enc(origin+"/"+provider+"/callback")+"&state="+enc(state)+(g?"&scope=openid%20email%20profile&prompt=select_account":"");
 }
 public WeaverSocialIdentity exchange(String provider,String code){
 boolean g=isGoogle(provider);
 try{
 String body="grant_type=authorization_code&client_id="+enc(g?googleId:kakaoId)+"&client_secret="+enc(g?googleSecret:kakaoSecret)+"&redirect_uri="+enc(origin+"/"+provider+"/callback")+"&code="+enc(code);
 var response=client.send(HttpRequest.newBuilder(URI.create(g?"https://oauth2.googleapis.com/token":"https://kauth.kakao.com/oauth/token")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
 if(response.statusCode()!=200)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"소셜 인증에 실패했습니다.");
 var json=new ObjectMapper().readTree(response.body());
 return g?google.verify(json.path("id_token").asText()):kakao.verify(json.path("access_token").asText());
 }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"인증 요청이 중단되었습니다.");}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"인증 서버에 연결하지 못했습니다.");}
 }
}
