package net.dogpoo.api.core.weaver.service;
import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.Scheduled;
import net.dogpoo.api.core.weaver.mapper.WeaverWorkspaceLifecycleMapper;
@Component public class WeaverWorkspaceRetentionJob {
 private final WeaverWorkspaceLifecycleMapper mapper;private final WeaverWorkspaceLifecycleService lifecycle;
 public WeaverWorkspaceRetentionJob(WeaverWorkspaceLifecycleMapper mapper,WeaverWorkspaceLifecycleService lifecycle){this.mapper=mapper;this.lifecycle=lifecycle;}
 @Scheduled(fixedDelayString="${weaver.retention.delay-ms:60000}",initialDelay=30000)
 public void purge(){for(String id:mapper.expired())try{lifecycle.purge(id);}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("WEAVER workspace purge will retry");}}
}
