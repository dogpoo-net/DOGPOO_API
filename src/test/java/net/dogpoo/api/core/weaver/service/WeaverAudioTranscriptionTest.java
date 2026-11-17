package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.net.http.*;import java.nio.file.*;
import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
class WeaverAudioTranscriptionTest {
 @Test void durationsAndModelAllowlist(){assertEquals(10,WeaverAudioTranscriptionService.transcriptionSeconds(60,false));assertEquals(3600,WeaverAudioTranscriptionService.transcriptionSeconds(3600,true));assertThrows(ResponseStatusException.class,()->WeaverAudioTranscriptionService.transcriptionSeconds(61,false));assertThrows(ResponseStatusException.class,()->WeaverAudioTranscriptionService.transcriptionSeconds(3601,true));assertThrows(ResponseStatusException.class,()->WeaverAudioTranscriptionService.transcriptionSeconds(Double.NaN,true));assertTrue(WeaverAudioTranscriptionService.supported("openai","gpt-4o-mini-transcribe"));assertFalse(WeaverAudioTranscriptionService.supported("openai","gpt-4o-mini"));assertFalse(WeaverAudioTranscriptionService.supported("http://evil","anything"));}
 @Test void noKeyCannotActivateOrCall(){var db=mock(WeaverAudioMapper.class);var policy=mock(WeaverWorkspacePolicyService.class);when(db.setting()).thenReturn(Map.of("provider","groq","model","whisper-large-v3-turbo","enabled",true));var svc=new WeaverAudioTranscriptionService(db,policy,mock(WeaverPolicyMapper.class),"","","ffmpeg","ffprobe");assertThrows(ResponseStatusException.class,()->svc.transcribe("w",""));assertThrows(ResponseStatusException.class,()->svc.configure("a","groq","whisper-large-v3-turbo",true));verify(db,never()).update(anyString(),anyString(),anyBoolean());}
 @Test void unauthorizedAdminCannotReadKeysOrChangeModels(){var db=mock(WeaverAudioMapper.class);var policy=mock(WeaverWorkspacePolicyService.class);doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN)).when(policy).requireAdmin("user");var svc=new WeaverAudioTranscriptionService(db,policy,mock(WeaverPolicyMapper.class),"secret","secret","ffmpeg","ffprobe");assertThrows(ResponseStatusException.class,()->svc.settings("user"));assertThrows(ResponseStatusException.class,()->svc.configure("user","openai","gpt-4o-mini-transcribe",false));verifyNoInteractions(db);}
 @Test void syntheticAudioIsClippedLocallyAndProviderCallIsMocked()throws Exception{
  Assumptions.assumeTrue(Files.isExecutable(Path.of("/usr/bin/ffmpeg"))&&Files.isExecutable(Path.of("/usr/bin/ffprobe")));
  Path file=Files.createTempFile("weaver-test-audio-",".wav");
  try{
   var generator=new ProcessBuilder("/usr/bin/ffmpeg","-v","error","-f","lavfi","-i","sine=frequency=440:duration=12","-y",file.toString()).start();assertEquals(0,generator.waitFor());
   var db=mock(WeaverAudioMapper.class);when(db.setting()).thenReturn(Map.of("provider","groq","model","whisper-large-v3-turbo","enabled",true));
   var policy=mock(WeaverWorkspacePolicyService.class);var svc=new WeaverAudioTranscriptionService(db,policy,mock(WeaverPolicyMapper.class),"test-not-real","","/usr/bin/ffmpeg","/usr/bin/ffprobe");
   HttpClient client=mock(HttpClient.class);HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{\"text\":\"test transcript\"}");when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);ReflectionTestUtils.setField(svc,"client",client);
   var result=svc.transcribe("w",Base64.getEncoder().encodeToString(Files.readAllBytes(file)));assertEquals(10.0,result.get("transcribedSeconds"));assertEquals(true,result.get("truncated"));assertEquals("test transcript",result.get("text"));verify(client).send(argThat(request->request.uri().getHost().equals("api.groq.com")),any(HttpResponse.BodyHandler.class));
  }finally{Files.deleteIfExists(file);}
 }
}
