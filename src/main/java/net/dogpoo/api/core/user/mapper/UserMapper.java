package net.dogpoo.api.core.user.mapper;

import java.util.HashMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import net.dogpoo.api.core.user.model.JwtRequest;
import net.dogpoo.api.core.user.model.UserModel;

public interface UserMapper {
	
	public UserModel getGestureUserInfoByUserId(@Param("userId") String userId);
	
	public UserModel getDogpooHomeUserInfoByUserId(@Param("userId") String userId);
	
	@Select("SELECT userId AS username, passwd AS password, userUuid FROM GESTURE_FORUM_USER WHERE userUuid = #{userUuid}")
	public JwtRequest getUserIdPasswdModel(@Param("userUuid") String userUuid);
	
	@Select("SELECT userId AS username, passwd AS password, userUuid FROM GESTURE_FORUM_USER WHERE userId = #{userId} AND snsSignId = #{snsSignId}")
	public JwtRequest getSocialUserIdPasswdModel(@Param("userId") String userId, @Param("snsSignId") String snsSignId);
	
	@Select("SELECT subject, body FROM GESTURE_MAIL_PRESET WHERE keyNm = #{keyNm}")
	public HashMap<String, String> getMailPreset(@Param("keyNm") String keyNm);
}
