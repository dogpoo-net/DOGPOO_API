package net.dogpoo.api.core.weaver.model;
import java.time.LocalDateTime;
public record WeaverOAuthFlow(String idHash,String stateHash,String proofHash,String platform,String provider,String status,String linkUserId,
 String subject,String email,String displayName,Integer errorCode,LocalDateTime expiresAt) {}
