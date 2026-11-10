package net.dogpoo.api.core.kakao.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import net.dogpoo.api.core.kakao.model.KakaoChatbotUserModel;

public interface TutorWelcomeBlockMapper {
	
	@Select("SELECT idx, userId, mode, userNm, gender, tutorNm, tutorGender, isGenTutor FROM DOGPOO_AI_CHATBOT_USER WHERE userId = #{userId} AND mode = #{mode}")
	public KakaoChatbotUserModel getChatbotUser(@Param("userId") String userId, @Param("mode") String mode);
	
	@Insert("INSERT INTO DOGPOO_AI_CHATBOT_USER (userId, mode) VALUES (#{userId}, #{mode})")
	public int initChatbotUser(@Param("userId") String userId, @Param("mode") String mode);
	
	@Update("UPDATE DOGPOO_AI_CHATBOT_USER SET gender = #{gender} WHERE userId = #{userId} AND mode = #{mode}")
	public int updateUserGender(@Param("userId") String userId, @Param("mode") String mode, @Param("gender") String gender);
	
	@Update("UPDATE DOGPOO_AI_CHATBOT_USER SET userNm = #{userNm} WHERE userId = #{userId} AND mode = #{mode}")
	public int updateUserName(@Param("userId") String userId, @Param("mode") String mode, @Param("userNm") String userNm);
	
	@Update("UPDATE DOGPOO_AI_CHATBOT_USER SET tutorNm = #{tutorNm} WHERE userId = #{userId} AND mode = #{mode}")
	public int updateTutorName(@Param("userId") String userId, @Param("mode") String mode, @Param("tutorNm") String tutorNm);
	

}
