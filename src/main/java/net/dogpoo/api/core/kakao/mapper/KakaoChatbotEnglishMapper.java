package net.dogpoo.api.core.kakao.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import net.dogpoo.api.core.kakao.model.KakaoChatbotConversationModel;

public interface KakaoChatbotEnglishMapper {
	
	@Select("SELECT configValue FROM COMMON_CONFIG_CONST WHERE configNm = #{configNm}")
	public String getConfigConstValue(@Param("configNm") String configNm);
	
	@Select("SELECT promptValue FROM DOGPOO_AI_SYSTEM_PROMPT WHERE promptNm = #{promptNm}")
	public String getSystemPromptValue(@Param("promptNm") String promptNm);
	
	public int insertChatbotConversationHistory(KakaoChatbotConversationModel model);
	
	public int insertChatbotWholeConversation(KakaoChatbotConversationModel model);
	
	@Update("UPDATE DOGPOO_AI_CHATBOT_HISTORY SET output = #{output}, inputTokens = ${inputTokens}, outputTokens = ${outputTokens}, totalTokens = ${totalTokens} WHERE idx = #{idx}")
	public int updateChatbotAIAnswerContent(KakaoChatbotConversationModel model);
	
	@Select("SELECT output FROM DOGPOO_AI_CHATBOT_HISTORY WHERE idx = #{idx}")
	public String getChatbotAIAnswerContent(@Param("idx") int idx);
	
	public int getNextUserSeq(@Param("userId") String userId, @Param("mode") String mode);
	
	@Delete("DELETE FROM DOGPOO_AI_CHATBOT_HISTORY WHERE idx = #{idx}")
	public int deleteChatbotConversationHistory(@Param("idx") int idx);
	
	@Select("SELECT errMsg FROM DOGPOO_AI_ERROR_MSG_LIST ORDER BY RAND() LIMIT 1")
	public String getRandomErrMsg();
	
	@Delete("DELETE FROM DOGPOO_AI_CHATBOT_HISTORY WHERE userId = #{userId} AND mode = #{mode}")
	public int deleteWholeChatUserHistory(@Param("userId") String userId, @Param("mode") String mode);
	
	@Delete("DELETE FROM DOGPOO_AI_CHATBOT_SUMMARY WHERE userId = #{userId} AND mode = #{mode}")
	public int deleteWholeChatUserSummary(@Param("userId") String userId, @Param("mode") String mode);
	
	@Delete("DELETE FROM DOGPOO_AI_CHATBOT_LONGTERM WHERE userId = #{userId} AND mode = #{mode}")
	public int deleteWholeChatUserLongterm(@Param("userId") String userId, @Param("mode") String mode);
	
	@Delete("DELETE FROM DOGPOO_AI_CHATBOT_USER WHERE userId = #{userId} AND mode = #{mode}")
	public int deleteWholeChatUser(@Param("userId") String userId, @Param("mode") String mode);
	
	@Select("SELECT IF(COUNT(*) > 0, IF(userNm IS NULL OR gender IS NULL OR isGenTutor = 0, 0, 1), 0) AS result FROM DOGPOO_AI_CHATBOT_USER WHERE userId = #{userId} AND mode = #{mode}")
	public boolean isExistChatbotUser(@Param("userId") String userId, @Param("mode") String mode);
	
	@Select("SELECT userNm FROM DOGPOO_AI_CHATBOT_USER WHERE userId = #{userId} AND mode = #{mode}")
	public String getUserNm(@Param("userId") String userId, @Param("mode") String mode);
	
	@Select("SELECT tutorNm FROM DOGPOO_AI_CHATBOT_USER WHERE userId = #{userId} AND mode = #{mode}")
	public String getTutorNm(@Param("userId") String userId, @Param("mode") String mode);
	
	@Update("UPDATE DOGPOO_AI_CHATBOT_USER SET userNm = #{userNm} WHERE userId = #{userId} AND mode = #{mode}")
	public int updateUserName(@Param("userId") String userId, @Param("mode") String mode, @Param("userNm") String userNm);
	
	@Update("UPDATE DOGPOO_AI_CHATBOT_USER SET isGenTutor = 1 WHERE userId = #{userId} AND mode = #{mode}")
	public int updateGenTutorFlag(@Param("userId") String userId, @Param("mode") String mode);

}
