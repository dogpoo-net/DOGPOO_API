package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;import java.util.*;
@Mapper public interface WeaverWithdrawalMapper {
 @Select("SELECT COUNT(*) FROM WEAVER_WITHDRAWAL_CHALLENGE WHERE user_id=#{id} AND sent_at>DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 60 SECOND)") int recent(String id);
 @Insert("INSERT INTO WEAVER_WITHDRAWAL_CHALLENGE(user_id,token_hash,expires_at,sent_at) VALUES(#{id},#{hash},DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 10 MINUTE),UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE token_hash=VALUES(token_hash),expires_at=VALUES(expires_at),sent_at=VALUES(sent_at),attempts=0") void challenge(@Param("id")String id,@Param("hash")String hash);
 @Update("UPDATE WEAVER_WITHDRAWAL_CHALLENGE SET attempts=attempts+1 WHERE user_id=#{id} AND expires_at>UTC_TIMESTAMP(6) AND attempts<5") int attempt(String id);
 @Select("SELECT COUNT(*) FROM WEAVER_WITHDRAWAL_CHALLENGE WHERE user_id=#{id} AND token_hash=#{hash} AND expires_at>UTC_TIMESTAMP(6) AND attempts<=5") int verify(@Param("id")String id,@Param("hash")String hash);
 @Delete("DELETE FROM WEAVER_USER_ACCOUNT WHERE id=#{id}") int deleteAccount(String id);
 @Delete("DELETE FROM WEAVER_TEST_DATA WHERE account=#{email}") void deleteLegacy(String email);
 @Delete("DELETE FROM WEAVER_OAUTH_FLOW WHERE email=#{email} OR link_user_id=#{id}") void deleteFlows(@Param("id")String id,@Param("email")String email);
 @Delete("DELETE FROM WEAVER_WORKSPACE_INVITE WHERE email=#{email}") void deleteReceived(String email);
 @Insert("INSERT IGNORE INTO WEAVER_ACCOUNT_ERASURE(resource_id,resource_kind) VALUES(#{id},#{kind})") void enqueue(@Param("id")String id,@Param("kind")String kind);
 @Select("SELECT resource_id AS id,resource_kind AS kind FROM WEAVER_ACCOUNT_ERASURE ORDER BY queued_at LIMIT 100") List<Map<String,String>> pending();
 @Delete("DELETE FROM WEAVER_ACCOUNT_ERASURE WHERE resource_id=#{id} AND resource_kind=#{kind}") void complete(@Param("id")String id,@Param("kind")String kind);
}
