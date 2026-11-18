package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.net.*;import java.net.http.*;import java.time.Duration;import java.util.concurrent.*;
import org.springframework.stereotype.Service;import org.springframework.beans.factory.annotation.Value;import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dogpoo.api.core.weaver.mapper.*;
@Service public class WeaverAudioTranscriptionService {
 private final WeaverAudioMapper mapper;private final WeaverWorkspacePolicyService policy;private final WeaverPolicyMapper audit;
 private final String groqKey,openaiKey,ffmpeg,ffprobe;private final Semaphore slots=new Semaphore(2);
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();
 public WeaverAudioTranscriptionService(WeaverAudioMapper mapper,WeaverWorkspacePolicyService policy,WeaverPolicyMapper audit,@Value("${GROQ_API_KEY:}")String groqKey,@Value("${OPENAI_API_KEY:}")String openaiKey,@Value("${WEAVER_FFMPEG:ffmpeg}")String ffmpeg,@Value("${WEAVER_FFPROBE:ffprobe}")String ffprobe){this.mapper=mapper;this.policy=policy;this.audit=audit;this.groqKey=groqKey;this.openaiKey=openaiKey;this.ffmpeg=ffmpeg;this.ffprobe=ffprobe;}
 public static boolean supported(String provider,String model){return "groq".equals(provider)&&Set.of("whisper-large-v3-turbo","whisper-large-v3").contains(model==null?"":model)||"openai".equals(provider)&&"gpt-4o-mini-transcribe".equals(model);}
 private String key(String provider){return "groq".equals(provider)?groqKey:openaiKey;}
 private boolean executable(String name){if(name.contains("/"))return Files.isExecutable(Path.of(name));for(String dir:System.getenv().getOrDefault("PATH","").split(java.io.File.pathSeparator))if(Files.isExecutable(Path.of(dir,name)))return true;return false;}
 public Map<String,Object> settings(String actor){policy.requireAdmin(actor);var result=new HashMap<>(mapper.setting());result.put("runtimeReady",executable(ffmpeg)&&executable(ffprobe));result.put("groqConfigured",!groqKey.isBlank());result.put("openaiConfigured",!openaiKey.isBlank());return result;}
 @Transactional public Map<String,Object> configure(String actor,String provider,String model,boolean enabled){policy.requireAdmin(actor);if(!supported(provider,model))throw WeaverWorkspacePolicyService.deny("지원하지 않는 음성 모델입니다.");if(enabled&&key(provider).isBlank())throw WeaverWorkspacePolicyService.deny("선택한 제공자의 API 키를 서버에 먼저 설정해주세요.");if(enabled&&(!executable(ffmpeg)||!executable(ffprobe)))throw WeaverWorkspacePolicyService.deny("서버의 ffmpeg / ffprobe를 설정해주세요.");var before=mapper.setting();mapper.update(provider,model,enabled);audit.audit(actor,"audio_provider","1",before+" -> "+mapper.setting());return settings(actor);}
 public static double transcriptionSeconds(double duration,boolean paid){if(!Double.isFinite(duration)||duration<=0||duration>(paid?3600:60)+.5)throw WeaverWorkspacePolicyService.deny(paid?"녹음은 최대 1시간입니다.":"무료 녹음은 최대 1분입니다.");return duration;}
 public Map<String,Object> availability(String account){
  var config=mapper.setting();String provider=(String)config.get("provider");Object enabled=config.get("enabled");
  boolean active=(Boolean.TRUE.equals(enabled)||enabled instanceof Number n&&n.intValue()==1)&&!key(provider).isBlank()&&executable(ffmpeg)&&executable(ffprobe);
  boolean paid=Set.of("standard","pro").contains(policy.accountPlan(account).get("code"));
  return Map.of("enabled",active&&paid,"allowed",paid,"maxRecordingSeconds",paid?3600:60,"maxTranscriptionSeconds",paid?3600:0);
 }
 public Map<String,Object> transcribe(String account,String data){
  if(!Set.of("standard","pro").contains(policy.accountPlan(account).get("code")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"AI 음성 변환은 구독 중인 계정에서 사용할 수 있습니다.");
  var config=mapper.setting();String provider=(String)config.get("provider"),model=(String)config.get("model");Object active=config.get("enabled");
  if(!(Boolean.TRUE.equals(active)||active instanceof Number n&&n.intValue()==1)||key(provider).isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"음성 변환이 아직 활성화되지 않았습니다.");
  if(!supported(provider,model))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"음성 모델 설정을 확인해주세요.");
  if(data==null||data.length()>48*1024*1024)throw WeaverWorkspacePolicyService.deny("음성 업로드 한도는 32MiB입니다.");
  byte[] input;try{input=Base64.getDecoder().decode(data);}catch(IllegalArgumentException e){throw WeaverWorkspacePolicyService.deny("음성 파일 형식이 올바르지 않습니다.");}
  if(input.length==0||input.length>32*1024*1024)throw WeaverWorkspacePolicyService.deny("음성 업로드 한도는 32MiB입니다.");
  if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"음성 변환이 진행 중입니다. 잠시 후 다시 시도해주세요.");
  Path dir=null;
  try{
   dir=Files.createTempDirectory("weaver-transcribe-");Path source=dir.resolve("input"),output=dir.resolve("clip.mp3"),probe=dir.resolve("duration"),log=dir.resolve("process.log");Files.write(source,input);
   // Browser WebM recordings may omit container duration. Decode to a bounded
   // MP3 first, then measure actual audio rather than trusting client metadata.
   boolean paid=true;Path decoded=dir.resolve("decoded.mp3");
   run(List.of(ffmpeg,"-v","error","-nostdin","-protocol_whitelist","file,pipe","-format_whitelist","mp3,wav,ogg,matroska,webm,mov,flac,aac","-i",source.toString(),"-t","3601","-vn","-ac","1","-ar","16000","-c:a","libmp3lame","-b:a","32k","-y",decoded.toString()),dir.resolve("stdout"),log,180);
   run(List.of(ffprobe,"-v","error","-show_entries","format=duration","-of","default=noprint_wrappers=1:nokey=1",decoded.toString()),probe,log,30);
   double duration;try{duration=Double.parseDouble(Files.readString(probe).strip());}catch(Exception e){throw WeaverWorkspacePolicyService.deny("녹음 길이를 확인할 수 없는 파일입니다.");}
   double seconds=transcriptionSeconds(duration,paid);
   Files.move(decoded,output);
   byte[] audio=Files.readAllBytes(output);if(audio.length>24*1024*1024)throw WeaverWorkspacePolicyService.deny("변환된 음성 파일이 너무 큽니다.");
   String boundary="weaver-"+UUID.randomUUID();var body=new java.io.ByteArrayOutputStream();
   body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\n"+model+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"response_format\"\r\n\r\njson\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"clip.mp3\"\r\nContent-Type: audio/mpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));body.write(audio);body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
   String endpoint="groq".equals(provider)?"https://api.groq.com/openai/v1/audio/transcriptions":"https://api.openai.com/v1/audio/transcriptions";
   var request=HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(180)).header("Authorization","Bearer "+key(provider)).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
   var response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
   if(response.statusCode()<200||response.statusCode()>=300)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"음성 인식 제공자가 요청을 처리하지 못했습니다. ("+response.statusCode()+")");
   var text=new ObjectMapper().readTree(response.body()).path("text");if(!text.isTextual())throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"음성 인식 응답을 확인할 수 없습니다.");
   return Map.of("text",text.asText(),"provider",provider,"model",model,"durationSeconds",duration,"transcribedSeconds",seconds,"truncated",seconds<duration);
  }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"음성 변환이 중단되었습니다.");}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"음성 변환을 완료하지 못했습니다. 서버 도구와 제공자 설정을 확인해주세요.");}
  finally{if(dir!=null)try(var paths=Files.walk(dir)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}catch(Exception ignored){}slots.release();}
 }
 private static void run(List<String> command,Path stdout,Path stderr,int timeout)throws Exception{Process p=new ProcessBuilder(command).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();try{if(!p.waitFor(timeout,TimeUnit.SECONDS)){p.destroyForcibly();p.waitFor();throw new java.io.IOException("Audio process timeout");}if(p.exitValue()!=0)throw WeaverWorkspacePolicyService.deny("지원하지 않거나 손상된 음성 파일입니다.");}finally{if(p.isAlive())p.destroyForcibly();}}
}
