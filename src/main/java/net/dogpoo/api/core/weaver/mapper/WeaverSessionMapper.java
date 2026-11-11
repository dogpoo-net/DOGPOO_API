package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;
import net.dogpoo.api.core.weaver.model.*;
import java.time.LocalDateTime;
@Mapper
public interface WeaverSessionMapper {
 String COLUMNS="id,user_id AS userId,kind,current_hash AS currentHash,previous_hash AS previousHash,proof_hash AS proofHash,last_request_hash AS lastRequestHash,created_at AS createdAt,last_used_at AS lastUsedAt,idle_expires_at AS idleExpiresAt,absolute_expires_at AS absoluteExpiresAt";
 @Insert("INSERT INTO WEAVER_DEVICE_SESSION(id,user_id,kind,current_hash,proof_hash,created_at,last_used_at,idle_expires_at,absolute_expires_at) VALUES(#{id},#{userId},#{kind},#{currentHash},#{proofHash},#{createdAt},#{lastUsedAt},#{idleExpiresAt},#{absoluteExpiresAt})")
 int insert(WeaverDeviceSession session);
 @Select("SELECT "+COLUMNS+" FROM WEAVER_DEVICE_SESSION WHERE id=#{id} FOR UPDATE")
 WeaverDeviceSession lock(String id);
 @Select("SELECT "+COLUMNS+" FROM WEAVER_DEVICE_SESSION WHERE id=(SELECT device_id FROM WEAVER_ACCESS_TOKEN WHERE token_hash=#{hash} AND expires_at>#{now}) FOR UPDATE")
 WeaverDeviceSession access(@Param("hash") String hash,@Param("now") LocalDateTime now);
 @Select("SELECT id,email,password_hash AS passwordHash,verified_at AS verifiedAt,display_name AS displayName FROM WEAVER_USER_ACCOUNT WHERE id=#{id}")
 WeaverAccount account(String id);
 @Update("UPDATE WEAVER_DEVICE_SESSION SET previous_hash=current_hash,current_hash=#{hash},last_request_hash=#{requestHash},last_used_at=#{now},idle_expires_at=#{idle} WHERE id=#{id}")
 int rotate(@Param("id") String id,@Param("hash") String hash,@Param("requestHash") String requestHash,@Param("now") LocalDateTime now,@Param("idle") LocalDateTime idle);
 @Update("UPDATE WEAVER_DEVICE_SESSION SET last_used_at=#{now},idle_expires_at=#{idle} WHERE id=#{id}")
 int touch(@Param("id") String id,@Param("now") LocalDateTime now,@Param("idle") LocalDateTime idle);
 @Insert("INSERT INTO WEAVER_ACCESS_TOKEN(token_hash,device_id,expires_at) VALUES(#{hash},#{id},#{expires})")
 int issueAccess(@Param("hash") String hash,@Param("id") String id,@Param("expires") LocalDateTime expires);
 @Delete("DELETE FROM WEAVER_ACCESS_TOKEN WHERE device_id=#{id}") int clearAccess(String id);
 @Delete("DELETE FROM WEAVER_DEVICE_SESSION WHERE id=#{id}") int revoke(String id);
 @Delete("DELETE FROM WEAVER_DEVICE_SESSION WHERE user_id=#{id}") int revokeAll(String id);
 @Delete("DELETE FROM WEAVER_DEVICE_SESSION WHERE idle_expires_at<=UTC_TIMESTAMP(6) OR absolute_expires_at<=UTC_TIMESTAMP(6)") int cleanupSessions();
 @Delete("DELETE FROM WEAVER_ACCESS_TOKEN WHERE expires_at<=UTC_TIMESTAMP(6)") int cleanupAccess();
 @Delete("DELETE FROM WEAVER_LOGIN_SESSION WHERE user_id=#{id}") int revokeLegacy(String id);
}
