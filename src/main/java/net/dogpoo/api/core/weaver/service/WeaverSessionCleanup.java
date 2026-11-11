package net.dogpoo.api.core.weaver.service;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import net.dogpoo.api.core.weaver.mapper.*;
@Service
public class WeaverSessionCleanup {
 private final WeaverSessionMapper sessions;
 private final WeaverOAuthMapper flows;
 public WeaverSessionCleanup(WeaverSessionMapper sessions,WeaverOAuthMapper flows){this.sessions=sessions;this.flows=flows;}
 @Scheduled(fixedDelayString="${weaver.auth.cleanup-delay-ms:3600000}",initialDelay=600000)
 public void expiredCredentials(){sessions.cleanupAccess();sessions.cleanupSessions();flows.cleanup();}
}
