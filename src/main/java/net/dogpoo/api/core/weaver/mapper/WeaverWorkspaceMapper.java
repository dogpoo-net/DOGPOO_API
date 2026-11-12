package net.dogpoo.api.core.weaver.mapper;
import org.apache.ibatis.annotations.*;import java.util.*;
import net.dogpoo.api.core.weaver.model.WeaverWorkspaceItem;
import net.dogpoo.api.core.weaver.model.WeaverMediaFile;
@Mapper public interface WeaverWorkspaceMapper {
 @Insert("INSERT IGNORE INTO WEAVER_ACCOUNT_WORKSPACE(user_id) VALUES(#{id})") int ensure(String id);
 @Select("SELECT revision FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id} FOR UPDATE") long lock(String id);
 @Select("SELECT revision FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id}") Long revision(String id);
 @Select("SELECT migrated FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id=#{id}") boolean migrated(String id);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET migrated=TRUE WHERE user_id=#{id}") void migratedDone(String id);
 @Update("UPDATE WEAVER_ACCOUNT_WORKSPACE SET revision=revision+1 WHERE user_id=#{id}") int advance(String id);
 @Select("SELECT w.kind,w.item_id AS id,w.payload,w.deleted,w.revision,a.display_name AS author,DATE_FORMAT(m.updated_at,'%Y-%m-%dT%H:%i:%s.%fZ') AS updatedAt FROM WEAVER_WORKSPACE_ITEM w LEFT JOIN WEAVER_ITEM_METADATA m ON m.workspace_id=w.user_id AND m.item_key=w.item_key LEFT JOIN WEAVER_USER_ACCOUNT a ON a.id=m.created_by WHERE w.user_id=#{id} AND w.revision>#{since} ORDER BY w.revision,w.item_key") List<WeaverWorkspaceItem> changes(@Param("id") String id,@Param("since") long since);
 @Select("SELECT w.kind,w.item_id AS id,w.payload,w.deleted,w.revision,a.display_name AS author,DATE_FORMAT(m.updated_at,'%Y-%m-%dT%H:%i:%s.%fZ') AS updatedAt FROM WEAVER_WORKSPACE_ITEM w LEFT JOIN WEAVER_ITEM_METADATA m ON m.workspace_id=w.user_id AND m.item_key=w.item_key LEFT JOIN WEAVER_USER_ACCOUNT a ON a.id=m.created_by WHERE w.user_id=#{id} AND w.item_key=#{key}") WeaverWorkspaceItem item(@Param("id") String id,@Param("key") String key);
 @Insert("INSERT INTO WEAVER_WORKSPACE_ITEM(user_id,item_key,kind,item_id,payload,deleted,revision) VALUES(#{user},#{key},#{kind},#{id},#{payload},#{deleted},#{revision}) ON DUPLICATE KEY UPDATE payload=VALUES(payload),deleted=VALUES(deleted),revision=VALUES(revision)") int put(@Param("user") String user,@Param("key") String key,@Param("kind") String kind,@Param("id") String id,@Param("payload") String payload,@Param("deleted") boolean deleted,@Param("revision") long revision);
 @Select("SELECT user_id FROM WEAVER_GUEST_IMPORT WHERE guest_id=#{id}") String claimed(String id);
 @Select("SELECT source_hash FROM WEAVER_GUEST_IMPORT WHERE guest_id=#{id}") String sourceHash(String id);
 @Insert("INSERT INTO WEAVER_GUEST_IMPORT(guest_id,user_id,source_hash) VALUES(#{guest},#{id},#{hash})") int claim(@Param("guest") String guest,@Param("id") String id,@Param("hash") String hash);
 @Select("SELECT storage_path AS path,media_type AS type,byte_size AS size,content_hash AS hash FROM WEAVER_WORKSPACE_MEDIA WHERE user_id=#{id} AND media_key=#{key}") WeaverMediaFile media(@Param("id") String id,@Param("key") String key);
 @Insert("INSERT INTO WEAVER_WORKSPACE_MEDIA(user_id,media_key,media_id,storage_path,media_type,byte_size,content_hash) VALUES(#{id},#{key},#{name},#{path},#{type},#{size},#{hash})") int mediaPut(@Param("id") String id,@Param("key") String key,@Param("name") String name,@Param("path") String path,@Param("type") String type,@Param("size") long size,@Param("hash") String hash);
 @Select("SELECT COALESCE(SUM(OCTET_LENGTH(payload)),0) FROM WEAVER_WORKSPACE_ITEM WHERE user_id=#{id} AND deleted=FALSE") long itemBytes(String id);
 @Select("SELECT COALESCE(SUM(byte_size),0) FROM WEAVER_WORKSPACE_MEDIA WHERE user_id=#{id}") long mediaBytes(String id);
 @Insert("INSERT INTO WEAVER_ITEM_METADATA(workspace_id,item_key,created_by,updated_by,updated_at) VALUES(#{workspace},#{key},#{creator},#{actor},UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE updated_by=VALUES(updated_by),updated_at=VALUES(updated_at)") void touch(@Param("workspace") String workspace,@Param("key") String key,@Param("creator") String creator,@Param("actor") String actor);
 @Select("SELECT payload FROM WEAVER_WORKSPACE_ITEM WHERE user_id=#{id} AND deleted=FALSE AND kind IN ('list:elements','list:trash')") List<String> attachmentRecords(String id);
 @Delete("DELETE FROM WEAVER_WORKSPACE_MEDIA WHERE user_id=#{id} AND media_key=#{key}") int removeMedia(@Param("id") String id,@Param("key") String key);
}
