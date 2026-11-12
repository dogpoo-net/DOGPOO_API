package net.dogpoo.api.core.weaver.service;
import java.nio.file.*;import java.io.*;import java.util.*;
import org.springframework.stereotype.Service;import org.springframework.beans.factory.annotation.Value;import org.springframework.scheduling.annotation.Scheduled;
import net.dogpoo.api.core.weaver.mapper.WeaverWithdrawalMapper;
@Service public class WeaverErasureService {
 private final WeaverWithdrawalMapper mapper;private final Path root;
 public WeaverErasureService(WeaverWithdrawalMapper mapper,@Value("${WEAVER_RESOURCE_DIR:/home/ocaptain/resource}")String root){this.mapper=mapper;this.root=Path.of(root).toAbsolutePath().normalize();}
 @Scheduled(fixedDelayString="${weaver.erasure.delay-ms:60000}",initialDelay=30000)
 public synchronized void drain(){
  for(var job:mapper.pending())try{
   String id=job.get("id");if(id==null || !id.matches("[a-f0-9-]{36}"))continue;
   if("workspace".equals(job.get("kind"))){remove(root.resolve("image").resolve(id));remove(root.resolve("audio").resolve(id));}
   else if("profile".equals(job.get("kind")))remove(root.resolve("profile/images").resolve(id));else continue;
   mapper.complete(id,job.get("kind"));
  }catch(IOException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("WEAVER file erasure will be retried");}
 }
 static void remove(Path path)throws IOException{
  if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return;
  try(var files=Files.walk(path)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}
 }
}
