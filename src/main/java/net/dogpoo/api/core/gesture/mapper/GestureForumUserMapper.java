package net.dogpoo.api.core.gesture.mapper;

import java.util.HashMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import net.dogpoo.api.core.user.model.JwtRequest;
import net.dogpoo.api.core.user.model.UserModel;

public interface GestureForumUserMapper {
	
	@Select("SELECT userId AS username, passwd AS password, userUuid FROM GESTURE_FORUM_USER WHERE userUuid = #{userUuid}")
	public JwtRequest getUserIdPasswdModel(@Param("userUuid") String userUuid);
	
	@Select("SELECT userId AS username, passwd AS password, userUuid FROM GESTURE_FORUM_USER WHERE userId = #{userId} AND snsSignId = #{snsSignId}")
	public JwtRequest getSocialUserIdPasswdModel(@Param("userId") String userId, @Param("snsSignId") String snsSignId);
	
	@Update("UPDATE GESTURE_FORUM_USER SET userUuid = #{userUuid} WHERE ccode = #{ccode}")
	public int updateUserUuid(@Param("ccode") String ccode, @Param("userUuid") String userUuid);
	
	@Select("SELECT COUNT(*) FROM GESTURE_FORUM_USER WHERE userId = #{userId} AND snsSignId = #{snsSignId}")
	public boolean isSocialSignIn(@Param("userId") String userId, @Param("snsSignId") String snsSignId);

	public UserModel getGestureUserInfoByUuid(@Param("userUuid") String userUuid);
	
	public UserModel getGestureUserInfoByUserId(@Param("userId") String userId);
	
	public int withdrawal(HashMap<String, Object> paramMap);
	
	public int deleteUserLikeHist(HashMap<String, Object> paramMap);
	
	public int duplicateCheckUserId(@Param("userId") String userId);
	
	@Select("SELECT COUNT(*) FROM GESTURE_FORUM_USER WHERE userNm = #{userNm}")
	public boolean duplicateCheckUserNm(@Param("userNm") String userNm);
	
	@Select("SELECT COUNT(*) FROM GESTURE_FORUM_USER WHERE userId = #{userId} AND passwd = #{passwd}")
	public boolean checkUserPassword(HashMap<String, Object> paramMap);
	
	public int insertSignUpUser(UserModel userModel);
	
	@Select("SELECT userWaitCd FROM GESTURE_FORUM_USER WHERE userId = #{userId}")
	public String getUserWaitCd(@Param("userId") String userId);
	
	@Update("UPDATE GESTURE_FORUM_USER SET userWaitCd = NULL, userType = 'NORMAL' WHERE userId = #{userId}")
	public int updateSignUpComplete(@Param("userId") String userId);
	
	@Update("UPDATE GESTURE_FORUM_USER SET userNm = #{nickname}, passwd = #{passwd} WHERE ccode = #{ccode} AND userId = #{username}")
	public int updateMyPage(HashMap<String, Object> paramMap);
	
	@Update("UPDATE GESTURE_FORUM_USER SET userNm = #{nickname} WHERE ccode = #{ccode} AND userId = #{username}")
	public int updateMyPageOnlyUserNm(HashMap<String, Object> paramMap);
	
	@Update("UPDATE GESTURE_FORUM_USER SET passwd = #{passwd} WHERE ccode = #{ccode} AND userId = #{username}")
	public int updateMyPageOnlyPasswd(HashMap<String, Object> paramMap);
	
	@Select("SELECT COUNT(*) FROM GESTURE_FORUM_USER WHERE userNm = #{nickname} AND ccode != #{ccode}")
	public boolean updateMyPageCheckUserNm(HashMap<String, Object> paramMap);
	
	@Select("SELECT userType FROM GESTURE_FORUM_USER WHERE userId = #{userId}")
	public String getUserType(@Param("userId") String userId);
	
	@Update("UPDATE GESTURE_FORUM_USER SET userWaitCd = #{userWaitCd} WHERE userId = #{userId}")
	public int updateUserWaitCd(@Param("userWaitCd") String userWaitCd, @Param("userId") String userId);
	
	@Update("UPDATE GESTURE_FORUM_USER SET passwd = #{passwd}, userWaitCd = NULL WHERE userId = #{userId} AND userWaitCd = #{userWaitCd}")
	public int updateInitPassword(HashMap<String, Object> paramMap);
	
	@Update("UPDATE FORUM_VISIT_COUNT SET visitor = visitor + 1")
	public void updateVisitorCount();
	
	@Select("SELECT visitor FROM FORUM_VISIT_COUNT")
	public int getVisitorCount();
}
