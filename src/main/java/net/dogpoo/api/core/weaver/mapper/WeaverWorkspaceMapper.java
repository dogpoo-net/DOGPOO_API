package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;import java.util.*;
import net.dogpoo.api.core.weaver.model.WeaverWorkspaceItem;
import net.dogpoo.api.core.weaver.model.WeaverMediaFile;
@Mapper public interface WeaverWorkspaceMapper {
 @Select("SELECT request_hash FROM WEAVER_CANVAS_OPERATION_RECEIPT WHERE workspace_id=#{workspace} AND operation_id=#{operation}") String canvasReceipt(@Param("workspace") String workspace,@Param("operation") String operation);
 @Insert("INSERT INTO WEAVER_CANVAS_OPERATION_RECEIPT(workspace_id,operation_id,request_hash) VALUES(#{workspace},#{operation},#{hash})") void canvasReceiptPut(@Param("workspace") String workspace,@Param("operation") String operation,@Param("hash") String hash);

 @Insert("INSERT IGNORE INTO WEAVER_ACCOUNT_WORKSPACE(user_id,owner_id,name) SELECT id,id,'기본 워크스페이스' FROM WEAVER_USER_ACCOUNT WHERE id=#{id} AND NOT EXISTS (SELECT 1 FROM WEAVER_ACCOUNT_PROVISION WHERE user_id=#{id}) AND NOT EXISTS (SELECT 1 FROM WEAVER_WORKSPACE_DELETION WHERE workspace_id=#{id})") int ensure(String id);
 @Select("SELECT default_workspace_id FROM WEAVER_USER_ACCOUNT WHERE id=#{id}") String preferredWorkspace(String id);
 @Update("UPDATE WEAVER_USER_ACCOUNT SET default_workspace_id=#{workspace} WHERE id=#{user}") void setPreferredWorkspace(@Param("user") String user,@Param("workspace") String workspace);
 @Select("SELECT id FROM WEAVER_USER_ACCOUNT WHERE id=#{id} FOR UPDATE") String lockAccountId(String id);
 @Select("SELECT owner_id FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id}") String owner(String id);
 @Select("SELECT user_id AS workspaceId,'owner' AS role,COALESCE(name,'내 워크스페이스') AS name FROM WEAVER_ACCOUNT_WORKSPACE WHERE owner_id=#{id} AND archived_at IS NULL ORDER BY (user_id=#{id}) DESC,user_id") List<Map<String,Object>> owned(String id);
 @Select("SELECT EXISTS(SELECT 1 FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id} AND archived_at IS NOT NULL)") boolean archived(String id);
 @Select("SELECT user_id AS workspaceId,'owner' AS role,COALESCE(name,'내 워크스페이스') AS name FROM WEAVER_ACCOUNT_WORKSPACE WHERE owner_id=#{id}") List<Map<String,Object>> ownedIncludingArchived(String id);
 @Insert("INSERT INTO WEAVER_ACCOUNT_WORKSPACE(user_id,owner_id,migrated,name) VALUES(#{id},#{owner},TRUE,'기본 워크스페이스')") void create(@Param("id") String id,@Param("owner") String owner);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET owner_id=#{owner},revision=revision+1 WHERE user_id=#{id}") void transfer(@Param("id") String id,@Param("owner") String owner);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET name=#{name} WHERE user_id=#{id}") void initializeName(@Param("id") String id,@Param("name") String name);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET name=#{name},revision=revision+1 WHERE user_id=#{id}") void rename(@Param("id") String id,@Param("name") String name);
 @Insert("INSERT IGNORE INTO WEAVER_WORKSPACE_DELETION(workspace_id) VALUES(#{id})") void tombstone(String id);
 @Delete("DELETE FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id}") void deleteWorkspace(String id);
 @Select("SELECT revision FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id} FOR UPDATE") long lock(String id);
 @Select("SELECT revision FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id}") Long revision(String id);
 @Select("SELECT migrated FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id}") boolean migrated(String id);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET migrated=TRUE WHERE user_id=#{id}") void migratedDone(String id);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET revision=revision+1 WHERE user_id=#{id}") int advance(String id);
 @Select("SELECT w.kind,w.item_id AS id,"+WeaverItemContentSql.PAYLOAD+" AS payload,w.deleted,w.revision,a.display_name AS author,m.created_by AS authorId,DATE_FORMAT(m.updated_at,'%Y-%m-%dT%H:%i:%s.%fZ') AS updatedAt FROM WEAVER_WORKSPACE_ITEM w"+WeaverItemContentSql.JOINS+" LEFT JOIN WEAVER_ITEM_METADATA m ON m.workspace_id=w.user_id AND m.item_key=w.item_key LEFT JOIN WEAVER_USER_ACCOUNT a ON a.id=m.created_by WHERE w.user_id=#{id} AND w.revision>#{since} ORDER BY w.revision,w.item_key") List<WeaverWorkspaceItem> changes(@Param("id") String id,@Param("since") long since);
 @Select("SELECT w.kind,w.item_id AS id,"+WeaverItemContentSql.PAYLOAD+" AS payload,w.deleted,w.revision,a.display_name AS author,m.created_by AS authorId,DATE_FORMAT(m.updated_at,'%Y-%m-%dT%H:%i:%s.%fZ') AS updatedAt FROM WEAVER_WORKSPACE_ITEM w"+WeaverItemContentSql.JOINS+" LEFT JOIN WEAVER_ITEM_METADATA m ON m.workspace_id=w.user_id AND m.item_key=w.item_key LEFT JOIN WEAVER_USER_ACCOUNT a ON a.id=m.created_by WHERE w.user_id=#{id} AND w.item_key=#{key}") WeaverWorkspaceItem item(@Param("id") String id,@Param("key") String key);
 @Insert("INSERT INTO WEAVER_WORKSPACE_ITEM(user_id,item_key,kind,item_id,payload_bytes,deleted,revision) VALUES(#{user},#{key},#{kind},#{id},#{bytes},#{deleted},#{revision}) ON DUPLICATE KEY UPDATE payload_bytes=VALUES(payload_bytes),deleted=VALUES(deleted),revision=VALUES(revision)")
 int putIndex(@Param("user") String user,@Param("key") String key,@Param("kind") String kind,@Param("id") String id,@Param("bytes") long bytes,@Param("deleted") boolean deleted,@Param("revision") long revision);
 @InsertProvider(type=WeaverItemContentSql.class,method="put") int putContent(@Param("user") String user,@Param("key") String key,@Param("kind") String kind,@Param("payload") String payload);
 @DeleteProvider(type=WeaverItemContentSql.class,method="delete") int deleteContent(@Param("user") String user,@Param("key") String key,@Param("kind") String kind);
 // Caller holds the workspace row lock inside its existing transaction.
 default int put(String user,String key,String kind,String id,String payload,boolean deleted,long revision) {
  if(!deleted && payload==null)throw new IllegalArgumentException("Active item requires content");
  long bytes=deleted?0:payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
  int changed=putIndex(user,key,kind,id,bytes,deleted,revision);
  if(deleted)deleteContent(user,key,kind);else putContent(user,key,kind,payload);
  return changed;
 }

 @Select("SELECT user_id FROM WEAVER_GUEST_IMPORT WHERE guest_id=#{id}") String claimed(String id);
 @Select("SELECT source_hash FROM WEAVER_GUEST_IMPORT WHERE guest_id=#{id}") String sourceHash(String id);
 @Insert("INSERT INTO WEAVER_GUEST_IMPORT(guest_id,user_id,source_hash) VALUES(#{guest},#{id},#{hash})") int claim(@Param("guest") String guest,@Param("id") String id,@Param("hash") String hash);
 @Select("SELECT storage_path AS path,media_type AS type,byte_size AS size,content_hash AS hash FROM WEAVER_WORKSPACE_MEDIA WHERE user_id=#{id} AND media_key=#{key}") WeaverMediaFile media(@Param("id") String id,@Param("key") String key);
 @Insert("INSERT INTO WEAVER_WORKSPACE_MEDIA(user_id,media_key,media_id,storage_path,media_type,byte_size,content_hash) VALUES(#{id},#{key},#{name},#{path},#{type},#{size},#{hash})") int mediaPut(@Param("id") String id,@Param("key") String key,@Param("name") String name,@Param("path") String path,@Param("type") String type,@Param("size") long size,@Param("hash") String hash);
 @Select("SELECT COALESCE(SUM(payload_bytes),0) FROM WEAVER_WORKSPACE_ITEM WHERE user_id=#{id} AND deleted=FALSE") long itemBytes(String id);
 @Select("SELECT COALESCE(SUM(byte_size),0) FROM WEAVER_WORKSPACE_MEDIA WHERE user_id=#{id}") long mediaBytes(String id);
 @Insert("INSERT INTO WEAVER_ITEM_METADATA(workspace_id,item_key,created_by,updated_by,updated_at) VALUES(#{workspace},#{key},#{creator},#{actor},UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE updated_by=VALUES(updated_by),updated_at=VALUES(updated_at)") void touch(@Param("workspace") String workspace,@Param("key") String key,@Param("creator") String creator,@Param("actor") String actor);
 @Select("SELECT c.payload FROM WEAVER_MEMO_CONTENT c JOIN WEAVER_WORKSPACE_ITEM i ON i.user_id=c.workspace_id AND i.item_key=c.item_key WHERE c.workspace_id=#{id} AND i.deleted=FALSE AND i.kind IN ('list:elements','list:trash') UNION ALL SELECT c.payload FROM WEAVER_WORKSPACE_SETTING c JOIN WEAVER_WORKSPACE_ITEM i ON i.user_id=c.workspace_id AND i.item_key=c.item_key WHERE c.workspace_id=#{id} AND i.deleted=FALSE AND i.kind='list:workspacePhotos' UNION ALL SELECT c.payload FROM WEAVER_DOCUMENT_CONTENT c JOIN WEAVER_WORKSPACE_ITEM i ON i.user_id=c.workspace_id AND i.item_key=c.item_key WHERE c.workspace_id=#{id} AND i.deleted=FALSE AND i.kind='list:notes'") List<String> attachmentRecords(String id);
 @Delete("DELETE FROM WEAVER_WORKSPACE_MEDIA WHERE user_id=#{id} AND media_key=#{key}") int removeMedia(@Param("id") String id,@Param("key") String key);
}
