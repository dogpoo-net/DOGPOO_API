package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;
import net.dogpoo.api.core.weaver.model.WeaverAccount;
@Mapper
public interface WeaverSocialMapper {
 @Select("SELECT a.id,a.email,a.password_hash AS passwordHash,a.verified_at AS verifiedAt,a.display_name AS displayName FROM WEAVER_USER_ACCOUNT a JOIN WEAVER_SOCIAL_ACCOUNT s ON s.user_id=a.id WHERE s.provider=#{provider} AND s.subject=#{subject}")
 WeaverAccount find(@Param("provider") String provider,@Param("subject") String subject);
 @Insert("INSERT INTO WEAVER_SOCIAL_ACCOUNT(provider,subject,user_id) VALUES(#{provider},#{subject},#{id})")
 int link(@Param("provider") String provider,@Param("subject") String subject,@Param("id") String id);
 @Update("UPDATE WEAVER_USER_ACCOUNT SET verified_at=UTC_TIMESTAMP(6) WHERE id=#{id}")
 int verified(String id);
}
