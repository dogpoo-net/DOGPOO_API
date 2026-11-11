package net.dogpoo.api.core.weaver.mapper;

import net.dogpoo.api.core.weaver.model.WeaverAccount;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WeaverAuthMapper {
 @Select("SELECT id,email,password_hash AS passwordHash,verified_at AS verifiedAt FROM WEAVER_USER_ACCOUNT WHERE email=#{email}")
 WeaverAccount account(String email);
 @Select("SELECT id,email,password_hash AS passwordHash,verified_at AS verifiedAt FROM WEAVER_USER_ACCOUNT WHERE email=#{email} FOR UPDATE")
 WeaverAccount lockAccount(String email);
 @Insert("INSERT INTO WEAVER_USER_ACCOUNT(id,email,password_hash) VALUES(#{id},#{email},#{hash})")
 int insertAccount(@Param("id") String id,@Param("email") String email,@Param("hash") String hash);
 @Insert("INSERT INTO WEAVER_EMAIL_TOKEN(user_id,token_hash,expires_at,sent_at) VALUES(#{id},#{hash},DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 30 MINUTE),UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE token_hash=VALUES(token_hash),expires_at=VALUES(expires_at),sent_at=VALUES(sent_at)")
 int token(@Param("id") String id,@Param("hash") String hash);
 @Select("SELECT COUNT(*) FROM WEAVER_EMAIL_TOKEN WHERE user_id=#{id} AND sent_at > DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 60 SECOND)")
 int recentlySent(String id);
 @Update("UPDATE WEAVER_USER_ACCOUNT a JOIN WEAVER_EMAIL_TOKEN t ON t.user_id=a.id SET a.verified_at=UTC_TIMESTAMP(6) WHERE t.token_hash=#{hash} AND t.expires_at>UTC_TIMESTAMP(6) AND a.verified_at IS NULL")
 int verify(String hash);
 @Delete("DELETE FROM WEAVER_EMAIL_TOKEN WHERE token_hash=#{hash}")
 int consume(String hash);
 @Insert("INSERT INTO WEAVER_LOGIN_SESSION(token_hash,user_id,expires_at) VALUES(#{hash},#{id},DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 12 HOUR))")
 int session(@Param("hash") String hash,@Param("id") String id);
 @Select("SELECT a.id,a.email,a.password_hash AS passwordHash,a.verified_at AS verifiedAt FROM WEAVER_USER_ACCOUNT a JOIN WEAVER_LOGIN_SESSION s ON s.user_id=a.id WHERE s.token_hash=#{hash} AND s.expires_at>UTC_TIMESTAMP(6) AND a.verified_at IS NOT NULL")
 WeaverAccount sessionAccount(String hash);
 @Delete("DELETE FROM WEAVER_LOGIN_SESSION WHERE token_hash=#{hash}")
 int logout(String hash);
 @Delete("DELETE FROM WEAVER_LOGIN_SESSION WHERE expires_at<=UTC_TIMESTAMP(6)")
 int cleanSessions();
}
