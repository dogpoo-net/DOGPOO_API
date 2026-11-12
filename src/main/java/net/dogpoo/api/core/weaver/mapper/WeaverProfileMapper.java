package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;
import net.dogpoo.api.core.weaver.model.WeaverProfile;
import java.util.List;
@Mapper public interface WeaverProfileMapper {
 @Insert("INSERT IGNORE INTO WEAVER_ACCOUNT_PROFILE(user_id,default_face) VALUES(#{id},FLOOR(RAND()*6))") int ensure(String id);
 @Select("SELECT avatar_url AS avatarUrl,avatar_path AS avatarPath,email_enabled AS emailEnabled,default_face AS defaultFace FROM WEAVER_ACCOUNT_PROFILE WHERE user_id=#{id} FOR UPDATE") WeaverProfile lock(String id);
 @Select("SELECT avatar_url AS avatarUrl,avatar_path AS avatarPath,email_enabled AS emailEnabled,default_face AS defaultFace FROM WEAVER_ACCOUNT_PROFILE WHERE user_id=#{id}") WeaverProfile get(String id);
 @Select("SELECT provider FROM WEAVER_SOCIAL_ACCOUNT WHERE user_id=#{id} ORDER BY provider") List<String> providers(String id);
 @Update("UPDATE WEAVER_ACCOUNT_PROFILE SET email_enabled=#{enabled} WHERE user_id=#{id}") int email(@Param("id") String id,@Param("enabled") boolean enabled);
 @Update("UPDATE WEAVER_ACCOUNT_PROFILE SET avatar_url=#{url} WHERE user_id=#{id} AND avatar_path IS NULL") int socialAvatar(@Param("id") String id,@Param("url") String url);
 @Update("UPDATE WEAVER_ACCOUNT_PROFILE SET avatar_path=#{path} WHERE user_id=#{id}") int avatar(@Param("id") String id,@Param("path") String path);
 @Update("UPDATE WEAVER_USER_ACCOUNT SET display_name=#{name} WHERE id=#{id}") int name(@Param("id") String id,@Param("name") String name);
 @Select("SELECT COUNT(*) FROM WEAVER_PASSWORD_CHALLENGE WHERE user_id=#{id} AND sent_at>DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 60 SECOND)") int recent(String id);
 @Insert("INSERT INTO WEAVER_PASSWORD_CHALLENGE(user_id,token_hash,expires_at,sent_at) VALUES(#{id},#{hash},DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 10 MINUTE),UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE token_hash=VALUES(token_hash),expires_at=VALUES(expires_at),sent_at=VALUES(sent_at),attempts=0") int challenge(@Param("id") String id,@Param("hash") String hash);
 @Update("UPDATE WEAVER_PASSWORD_CHALLENGE SET attempts=attempts+1 WHERE user_id=#{id} AND expires_at>UTC_TIMESTAMP(6) AND attempts<5") int attempt(String id);
 @Delete("DELETE FROM WEAVER_PASSWORD_CHALLENGE WHERE user_id=#{id} AND token_hash=#{hash} AND expires_at>UTC_TIMESTAMP(6) AND attempts<=5") int consume(@Param("id") String id,@Param("hash") String hash);
 @Update("UPDATE WEAVER_USER_ACCOUNT SET password_hash=#{hash} WHERE id=#{id}") int password(@Param("id") String id,@Param("hash") String hash);
}
