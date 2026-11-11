package net.dogpoo.api.core.weaver.model;
import java.time.LocalDateTime;
public record WeaverDeviceSession(String id,String userId,String kind,String currentHash,String previousHash,
 String proofHash,String lastRequestHash,LocalDateTime createdAt,LocalDateTime lastUsedAt,LocalDateTime idleExpiresAt,LocalDateTime absoluteExpiresAt) {}
