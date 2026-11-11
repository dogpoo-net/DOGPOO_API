package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;import java.util.*;
@Mapper public interface WeaverDocumentLockMapper {
 @Select("SELECT l.user_id AS userId,l.client_hash AS clientHash,a.display_name AS name FROM WEAVER_DOCUMENT_LOCK l JOIN WEAVER_USER_ACCOUNT a ON a.id=l.user_id WHERE l.workspace_id=#{workspace} AND l.document_key=#{key} AND l.expires_at>UTC_TIMESTAMP(6)") Map<String,Object> active(@Param("workspace") String workspace,@Param("key") String key);
 @Insert("INSERT INTO WEAVER_DOCUMENT_LOCK(workspace_id,document_key,user_id,client_hash,expires_at) VALUES(#{workspace},#{key},#{user},#{hash},DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 120 SECOND)) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id),client_hash=VALUES(client_hash),expires_at=VALUES(expires_at)") void renew(@Param("workspace") String workspace,@Param("key") String key,@Param("user") String user,@Param("hash") String hash);
 @Delete("DELETE FROM WEAVER_DOCUMENT_LOCK WHERE workspace_id=#{workspace} AND document_key=#{key} AND user_id=#{user} AND client_hash=#{hash}") void release(@Param("workspace") String workspace,@Param("key") String key,@Param("user") String user,@Param("hash") String hash);
}
