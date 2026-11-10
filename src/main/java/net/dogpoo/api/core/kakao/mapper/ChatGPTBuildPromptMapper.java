package net.dogpoo.api.core.kakao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import net.dogpoo.api.core.kakao.model.KakaoChatbotConversationModel;

public interface ChatGPTBuildPromptMapper {
	
	public String getSystemMainPrompt(@Param("userId") String userId, @Param("mode") String mode, @Param("promptNm") String promptNm);
	
	@Select("SELECT promptValue FROM DOGPOO_AI_SYSTEM_PROMPT WHERE mode = #{mode} AND promptNm = #{promptNm} AND useYn LIMIT 1")
	public String getSystemPromptValueList(@Param("mode") String mode, @Param("promptNm") String promptNm);
	
	@Select("SELECT COUNT(*) FROM DOGPOO_AI_CHATBOT_HISTORY WHERE userId = #{userId} AND mode = #{mode}")
	public boolean isExistChatHistory(@Param("userId") String userId, @Param("mode") String mode);
	
	public List<KakaoChatbotConversationModel> getChatHistoryList(@Param("userId") String userId, @Param("mode") String mode, @Param("memoryLimit") String memoryLimit);
	
	public List<KakaoChatbotConversationModel> getChatSummaryTargetList(@Param("userId") String userId, @Param("mode") String mode, @Param("startSeq") int startSeq, @Param("lastSeq") int lastSeq);
	
	public String getChatbotSummaryContentList(@Param("userId") String userId, @Param("mode") String mode, @Param("summaryLimit") String summaryLimit);
	
	@Select("SELECT IFNULL(content, '') AS content FROM DOGPOO_AI_CHATBOT_LONGTERM WHERE userId = #{userId} AND mode = #{mode} ORDER BY endSeq DESC LIMIT 1")
	public String getChatbotLongtermContentList(@Param("userId") String userId, @Param("mode") String mode);
	
	public String getLongtermTargetSummaryList(@Param("userId") String userId, @Param("mode") String mode, @Param("startSeq") int startSeq, @Param("lastSeq") int lastSeq);
	
	
	
}