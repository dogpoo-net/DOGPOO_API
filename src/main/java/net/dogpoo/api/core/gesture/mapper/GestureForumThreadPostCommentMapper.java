package net.dogpoo.api.core.gesture.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Select;

import net.dogpoo.api.core.gesture.model.GestureForumThreadPostCommentModel;

public interface GestureForumThreadPostCommentMapper {
	
    @Select("SELECT COUNT(*) FROM GESTURE_THREAD_POST WHERE threadNo = #{threadNo} AND postNo = #{postNo}")
	public boolean isValidThreadNo(HashMap<String, Object> paramMap);
	
	public int insertForumThreadPostComment(HashMap<String, Object> paramMap);
	
	public int updateForumThreadPostComment(HashMap<String, Object> paramMap);
	
	public List<GestureForumThreadPostCommentModel> getForumThreadPostCommentList(HashMap<String, Object> paramMap);

	public int deleteForumThreadPostComment(HashMap<String, Object> paramMap);
}
