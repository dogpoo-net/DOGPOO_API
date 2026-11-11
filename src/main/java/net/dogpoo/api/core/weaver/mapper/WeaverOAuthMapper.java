package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;
import net.dogpoo.api.core.weaver.model.*;
@Mapper
public interface WeaverOAuthMapper {
 String COLUMNS="id_hash AS idHash,state_hash AS stateHash,proof_hash AS proofHash,platform,provider,status,link_user_id AS linkUserId,subject,email,display_name AS displayName,error_code AS errorCode,expires_at AS expiresAt";
 @Insert("INSERT INTO WEAVER_OAUTH_FLOW(id_hash,state_hash,proof_hash,platform,provider,link_user_id,expires_at) VALUES(#{id},#{state},#{proof},#{platform},#{provider},#{link},DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 10 MINUTE))")
 int insert(@Param("id") String id,@Param("state") String state,@Param("proof") String proof,@Param("platform") String platform,@Param("provider") String provider,@Param("link") String link);
 @Select("SELECT "+COLUMNS+" FROM WEAVER_OAUTH_FLOW WHERE id_hash=#{id} AND expires_at>UTC_TIMESTAMP(6) FOR UPDATE")
 WeaverOAuthFlow lock(String id);
 @Update("UPDATE WEAVER_OAUTH_FLOW SET status='exchanging' WHERE state_hash=#{state} AND provider=#{provider} AND status='pending' AND expires_at>UTC_TIMESTAMP(6)")
 int claim(@Param("state") String state,@Param("provider") String provider);
 @Update("UPDATE WEAVER_OAUTH_FLOW SET status='ready',subject=#{profile.subject},email=#{profile.email},display_name=#{profile.name} WHERE state_hash=#{state} AND status='exchanging'")
 int complete(@Param("state") String state,@Param("profile") WeaverSocialIdentity profile);
 @Update("UPDATE WEAVER_OAUTH_FLOW SET status='failed',error_code=#{code} WHERE state_hash=#{state}")
 int fail(@Param("state") String state,@Param("code") int code);
 @Delete("DELETE FROM WEAVER_OAUTH_FLOW WHERE id_hash=#{id}") int consume(String id);
 @Delete("DELETE FROM WEAVER_OAUTH_FLOW WHERE expires_at<=UTC_TIMESTAMP(6)") int cleanup();
}
