package net.dogpoo.api.core.weaver.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Per-process abuse protection. Use a shared edge limiter before scaling instances. */
@Service
public class WeaverAuthRateLimit {
 private record Window(long until,int count) {}
 private final Map<String,Window> windows=new HashMap<>();
 private final Clock clock;
 public WeaverAuthRateLimit(){this(Clock.systemUTC());}
 WeaverAuthRateLimit(Clock clock){this.clock=clock;}
 public synchronized void check(String key,int maximum) {
  long now=clock.millis();
  windows.entrySet().removeIf(e->e.getValue().until<=now);
  Window w=windows.get(key);
  if(w==null) {
   if(windows.size()>=10000) throw limited();
   w=new Window(now+60000,0);
  }
  if(w.count>=maximum) throw limited();
  windows.put(key,new Window(w.until,w.count+1));
 }
 private ResponseStatusException limited(){return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"요청이 많습니다. 1분 뒤 다시 시도해주세요.");}
}
