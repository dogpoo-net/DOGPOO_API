package net.dogpoo.api.core.weaver.model;
import java.time.LocalDateTime;
public record WeaverAccount(String id,String email,String passwordHash,LocalDateTime verifiedAt,String displayName) {
 public WeaverAccount(String id,String email,String passwordHash,LocalDateTime verifiedAt){this(id,email,passwordHash,verifiedAt,null);}
}
