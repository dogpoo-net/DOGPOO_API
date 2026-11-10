package net.dogpoo.api.core.kakao.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import net.dogpoo.api.core.kakao.model.KakaoChatbotSummaryModel;

public interface ChatGPTSummaryScheduleMapper {
	
	@Select("SELECT configValue FROM COMMON_CONFIG_CONST WHERE configNm = #{configNm}")
	public String getConfigConstValue(@Param("configNm") String configNm);
	
	@Select("SELECT promptValue FROM DOGPOO_AI_SYSTEM_PROMPT WHERE promptNm = #{promptNm}")
	public String getSystemPromptValue(@Param("promptNm") String promptNm);
	
    @Select("SELECT COUNT(*) FROM DOGPOO_AI_CHATBOT_SUMMARY WHERE userId = #{userId} AND mode = #{mode} AND endSeq = #{lastSeq};")
	public boolean isExistSummary(@Param("userId") String userId, @Param("mode") String mode, @Param("lastSeq") int lastSeq);
	
    @Select("SELECT COUNT(*) FROM DOGPOO_AI_CHATBOT_LONGTERM WHERE userId = #{userId} AND mode = #{mode} AND endSeq = #{lastSeq};")
	public boolean isExistLongterm(@Param("userId") String userId, @Param("mode") String mode, @Param("lastSeq") int lastSeq);
	
	public List<HashMap<String, Object>> getSummaryTargetList(@Param("summarySize") int summarySize);
	
	public int getNextSummarySeq(@Param("userId") String userId, @Param("mode") String mode);
	
	public int insertChatbotSummaryContent(KakaoChatbotSummaryModel model);
	
	public List<HashMap<String, Object>> getLongtermTargetList(@Param("longtermSize") int longtermSize);
	
	public int insertLongtermContent(KakaoChatbotSummaryModel model);
	
	/* 이것도 가능하지 않을까...
	 SELECT 
	  a.userId,
	  CAST(MAX(chatSeq) / 20 AS UNSIGNED) * 20 
	FROM
	  DOGPOO_AI_CHATBOT_HISTORY AS a
	  INNER JOIN DOGPOO_AI_CHATBOT_SUMMARY AS b
	  ON a.userId = b.userId
	WHERE endSeq = CAST(MAX(chatSeq) / 20 AS UNSIGNED) * 20 
	GROUP BY a.userId
	HAVING ;
	 */
}
