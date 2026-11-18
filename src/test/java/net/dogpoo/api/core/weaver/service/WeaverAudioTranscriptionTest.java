package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.net.http.*;import java.nio.file.*;
import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
class WeaverAudioTranscriptionTest {
 @Test void freeAccountCannotTranscribeEvenWhenProviderIsReady(){
  var db=mock(WeaverAudioMapper.class);var policy=mock(WeaverWorkspacePolicyService.class);
  when(policy.accountPlan("free-user")).thenReturn(Map.of("code","free"));
  var svc=new WeaverAudioTranscriptionService(db,policy,mock(WeaverPolicyMapper.class),"key","","/usr/bin/ffmpeg","/usr/bin/ffprobe");
  var error=assertThrows(ResponseStatusException.class,()->svc.transcribe("free-user","data"));assertEquals(403,error.getStatusCode().value());verifyNoInteractions(db);
 }

 @Test void durationsAndModelAllowlist(){assertEquals(60,WeaverAudioTranscriptionService.transcriptionSeconds(60,false));assertEquals(3600,WeaverAudioTranscriptionService.transcriptionSeconds(3600,true));assertThrows(ResponseStatusException.class,()->WeaverAudioTranscriptionService.transcriptionSeconds(61,false));assertThrows(ResponseStatusException.class,()->WeaverAudioTranscriptionService.transcriptionSeconds(3601,true));assertThrows(ResponseStatusException.class,()->WeaverAudioTranscriptionService.transcriptionSeconds(Double.NaN,true));assertTrue(WeaverAudioTranscriptionService.supported("openai","gpt-4o-mini-transcribe"));assertFalse(WeaverAudioTranscriptionService.supported("openai","gpt-4o-mini"));assertFalse(WeaverAudioTranscriptionService.supported("http://evil","anything"));}
 @Test void noKeyCannotActivateOrCall(){var db=mock(WeaverAudioMapper.class);var policy=mock(WeaverWorkspacePolicyService.class);when(policy.accountPlan(anyString())).thenReturn(Map.of("code","pro"));when(db.setting()).thenReturn(Map.of("provider","groq","model","whisper-large-v3-turbo","enabled",true));var svc=new WeaverAudioTranscriptionService(db,policy,mock(WeaverPolicyMapper.class),"","","ffmpeg","ffprobe");assertThrows(ResponseStatusException.class,()->svc.transcribe("w",""));assertThrows(ResponseStatusException.class,()->svc.configure("a","groq","whisper-large-v3-turbo",true));verify(db,never()).update(anyString(),anyString(),anyBoolean());}
 @Test void unauthorizedAdminCannotReadKeysOrChangeModels(){var db=mock(WeaverAudioMapper.class);var policy=mock(WeaverWorkspacePolicyService.class);when(policy.accountPlan(anyString())).thenReturn(Map.of("code","pro"));doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN)).when(policy).requireAdmin("user");var svc=new WeaverAudioTranscriptionService(db,policy,mock(WeaverPolicyMapper.class),"secret","secret","ffmpeg","ffprobe");assertThrows(ResponseStatusException.class,()->svc.settings("user"));assertThrows(ResponseStatusException.class,()->svc.configure("user","openai","gpt-4o-mini-transcribe",false));verifyNoInteractions(db);}
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"wav","webm"})
 void syntheticAudioIsConvertedInFullAndProviderCallIsMocked(String format)throws Exception{
  Assumptions.assumeTrue(Files.isExecutable(Path.of("/usr/bin/ffmpeg"))&&Files.isExecutable(Path.of("/usr/bin/ffprobe")));
  Path file=Files.createTempFile("weaver-test-audio-","."+format);
  try{
   var command=new ArrayList<String>(List.of("/usr/bin/ffmpeg","-v","error","-f","lavfi","-i","sine=frequency=440:duration=12"));
   if("webm".equals(format))command.addAll(List.of("-c:a","libopus","-f","webm","pipe:1"));else command.addAll(List.of("-y",file.toString()));
   var process=new ProcessBuilder(command);if("webm".equals(format))process.redirectOutput(file.toFile());
   var generator=process.start();assertEquals(0,generator.waitFor());
   var db=mock(WeaverAudioMapper.class);when(db.setting()).thenReturn(Map.of("provider","groq","model","whisper-large-v3-turbo","enabled",true));
   var policy=mock(WeaverWorkspacePolicyService.class);when(policy.accountPlan(anyString())).thenReturn(Map.of("code","pro"));var svc=new WeaverAudioTranscriptionService(db,policy,mock(WeaverPolicyMapper.class),"test-not-real","","/usr/bin/ffmpeg","/usr/bin/ffprobe");
   HttpClient client=mock(HttpClient.class);HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{\"text\":\"test transcript\"}");when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);ReflectionTestUtils.setField(svc,"client",client);
   var result=svc.transcribe("w",Base64.getEncoder().encodeToString(Files.readAllBytes(file)));assertTrue(((Number)result.get("transcribedSeconds")).doubleValue()>=12);assertEquals(false,result.get("truncated"));assertEquals("test transcript",result.get("text"));verify(client).send(argThat(request->request.uri().getHost().equals("api.groq.com")),any(HttpResponse.BodyHandler.class));
  }finally{Files.deleteIfExists(file);}
 }
}
