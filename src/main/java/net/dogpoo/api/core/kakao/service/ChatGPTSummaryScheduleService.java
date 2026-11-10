package net.dogpoo.api.core.kakao.service;

import java.util.HashMap;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.kakao.mapper.ChatGPTSummaryScheduleMapper;
import net.dogpoo.api.core.kakao.model.KakaoChatbotSummaryModel;
import net.dogpoo.api.core.kakao.service.util.BuildChatGPTAPIConnectUtil;
import net.dogpoo.api.core.kakao.service.util.ChatGPTBuildPromptUtilComponent;
import net.dogpoo.api.core.kakao.service.util.ModelParseUtil;

@Service
public class ChatGPTSummaryScheduleService {

    public ChatGPTSummaryScheduleService(
            ChatGPTSummaryScheduleMapper mapper,
            ChatGPTBuildPromptUtilComponent promptUtil) {
        this.mapper = mapper;
        this.promptUtil = promptUtil;
    }

	
	private final ChatGPTSummaryScheduleMapper mapper;
	private final ChatGPTBuildPromptUtilComponent promptUtil;
	private static String serverType = System.getProperty("serverType");
	private final static String GPT_SUMMARY_TARGET_SIZE = "GPT_SUMMARY_TARGET_SIZE";
	private final static String GPT_LONGTERM_TARGET_SIZE = "GPT_LONGTERM_TARGET_SIZE";
	private final static String GPT_API_SUMMARY_MODEL_NM = "GPT_API_SUMMARY_MODEL_NM";
	private final static int DEFAULT_CHAT_SUMMARY_SIZE = 10;
	private final static int DEFAULT_LONGTERM_SIZE = 5;
	private final static int TIMEOUT_MILLISECOND = 60000;

	@Scheduled(fixedDelay = 300000)
	public void summaryHistoryScheduleMain() {
		if(!"live".equals(serverType)) return;
		String summarySizeStr = mapper.getConfigConstValue(GPT_SUMMARY_TARGET_SIZE);
		int summarySize = Integer.parseInt(summarySizeStr);
		if(summarySize == 0) summarySize = DEFAULT_CHAT_SUMMARY_SIZE;
		
		insertSummaryHistoryData(summarySize);
	}
	
	@Scheduled(cron = "0 0/30 * * * *")
	public void buildLongtermMain() {
		if(!"live".equals(serverType)) return;
		String longtermSizeStr = mapper.getConfigConstValue(GPT_LONGTERM_TARGET_SIZE);
		int longtermSize = Integer.parseInt(longtermSizeStr);
		if(longtermSize == 0) longtermSize = DEFAULT_LONGTERM_SIZE;
		
		insertLongtermMemory(longtermSize);
	}
	
	public void insertSummaryHistoryData(int summarySize) {
		List<HashMap<String, Object>> summaryTargets = mapper.getSummaryTargetList(summarySize);
		String modelNm = mapper.getConfigConstValue(GPT_API_SUMMARY_MODEL_NM);
		
		for(HashMap<String, Object> summaryTargetMap : summaryTargets) {
			String userId = summaryTargetMap.get("userId").toString();
			String mode = summaryTargetMap.get("mode").toString();
			int lastSeq = Integer.parseInt(summaryTargetMap.get("lastSeq").toString());
			
			for(int seq = summarySize; seq <= lastSeq; seq+=summarySize) {
				boolean isExistSummary = mapper.isExistSummary(userId, mode, seq);
				if(isExistSummary) continue;
				
				String summaryLimit = mapper.getConfigConstValue(GPT_LONGTERM_TARGET_SIZE);
				List<HashMap<String, String>> inputs = promptUtil.getMergedSummaryTargetList(userId, mode, seq, summarySize, summaryLimit);
				String responseBody = BuildChatGPTAPIConnectUtil.buildChatGPTAPIConnection(inputs, TIMEOUT_MILLISECOND, modelNm);
				String summaryContent = ModelParseUtil.getParsingOutputText(responseBody)
										.replace("너의 이름: 없음 ", "")
										.replace("너의 이름: 없음, ", "")
										.replace("나의 이름: 없음 ", "")
										.replace("나의 이름: 없음", "");
				
				while(true) {
					if(summaryContent.charAt(0) == ' ' || summaryContent.charAt(0) == '\n') {
						summaryContent = summaryContent.substring(1);
					} else {
						break;
					}
				}
				
				int summarySeq = mapper.getNextSummarySeq(userId, mode);
				KakaoChatbotSummaryModel summaryModel = new KakaoChatbotSummaryModel();
				summaryModel.setMode(mode);
				summaryModel.setSummarySeq(summarySeq);
				summaryModel.setEndSeq(seq);
				summaryModel.setUserId(userId);
				summaryModel.setContent(summaryContent);
				mapper.insertChatbotSummaryContent(summaryModel);
			}
		}
	}
	
	public void insertLongtermMemory(int longtermSize) {
		List<HashMap<String, Object>> longtermTargets = mapper.getLongtermTargetList(longtermSize);
		String modelNm = mapper.getConfigConstValue(GPT_API_SUMMARY_MODEL_NM);
		
		for(HashMap<String, Object> longtermTargetMap : longtermTargets) {
			String userId = longtermTargetMap.get("userId").toString();
			String mode = longtermTargetMap.get("mode").toString();
			int lastSeq = Integer.parseInt(longtermTargetMap.get("lastSeq").toString());
			
			for(int seq = longtermSize; seq <= lastSeq; seq+=longtermSize) {
				boolean isExistLongterm = mapper.isExistLongterm(userId, mode, seq);
				if(isExistLongterm) continue;
				
				List<HashMap<String, String>> inputs = promptUtil.getMergedLongtermTargetList(userId, mode, seq, longtermSize);
				String responseBody = BuildChatGPTAPIConnectUtil.buildChatGPTAPIConnection(inputs, TIMEOUT_MILLISECOND, modelNm);
				String longtermContent = ModelParseUtil.getParsingOutputText(responseBody);
				
				KakaoChatbotSummaryModel longtermModel = new KakaoChatbotSummaryModel();
				longtermModel.setMode(mode);
				longtermModel.setEndSeq(seq);
				longtermModel.setUserId(userId);
				longtermModel.setContent(longtermContent);
				
				mapper.insertLongtermContent(longtermModel);
			}
		}
	}
}