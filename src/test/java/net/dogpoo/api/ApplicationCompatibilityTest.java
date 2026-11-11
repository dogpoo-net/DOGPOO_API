package net.dogpoo.api;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
 "spring.datasource.url=jdbc:mariadb://127.0.0.1:1/unused",
 "spring.datasource.username=unused", "spring.datasource.password=unused",
 "jwt.secret=smoke-test-only", "weaver.scheduling.enabled=false"
})
class ApplicationCompatibilityTest {
 @LocalServerPort int port;
 @Test void dispatcherAndSecurityChainRetainLegacyBehavior() throws Exception {
  try(var client=HttpClient.newHttpClient()) {
   var get=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/dogpoo/api/checkGet")).build(),HttpResponse.BodyHandlers.ofString());
   assertEquals(200,get.statusCode()); assertEquals("{\"result\":\"겟 통신 정상\"}",get.body());
   var post=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/dogpoo/api/checkPost")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());
   assertEquals(200,post.statusCode()); assertEquals("{\"result\":\"포스트 통신 정상\"}",post.body());
   var denied=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/dogpoo/private")).header("Authorization","Bearer invalid.token.value").build(),HttpResponse.BodyHandlers.ofString());
   assertEquals(401,denied.statusCode());
   var invalidSignup=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/dogpoo/weaver/api/auth/signup")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"invalid\"}")).build(),HttpResponse.BodyHandlers.ofString());
   assertEquals(400,invalidSignup.statusCode());
   assertEquals("no-store",invalidSignup.headers().firstValue("Cache-Control").orElse(""));
   var anonymousMe=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/dogpoo/weaver/api/auth/me")).build(),HttpResponse.BodyHandlers.ofString());
   assertEquals(401,anonymousMe.statusCode());

  }
 }
}
