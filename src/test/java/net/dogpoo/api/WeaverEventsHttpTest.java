package net.dogpoo.api;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:mariadb://127.0.0.1:1/unused",
 "spring.datasource.username=unused","spring.datasource.password=unused",
 "jwt.secret=smoke-test-only","weaver.scheduling.enabled=false"
})
class WeaverEventsHttpTest {
 @LocalServerPort int port;
 @MockitoBean WeaverSessionService sessions;
 @MockitoBean WeaverSharingService sharing;
 @Autowired WeaverWorkspaceEvents events;
 HttpRequest request(String body){return HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/dogpoo/weaver/api/auth/session/native/workspace/events"))
  .timeout(Duration.ofSeconds(10)).header("Authorization","Bearer test").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();}
 String event(BufferedReader reader)throws Exception{
  return CompletableFuture.supplyAsync(()->{try{String line;while((line=reader.readLine())!=null)if(line.startsWith("event:"))return line;return "eof";}catch(IOException e){throw new UncheckedIOException(e);}}).get(5,TimeUnit.SECONDS);
 }
 @Test void authenticatesAndStreamsBeforeClosingResponse()throws Exception{
  when(sessions.me("Bearer test")).thenReturn(Map.of("id","actor","email","a@example.invalid"));
  when(sharing.access("actor","workspace",false)).thenReturn(new WeaverSharingService.Access("workspace","a@example.invalid","viewer"));
  when(sharing.access("actor","other",false)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
  try(var client=HttpClient.newHttpClient()){
   var denied=client.send(request("{\"workspaceId\":\"other\"}"),HttpResponse.BodyHandlers.ofString());assertEquals(403,denied.statusCode());
   var response=client.send(request("{\"workspaceId\":\"workspace\"}"),HttpResponse.BodyHandlers.ofInputStream());
   assertEquals(200,response.statusCode());assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("text/event-stream"));assertEquals("no",response.headers().firstValue("x-accel-buffering").orElse(""));
   try(var reader=new BufferedReader(new InputStreamReader(response.body()))){
    assertEquals("event:ready",event(reader));
    events.changedAfterCommit("workspace");assertEquals("event:change",event(reader));
   }
  }
 }
}
