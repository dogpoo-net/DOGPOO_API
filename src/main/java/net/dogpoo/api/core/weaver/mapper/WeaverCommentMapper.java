package net.dogpoo.api.core.weaver.mapper;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper public interface WeaverCommentMapper {
 @Select("SELECT COUNT(*) FROM WEAVER_WORKSPACE_ITEM WHERE user_id=#{workspace} AND kind='list:elements' AND item_id=#{memo} AND deleted=FALSE") int exists(@Param("workspace")String workspace,@Param("memo")String memo);
 @Select("SELECT c.id,c.content,a.display_name AS author,DATE_FORMAT(c.created_at,'%Y-%m-%dT%H:%i:%s.%fZ') AS createdAt FROM WEAVER_MEMO_COMMENT_DATA c JOIN WEAVER_USER_ACCOUNT a ON a.id=c.author_id WHERE c.workspace_id=#{workspace} AND c.memo_id=#{memo} AND c.id<#{before} ORDER BY c.id DESC LIMIT 50") List<Map<String,Object>> list(@Param("workspace")String workspace,@Param("memo")String memo,@Param("before")long before);
 @Insert("INSERT INTO WEAVER_MEMO_COMMENT_DATA(workspace_id,memo_id,author_id,request_id,content) VALUES(#{workspace},#{memo},#{actor},#{request},#{content}) ON DUPLICATE KEY UPDATE id=id") int add(@Param("workspace")String workspace,@Param("memo")String memo,@Param("actor")String actor,@Param("request")String request,@Param("content")String content);
 @Select("SELECT workspace_id AS workspace,memo_id AS memo,content FROM WEAVER_MEMO_COMMENT_DATA WHERE author_id=#{actor} AND request_id=#{request}") Map<String,Object> submitted(@Param("actor")String actor,@Param("request")String request);
}
