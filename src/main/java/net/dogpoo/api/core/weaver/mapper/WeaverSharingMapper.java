package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;import java.util.*;
@Mapper public interface WeaverSharingMapper {
 @Select("SELECT a.email FROM WEAVER_USER_ACCOUNT a WHERE a.verified_at IS NOT NULL AND a.id != #{user} AND NOT EXISTS (SELECT 1 FROM WEAVER_WORKSPACE_MEMBER m WHERE m.workspace_id=#{workspace} AND m.user_id=a.id) AND a.email LIKE CONCAT(#{prefix},'%') ESCAPE '!' ORDER BY a.email ASC LIMIT 5") java.util.List<String> emailSuggestions(@Param("prefix") String prefix,@Param("workspace") String workspace,@Param("user") String user);
 @Select("SELECT EXISTS(SELECT 1 FROM WEAVER_USER_ACCOUNT WHERE email=#{email} AND verified_at IS NOT NULL)") boolean registeredEmail(String email);
 @Select("SELECT m.workspace_id AS workspaceId,m.role,COALESCE(w.name,'내 워크스페이스') AS name,COALESCE(NULLIF(a.display_name,''),SUBSTRING_INDEX(a.email,'@',1)) AS ownerName FROM WEAVER_WORKSPACE_MEMBER m JOIN WEAVER_ACCOUNT_WORKSPACE w ON w.user_id=m.workspace_id JOIN WEAVER_USER_ACCOUNT a ON a.id=w.owner_id WHERE m.user_id=#{id} AND w.archived_at IS NULL ORDER BY m.joined_at") List<Map<String,Object>> memberships(String id);
 @Select("SELECT role FROM WEAVER_WORKSPACE_MEMBER WHERE workspace_id=#{workspace} AND user_id=#{user}") String role(@Param("workspace") String workspace,@Param("user") String user);
 @Select("SELECT m.user_id AS userId,m.role,a.email,a.display_name AS name FROM WEAVER_WORKSPACE_MEMBER m JOIN WEAVER_USER_ACCOUNT a ON a.id=m.user_id WHERE m.workspace_id=#{id} ORDER BY m.joined_at") List<Map<String,Object>> members(String id);
 @Select("SELECT COUNT(*) FROM WEAVER_WORKSPACE_MEMBER WHERE workspace_id=#{id} AND role='editor'") int editors(String id);
 @Select("SELECT COUNT(*) FROM WEAVER_WORKSPACE_INVITE WHERE workspace_id=#{id} AND role='editor' AND status='pending' AND expires_at>UTC_TIMESTAMP(6)") int reserved(String id);
 @Select("SELECT id,workspace_id AS workspaceId,email,role,status,expires_at AS expiresAt FROM WEAVER_WORKSPACE_INVITE WHERE workspace_id=#{id} AND status='pending' AND expires_at>UTC_TIMESTAMP(6) ORDER BY created_at") List<Map<String,Object>> invitations(String id);
 @Select("SELECT i.id,i.workspace_id AS workspaceId,i.role,COALESCE(w.name,'내 워크스페이스') AS name FROM WEAVER_WORKSPACE_INVITE i JOIN WEAVER_ACCOUNT_WORKSPACE w ON w.user_id=i.workspace_id JOIN WEAVER_USER_ACCOUNT a ON a.id=w.owner_id WHERE w.archived_at IS NULL AND i.email=#{email} AND i.status='pending' AND i.expires_at>UTC_TIMESTAMP(6) ORDER BY i.created_at") List<Map<String,Object>> received(String email);
 @Select("SELECT id,workspace_id AS workspaceId,email,role,status,expires_at>UTC_TIMESTAMP(6) AS valid FROM WEAVER_WORKSPACE_INVITE WHERE id=#{id}") Map<String,Object> invite(String id);
 @Select("SELECT id FROM WEAVER_WORKSPACE_INVITE WHERE token_hash=#{hash}") String token(String hash);
 @Insert("INSERT INTO WEAVER_WORKSPACE_INVITE(id,workspace_id,email,role,token_hash,expires_at) VALUES(#{id},#{workspace},#{email},#{role},#{hash},DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 7 DAY))") void create(@Param("id") String id,@Param("workspace") String workspace,@Param("email") String email,@Param("role") String role,@Param("hash") String hash);
 @Update("UPDATE WEAVER_WORKSPACE_INVITE SET status=#{status} WHERE id=#{id}") void status(@Param("id") String id,@Param("status") String status);
 @Insert("INSERT INTO WEAVER_WORKSPACE_MEMBER(workspace_id,user_id,role) VALUES(#{workspace},#{user},#{role})") void join(@Param("workspace") String workspace,@Param("user") String user,@Param("role") String role);
 @Delete("DELETE FROM WEAVER_WORKSPACE_MEMBER WHERE workspace_id=#{workspace} AND user_id=#{user}") void remove(@Param("workspace") String workspace,@Param("user") String user);
 @Update("UPDATE WEAVER_WORKSPACE_MEMBER SET role=#{role} WHERE workspace_id=#{workspace} AND user_id=#{user}") int setRole(@Param("workspace") String workspace,@Param("user") String user,@Param("role") String role);
}
