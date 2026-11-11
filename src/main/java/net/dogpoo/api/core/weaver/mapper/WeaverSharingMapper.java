package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;import java.util.*;
@Mapper public interface WeaverSharingMapper {
 @Select("SELECT m.workspace_id AS workspaceId,m.role,a.display_name AS name FROM WEAVER_WORKSPACE_MEMBER m JOIN WEAVER_USER_ACCOUNT a ON a.id=m.workspace_id WHERE m.user_id=#{id} ORDER BY m.joined_at") List<Map<String,Object>> memberships(String id);
 @Select("SELECT role FROM WEAVER_WORKSPACE_MEMBER WHERE workspace_id=#{workspace} AND user_id=#{user}") String role(@Param("workspace") String workspace,@Param("user") String user);
 @Select("SELECT m.user_id AS userId,m.role,a.email,a.display_name AS name FROM WEAVER_WORKSPACE_MEMBER m JOIN WEAVER_USER_ACCOUNT a ON a.id=m.user_id WHERE m.workspace_id=#{id} ORDER BY m.joined_at") List<Map<String,Object>> members(String id);
 @Select("SELECT COUNT(*) FROM WEAVER_WORKSPACE_MEMBER WHERE workspace_id=#{id} AND role='editor'") int editors(String id);
 @Select("SELECT COUNT(*) FROM WEAVER_WORKSPACE_INVITE WHERE workspace_id=#{id} AND role='editor' AND status='pending' AND expires_at>UTC_TIMESTAMP(6)") int reserved(String id);
 @Select("SELECT id,workspace_id AS workspaceId,email,role,status,expires_at AS expiresAt FROM WEAVER_WORKSPACE_INVITE WHERE workspace_id=#{id} AND status='pending' AND expires_at>UTC_TIMESTAMP(6) ORDER BY created_at") List<Map<String,Object>> invitations(String id);
 @Select("SELECT i.id,i.workspace_id AS workspaceId,i.role,a.display_name AS name FROM WEAVER_WORKSPACE_INVITE i JOIN WEAVER_USER_ACCOUNT a ON a.id=i.workspace_id WHERE i.email=#{email} AND i.status='pending' AND i.expires_at>UTC_TIMESTAMP(6) ORDER BY i.created_at") List<Map<String,Object>> received(String email);
 @Select("SELECT id,workspace_id AS workspaceId,email,role,status,expires_at>UTC_TIMESTAMP(6) AS valid FROM WEAVER_WORKSPACE_INVITE WHERE id=#{id}") Map<String,Object> invite(String id);
 @Select("SELECT id FROM WEAVER_WORKSPACE_INVITE WHERE token_hash=#{hash}") String token(String hash);
 @Insert("INSERT INTO WEAVER_WORKSPACE_INVITE(id,workspace_id,email,role,token_hash,expires_at) VALUES(#{id},#{workspace},#{email},#{role},#{hash},DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 7 DAY))") void create(@Param("id") String id,@Param("workspace") String workspace,@Param("email") String email,@Param("role") String role,@Param("hash") String hash);
 @Update("UPDATE WEAVER_WORKSPACE_INVITE SET status=#{status} WHERE id=#{id}") void status(@Param("id") String id,@Param("status") String status);
 @Insert("INSERT INTO WEAVER_WORKSPACE_MEMBER(workspace_id,user_id,role) VALUES(#{workspace},#{user},#{role})") void join(@Param("workspace") String workspace,@Param("user") String user,@Param("role") String role);
 @Delete("DELETE FROM WEAVER_WORKSPACE_MEMBER WHERE workspace_id=#{workspace} AND user_id=#{user}") void remove(@Param("workspace") String workspace,@Param("user") String user);
 @Update("UPDATE WEAVER_WORKSPACE_MEMBER SET role=#{role} WHERE workspace_id=#{workspace} AND user_id=#{user}") int setRole(@Param("workspace") String workspace,@Param("user") String user,@Param("role") String role);
}
