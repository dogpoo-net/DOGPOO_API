package net.dogpoo.api.core.kakao.service;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.kakao.mapper.KakaoChatbotEnglishMapper;
import net.dogpoo.api.core.kakao.model.KakaoChatbotConversationModel;
import net.dogpoo.api.core.kakao.service.block.TutorWelcomeBlockComponent;
import net.dogpoo.api.core.kakao.service.util.BuildChatGPTAPIConnectUtil;
import net.dogpoo.api.core.kakao.service.util.CallbackAsyncConnectUtilService;
import net.dogpoo.api.core.kakao.service.util.ChatGPTBuildPromptUtilComponent;
import net.dogpoo.api.core.kakao.service.util.ModelParseUtil;

@Service
public class KakaoChatbotEnglishService {

    public KakaoChatbotEnglishService(
            KakaoChatbotEnglishMapper mapper,
            ChatGPTBuildPromptUtilComponent promptUtil,
            CallbackAsyncConnectUtilService callbackService,
            TutorWelcomeBlockComponent welcome) {
        this.mapper = mapper;
        this.promptUtil = promptUtil;
        this.callbackService = callbackService;
        this.welcome = welcome;
    }

	
	private final KakaoChatbotEnglishMapper mapper;
	private final ChatGPTBuildPromptUtilComponent promptUtil;
	private final CallbackAsyncConnectUtilService callbackService;
	private final TutorWelcomeBlockComponent welcome;
	
	private final static Logger LOG = LoggerFactory.getLogger(KakaoChatbotEnglishService.class);
	private final static int CALLBACK_TIMEOUT_MS = 20000;
	private final static String GPT_SYS_ROLE_MAIN_PROMPT = "GPT_SYS_ROLE_MAIN_PROMPT";
	private final static String GPT_USER_ROLE_WELCOME_PROMPT = "GPT_USER_ROLE_WELCOME_PROMPT";
	private final static String GPT_TUTOR_CHAT_MODEL_NM = "GPT_TUTOR_CHAT_MODEL_NM";
	
	/**
	 * 일반 대화
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> getChatbotEngTeacherAnswer(HashMap<String, Object> paramMap) {
//		System.out.println(paramMap);
		
		HashMap<String, String> params = ModelParseUtil.getRequestParams(paramMap);
		String userId = params.get("userId");
		String mode = params.get("mode");
		String welcomeKey = params.get("welcomeKey");
		String question = params.get("question");
		
		boolean isExistChatbotUser = mapper.isExistChatbotUser(userId, mode);
		if(!isExistChatbotUser || "V0VMQ09NRUtFWQ==".equals(welcomeKey)) {
			String temp = mapper.getUserNm(userId, mode);
			if(null != temp && "dXNlck5t".equals(temp)) {
				mapper.updateUserName(userId, mode, question);
				String tutorNm = mapper.getTutorNm(userId, mode);
				return generateTutor(paramMap, tutorNm);
			}
			HashMap<String, Object> resMap = welcome.getWelcomeProcess(userId, mode);
			return resMap;
		}
		
		long currentMilli = System.currentTimeMillis();
		String memoryLimit = params.get("memoryLimit");
		String summaryLimit = params.get("summaryLimit");
		String callbackUrl = params.get("callbackUrl");
		
		KakaoChatbotConversationModel model = new KakaoChatbotConversationModel();
		model.setUserId(userId);
		model.setMode(mode);
		model.setInput(question);
		
		int chatSeq = mapper.getNextUserSeq(userId, mode);
		model.setChatSeq(chatSeq);
		
		mapper.insertChatbotConversationHistory(model);
		int idx = model.getIdx();
		
		String modelNm = mapper.getConfigConstValue(GPT_TUTOR_CHAT_MODEL_NM);
		
		List<HashMap<String, String>> inputs = promptUtil.getMergedWholePromptList(model, GPT_SYS_ROLE_MAIN_PROMPT, memoryLimit, summaryLimit);
		callbackService.buildChatGPTAPIConnection(inputs, CALLBACK_TIMEOUT_MS, modelNm, model);
		
		HashMap<String, Object> resultMap = getSeekerGPTAnswer(idx);
		String answer = (String) resultMap.get("answer");
		boolean isCallback = (boolean) resultMap.get("isCallback");
		
		HashMap<String, Object> resMap = ModelParseUtil.setResponseSimpleText(answer, isCallback);
		
		long workoutMilli = System.currentTimeMillis();
		int runTime = (int) (workoutMilli - currentMilli);
		model.setRunTime(runTime);
		
		if(isCallback) {
			callbackService.buildKakaoChatbotCallbackConnection(callbackUrl, idx, model, "SIMPLE_TEXT");
		} else {
			LOG.info("====================================================");
			System.out.println("RUNTIME => " + runTime);
			System.out.println("USERID ==> " + userId);
			System.out.println("INPUT ===> " + question);
			System.out.println("OUTPUT ==> " + answer);
			LOG.info("====================================================");
		}
		
		return resMap;
	}
	
	/**
	 * 웰컴 블록
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> generateTutor(HashMap<String, Object> paramMap, String tutorNm) {
		long currentMilli = System.currentTimeMillis();
		
		HashMap<String, String> params = ModelParseUtil.getRequestParams(paramMap);
		String userId = params.get("userId");
		String mode = params.get("mode");
		String question = params.get("question");
		String callbackUrl = params.get("callbackUrl");
		
		KakaoChatbotConversationModel model = new KakaoChatbotConversationModel();
		model.setUserId(userId);
		model.setMode(mode);
		model.setInput(question);
		
		mapper.insertChatbotConversationHistory(model);
		int idx = model.getIdx();
		
		String modelNm = mapper.getConfigConstValue(GPT_TUTOR_CHAT_MODEL_NM);
		
		List<HashMap<String, String>> inputs = promptUtil.getWelcomePromptList(mode, question, tutorNm);
		callbackService.buildChatGPTAPIConnection(inputs, CALLBACK_TIMEOUT_MS, modelNm, model);
		
		HashMap<String, Object> resultMap = getSeekerGPTAnswer(idx);
		String answer = (String) resultMap.get("answer");
		boolean isCallback = (boolean) resultMap.get("isCallback");
		
		HashMap<String, Object> resMap = ModelParseUtil.setResponseBasicCard(answer, isCallback);
		
		long workoutMilli = System.currentTimeMillis();
		int runTime = (int) (workoutMilli - currentMilli);
		model.setRunTime(runTime);
		
		mapper.updateGenTutorFlag(userId, mode);
		
		if(isCallback) {
			callbackService.buildKakaoChatbotCallbackConnection(callbackUrl, idx, model, "BASIC_CARD");
		} else {
			LOG.info("====================================================");
			System.out.println("RUNTIME => " + runTime);
			System.out.println("USERID ==> " + userId);
			System.out.println("INPUT ===> " + question);
			System.out.println("OUTPUT ==> " + answer);
		}
		
		return resMap;
	}
	
	/**
	 * 기억 삭제 블록
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> removeChatbotWholeMemory(HashMap<String, Object> paramMap) {
		long currentMilli = System.currentTimeMillis();
		
		HashMap<String, String> params = ModelParseUtil.getRequestParams(paramMap);
		String userId = params.get("userId");
		String mode = params.get("mode");
		String question = mapper.getSystemPromptValue(GPT_USER_ROLE_WELCOME_PROMPT);
		
		KakaoChatbotConversationModel model = new KakaoChatbotConversationModel();
		model.setUserId(userId);
		model.setMode(mode);
		model.setInput(question);
		
		mapper.deleteWholeChatUserHistory(userId, mode);
		mapper.deleteWholeChatUserSummary(userId, mode);
		mapper.deleteWholeChatUserLongterm(userId, mode);
		mapper.deleteWholeChatUser(userId, mode);
		
		String answer = "모든 기억과 정보를 삭제했어요. 처음부터 다시 시작해볼까요?\n\n저에게 아무말이나 걸어주세요.";
		
		long workoutMilli = System.currentTimeMillis();
		int runTime = (int) (workoutMilli - currentMilli);
		
		// 기억 삭제 블록에서는 콜백을 사용하지 않음.
		boolean isCallback = false;
		HashMap<String, Object> resMap = ModelParseUtil.setResponseSimpleText(answer, isCallback);
		
		System.out.println("RUNTIME => " + runTime);
		System.out.println("USERID ==> " + userId);
		System.out.println("INPUT ===> " + question);
		System.out.println("OUTPUT ==> " + answer);
		System.out.println();
		LOG.info("====================================================");
		
		return resMap;
	}
	
	public String callChatGPT(String mode, String question, int timeoutMs, String modelNm) {
		List<HashMap<String, String>> inputs = promptUtil.getWelcomePromptList(mode, "", "");
		String responseBody = BuildChatGPTAPIConnectUtil.buildChatGPTAPIConnection(inputs, timeoutMs, modelNm);
		String answer = ModelParseUtil.getParsingOutputText(responseBody);
		return answer;
	}
	
	public HashMap<String, Object> getSeekerGPTAnswer(int idx) {
		
		HashMap<String, Object> resultMap = new HashMap<String, Object>();
		
		String answer = "";
		boolean isCallback = false;
		for(int i = 0; i <= 5; i++) {
			int ms = i < 2 ? 1000 : 520;
			try {
				Thread.sleep(ms);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			answer = mapper.getChatbotAIAnswerContent(idx);
			if(i == 5) isCallback = true;
			if(answer != null && answer.length() > 0) {
				break;
			}
		}
		
		resultMap.put("answer", answer);
		resultMap.put("isCallback", isCallback);
		
		return resultMap;
	}
}