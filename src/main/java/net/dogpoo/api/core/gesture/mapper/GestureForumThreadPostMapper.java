package net.dogpoo.api.core.gesture.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import net.dogpoo.api.core.gesture.model.GestureForumThreadHeadContentModel;
import net.dogpoo.api.core.gesture.model.GestureForumThreadHeaderModel;
import net.dogpoo.api.core.gesture.model.GestureForumThreadPostModel;

public interface GestureForumThreadPostMapper {
	
	public List<GestureForumThreadHeaderModel> getGestureForumThreadWholeList(HashMap<String, Object> paramMap);
	
	public int getGestureForumThreadWholeListCount(HashMap<String, Object> paramMap);
	
	public List<GestureForumThreadHeaderModel> getGestureForumThreadListSearch(HashMap<String, Object> paramMap);
	
	public int getGestureForumThreadListSearchCount(HashMap<String, Object> paramMap);
	
	public GestureForumThreadHeaderModel getForumThreadHeadData(HashMap<String, Object> paramMap);
	
	public int updateForumThreadCountingViewNum(HashMap<String, Object> paramMap);
	
	public List<GestureForumThreadPostModel> getGestureForumThreadPost(HashMap<String, Object> paramMap);
	
	public GestureForumThreadHeadContentModel getForumHeadThreadContent(HashMap<String, Object> paramMap);

	public List<HashMap<String, Object>> getForumThreadCategoryList(@Param("ccode") String ccode);
	
	public int insertForumThread(HashMap<String, Object> paramMap);
	
	public int insertForumThreadHeadPost(HashMap<String, Object> paramMap);
	
	public void deleteForumThread(HashMap<String, Object> paramMap);
	
	public int insertForumThreadPost(HashMap<String, Object> paramMap);
	
	public String getForumThreadPostContentOnly(HashMap<String, Object> paramMap);
	
	public List<String> getForumThreadHeadPostContentOnly(HashMap<String, Object> paramMap);
	
	public int updateForumThread(HashMap<String, Object> paramMap);
	
	public int updateForumThreadPost(HashMap<String, Object> paramMap);
	
	public int deleteForumThreadPost(HashMap<String, Object> paramMap);
	
	public int removeForumThreadWholeData(HashMap<String, Object> paramMap);
	
	@Insert("INSERT INTO GESTURE_THREAD_POST_LIKE VALUES (${threadNo}, ${postNo}, ${ccode}, ${isPostLike})")
	public int insertForumThreadPostLike(HashMap<String, Object> paramMap);
	
	@Select("SELECT COUNT(*) FROM GESTURE_THREAD_POST_LIKE WHERE threadNo = ${threadNo} AND postNo = ${postNo} AND ccode = ${ccode}")
	public boolean getFourmThreadPostLikeDuplicateCheck(HashMap<String, Object> paramMap);
	
	@Select("SELECT isPostLike FROM GESTURE_THREAD_POST_LIKE WHERE threadNo = ${threadNo} AND postNo = ${postNo} AND ccode = ${ccode}")
	public int isPostLike(HashMap<String, Object> paramMap);
	
	@Delete("DELETE FROM GESTURE_THREAD_POST_LIKE WHERE threadNo = ${threadNo} AND postNo = ${postNo} AND ccode = ${ccode}")
	public int deleteForumThreadPostLike(HashMap<String, Object> paramMap);
	
	@Update("UPDATE GESTURE_THREAD_POST SET ${target} = ${target} ${cnt} WHERE postNo = ${postNo} AND threadNo = ${threadNo}")
	public int updateForumThreadPostLikeCnt(HashMap<String, Object> paramMap);
	
	@Select("SELECT COUNT(*) FROM GESTURE_THREAD_HEAD_LIKE WHERE threadNo = ${threadNo} AND ccode = ${ccode}")
	public boolean isAlreadyForumThreadLike(HashMap<String, Object> paramMap);
	
	@Insert("INSERT INTO GESTURE_THREAD_HEAD_LIKE VALUES (${threadNo}, ${ccode})")
	public int insertForumThreadLike(HashMap<String, Object> paramMap);
	
	@Delete("DELETE FROM GESTURE_THREAD_HEAD_LIKE WHERE threadNo = ${threadNo} AND ccode = ${ccode}")
	public int deleteForumThreadLike(HashMap<String, Object> paramMap);
	
	@Update("UPDATE GESTURE_THREAD_HEAD SET threadLikeCnt = threadLikeCnt ${cnt} WHERE threadNo = ${threadNo}")
	public int updateForumThreadLikeCnt(HashMap<String, Object> paramMap);
	
}