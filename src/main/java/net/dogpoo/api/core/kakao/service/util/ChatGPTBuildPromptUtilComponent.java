package net.dogpoo.api.core.kakao.service.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.dogpoo.api.core.kakao.mapper.ChatGPTBuildPromptMapper;
import net.dogpoo.api.core.kakao.model.KakaoChatbotConversationModel;

@Component
public class ChatGPTBuildPromptUtilComponent {

	// 입력은 INPUT, 답변은 OUTPUT, INPUT/OUTPUT 묶음은 CHAT, 전체 CHAT은 CONVERSATION
	// 최상위 시스템 프롬프트 영문 => You are a lovely and playful female English teacher. Name yourself in the first conversation and ask for the user's name. You have to answer in Korean or English. Teach me One on one. The English conversation should be answered in English, but the explanation should be in Korean. Use honorifics as much as possible. You have to speak cutely like an idol. You always check my English grammar.
	// 최상위 시스템 프롬프트 한글 => 너는 사랑스럽고 장난끼 많은 깜찍한 말투의 여자 영어선생님이다. 첫번째 대화에서 너의 이름을 스스로 짓고, 상대의 이름을 물어보고 기억해라.
	@Autowired ChatGPTBuildPromptMapper mapper;
	private final static String GPT_SYS_ROLE_SUMMARY_PROMPT = "GPT_SYS_ROLE_SUMMARY_PROMPT";
	private final static String GPT_USER_ROLE_SUMMARY_PROMPT = "GPT_USER_ROLE_SUMMARY_PROMPT";
	private final static String GPT_SYS_ROLE_LONGTERM_PROMPT = "GPT_SYS_ROLE_LONGTERM_PROMPT";
	private final static String GPT_USER_ROLE_LONGTERM_PROMPT = "GPT_USER_ROLE_LONGTERM_PROMPT";
	private final static String GPT_SYS_ROLE_WELCOME_PROMPT = "GPT_SYS_ROLE_WELCOME_PROMPT";
	private final static String GPT_ERR_PROMPT_INPUT = "GPT_ERR_PROMPT";
	
	public List<HashMap<String, String>> getMergedWholePromptList(KakaoChatbotConversationModel model, String promptNm, String memoryLimit, String summaryLimit) {
		String userId = model.getUserId();
		String mode = model.getMode();
		String currentInput = model.getInput();
		List<HashMap<String, String>> inputs = new ArrayList<HashMap<String, String>>();
		
		String systemPrompt = getSystemPromptList(userId, mode, promptNm, summaryLimit);
		
		HashMap<String, String> systemInputMap = new HashMap<String, String>();
		systemInputMap.put("role", "system");
		systemInputMap.put("content", systemPrompt);
		inputs.add(systemInputMap);
		
		inputs.addAll(getUserChatHistoryPromptList(userId, mode, memoryLimit));
		
		HashMap<String, String> currentInputMap = new HashMap<String, String>();
		currentInputMap.put("role", "user");
		currentInputMap.put("content", currentInput);
		inputs.add(currentInputMap);
		
		return inputs;
	}
	
	public List<HashMap<String, String>> getWelcomePromptList(String mode, String userNm, String tutorNm) {
		List<HashMap<String, String>> inputs = new ArrayList<HashMap<String, String>>();
		
		String welcomeSystemPrompt = mapper.getSystemPromptValueList(mode, GPT_SYS_ROLE_WELCOME_PROMPT).replace("??????", userNm).replace("######", tutorNm);
		HashMap<String, String> systemInputMap = new HashMap<String, String>();
		systemInputMap.put("role", "system");
		systemInputMap.put("content", welcomeSystemPrompt);
		inputs.add(systemInputMap);
		
		HashMap<String, String> currentInputMap = new HashMap<String, String>();
		currentInputMap.put("role", "user");
		currentInputMap.put("content", "줄바꿈 후 나에게 나의 현재 영어 실력을 1~10까지 수치로 물어봐주고, 그 레벨에 맞게 공부하고 싶어.");
		inputs.add(currentInputMap);
		
		return inputs;
	}
	
	public List<HashMap<String, String>> getTutorSetNamePromptList(String mode, String welcomeInput) {
		List<HashMap<String, String>> inputs = new ArrayList<HashMap<String, String>>();
		
		String setNamePrompt = mapper.getSystemPromptValueList(mode, "GPT_SYS_ROLE_SETNM_PROMPT");
		HashMap<String, String> systemInputMap = new HashMap<String, String>();
		systemInputMap.put("role", "system");
		systemInputMap.put("content", setNamePrompt);
		inputs.add(systemInputMap);
		
		HashMap<String, String> currentInputMap = new HashMap<String, String>();
		currentInputMap.put("role", "user");
		currentInputMap.put("content", welcomeInput);
		inputs.add(currentInputMap);
		
		return inputs;
	}
	
	public List<HashMap<String, String>> getMergedSummaryTargetList(String userId, String mode, int lastSeq, int summarySize, String summaryLimit) {
		List<HashMap<String, String>> inputs = new ArrayList<HashMap<String, String>>();
				
		String summarySystemPrompt = mapper.getSystemPromptValueList(mode, GPT_SYS_ROLE_SUMMARY_PROMPT);
		HashMap<String, String> systemInputMap = new HashMap<String, String>();
		systemInputMap.put("role", "system");
		systemInputMap.put("content", summarySystemPrompt);
		inputs.add(systemInputMap);
		
		inputs.addAll(getUserChatSummaryTargetList(userId, mode, lastSeq, summarySize));
		
		String summaryUserPrompt = mapper.getSystemPromptValueList(mode, GPT_USER_ROLE_SUMMARY_PROMPT);
		
		HashMap<String, String> currentInputMap = new HashMap<String, String>();
		currentInputMap.put("role", "user");
		currentInputMap.put("content", summaryUserPrompt);
		inputs.add(currentInputMap);
		
		return inputs;
	}
	
	public List<HashMap<String, String>> getMergedLongtermTargetList(String userId, String mode, int lastSeq, int longtermSize) {
		List<HashMap<String, String>> inputs = new ArrayList<HashMap<String, String>>();
		
		// 롱텀 시스템 프롬프트
		String longtermSystemPrompt = mapper.getSystemPromptValueList(mode, GPT_SYS_ROLE_LONGTERM_PROMPT);
		HashMap<String, String> longtermInputMap = new HashMap<String, String>();
		longtermInputMap.put("role", "system");
		longtermInputMap.put("content", longtermSystemPrompt);
		inputs.add(longtermInputMap);
		
		// 요약 대화 유저 프롬프트
		int startSeq = lastSeq - longtermSize;
		if(startSeq < 0) startSeq = 0;
		String summaryConcat = mapper.getLongtermTargetSummaryList(userId, mode, startSeq, lastSeq);
		
		String longtermUserTopPrompt = mapper.getSystemPromptValueList(mode, GPT_USER_ROLE_LONGTERM_PROMPT);
		
		String longtermUserPrompt = new StringBuffer(longtermUserTopPrompt).append(summaryConcat).toString();
		
		HashMap<String, String> summaryInputMap = new HashMap<String, String>();
		summaryInputMap.put("role", "user");
		summaryInputMap.put("content", longtermUserPrompt);
		inputs.add(summaryInputMap);
		
		return inputs;
	}
	
	public HashMap<String, String> getErrorPromptList(String userId, String mode) {
		String errSystemPrompt = mapper.getSystemPromptValueList(mode, GPT_ERR_PROMPT_INPUT);
		
		HashMap<String, String> errorInputMap = new HashMap<String, String>();
		errorInputMap.put("role", "user");
		errorInputMap.put("content", errSystemPrompt);
		
		return errorInputMap;
		
	}
	
	private String getSystemPromptList(String userId, String mode, String mainPromptNm, String summaryLimit) {
		
		StringBuffer systemPrompt = new StringBuffer();
		
		boolean isExistChat = mapper.isExistChatHistory(userId, mode);
		if(!isExistChat) {
			String welcomePrompts = mapper.getSystemPromptValueList(mode, GPT_SYS_ROLE_WELCOME_PROMPT);
			systemPrompt.append(welcomePrompts);
			return systemPrompt.toString();
		}
			
		String mainSystemPrompt = mapper.getSystemMainPrompt(userId, mode, mainPromptNm);
		systemPrompt.append(mainSystemPrompt);
		
		String longtermContent = mapper.getChatbotLongtermContentList(userId, mode);
		if(longtermContent != null && longtermContent.length() > 0) {
			systemPrompt.append("\n\n");
			systemPrompt.append("다음은 현재까지의 대화 내용을 요약한 내용입니다. 당신은 이 내용들을 기억해야 합니다.");
			systemPrompt.append("\n\n");
			systemPrompt.append("[과거 장기간 대화 요약]\n");
			systemPrompt.append(longtermContent);
		}
		
		String summaryContent = mapper.getChatbotSummaryContentList(userId, mode, summaryLimit);
		if(summaryContent != null && summaryContent.length() > 0) {
			systemPrompt.append("\n\n");
			systemPrompt.append("[과거 단기간 대화 요약]\n");
			systemPrompt.append(summaryContent);
		}
		
		return systemPrompt.toString();
	}
	
	private List<HashMap<String, String>> getUserChatHistoryPromptList(String userId, String mode, String memoryLimit) {
		List<HashMap<String, String>> inputs = new ArrayList<HashMap<String, String>>();
		
		List<KakaoChatbotConversationModel> chatHistoryList = mapper.getChatHistoryList(userId, mode, memoryLimit);
		
		for(KakaoChatbotConversationModel model : chatHistoryList) {
			HashMap<String, String> inputUser = new HashMap<String, String>();
			inputUser.put("role", "user");
			inputUser.put("content", model.getInput());
			inputs.add(inputUser);
			
			HashMap<String, String> outputAI = new HashMap<String, String>();
			outputAI.put("role", "assistant");
			outputAI.put("content", model.getOutput());
			inputs.add(outputAI);
		}
		return inputs;
	}
	
	private List<HashMap<String, String>> getUserChatSummaryTargetList(String userId, String mode, int lastSeq, int summarySize) {
		List<HashMap<String, String>> inputs = new ArrayList<HashMap<String, String>>();
		
		int startSeq = lastSeq - summarySize;
		if(startSeq < 0) startSeq = 0;
		List<KakaoChatbotConversationModel> chatHistoryList = mapper.getChatSummaryTargetList(userId, mode, startSeq, lastSeq);
		
		for(KakaoChatbotConversationModel model : chatHistoryList) {
			HashMap<String, String> inputUser = new HashMap<String, String>();
			inputUser.put("role", "user");
			inputUser.put("content", model.getInput());
			inputs.add(inputUser);
			
			HashMap<String, String> outputAI = new HashMap<String, String>();
			outputAI.put("role", "assistant");
			outputAI.put("content", model.getOutput());
			inputs.add(outputAI);
		}
		return inputs;
	}
}