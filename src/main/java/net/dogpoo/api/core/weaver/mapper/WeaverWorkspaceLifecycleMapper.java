package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;import java.util.*;
@Mapper public interface WeaverWorkspaceLifecycleMapper {
 @Select("SELECT (revision>0 OR EXISTS(SELECT 1 FROM WEAVER_WORKSPACE_ITEM WHERE user_id=#{id}) OR EXISTS(SELECT 1 FROM WEAVER_WORKSPACE_MEDIA WHERE user_id=#{id}) OR EXISTS(SELECT 1 FROM WEAVER_WORKSPACE_INVITE WHERE workspace_id=#{id}) OR EXISTS(SELECT 1 FROM WEAVER_WORKSPACE_MEMBER WHERE workspace_id=#{id}) OR EXISTS(SELECT 1 FROM WEAVER_DOCUMENT_HISTORY WHERE workspace_id=#{id})) FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id}") boolean used(String id);
 @Select("SELECT COUNT(*) FROM WEAVER_WORKSPACE_DELETE_CHALLENGE WHERE workspace_id=#{id} AND sent_at>DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 60 SECOND)") int recent(String id);
 @Insert("INSERT INTO WEAVER_WORKSPACE_DELETE_CHALLENGE(workspace_id,owner_id,token_hash,expires_at,sent_at) VALUES(#{workspace},#{owner},#{hash},DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 10 MINUTE),UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE owner_id=VALUES(owner_id),token_hash=VALUES(token_hash),expires_at=VALUES(expires_at),sent_at=VALUES(sent_at),attempts=0") void issue(@Param("workspace")String workspace,@Param("owner")String owner,@Param("hash")String hash);
 @Update("UPDATE WEAVER_WORKSPACE_DELETE_CHALLENGE SET attempts=attempts+1 WHERE workspace_id=#{workspace} AND owner_id=#{owner} AND expires_at>UTC_TIMESTAMP(6) AND attempts<5") int attempt(@Param("workspace")String workspace,@Param("owner")String owner);
 @Select("SELECT COUNT(*) FROM WEAVER_WORKSPACE_DELETE_CHALLENGE WHERE workspace_id=#{workspace} AND owner_id=#{owner} AND token_hash=#{hash} AND expires_at>UTC_TIMESTAMP(6) AND attempts<=5") int valid(@Param("workspace")String workspace,@Param("owner")String owner,@Param("hash")String hash);
 @Delete("DELETE FROM WEAVER_WORKSPACE_DELETE_CHALLENGE WHERE workspace_id=#{id}") void clear(String id);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET archived_at=UTC_TIMESTAMP(6),purge_after=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 30 DAY),revision=revision+1 WHERE user_id=#{id} AND archived_at IS NULL") int archive(String id);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET archived_at=NULL,purge_after=NULL,revision=revision+1 WHERE user_id=#{id} AND archived_at IS NOT NULL AND purge_after>UTC_TIMESTAMP(6)") int restore(String id);
 @Select("SELECT user_id AS workspaceId,COALESCE(name,'워크스페이스') AS name,DATE_FORMAT(purge_after,'%Y-%m-%dT%H:%i:%s.%fZ') AS purgeAfter FROM WEAVER_ACCOUNT_WORKSPACE WHERE owner_id=#{id} AND archived_at IS NOT NULL AND purge_after>UTC_TIMESTAMP(6) ORDER BY archived_at DESC") List<Map<String,Object>> archived(String id);
 @Select("SELECT user_id FROM WEAVER_ACCOUNT_WORKSPACE WHERE archived_at IS NOT NULL AND purge_after<=UTC_TIMESTAMP(6) ORDER BY purge_after") List<String> expired();
 @Select("SELECT COUNT(*) FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id} AND archived_at IS NOT NULL AND purge_after<=UTC_TIMESTAMP(6)") int expiredOne(String id);
}
