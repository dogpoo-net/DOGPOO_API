package net.dogpoo.api.core.kakao.service.block;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.dogpoo.api.core.kakao.mapper.TutorWelcomeBlockMapper;
import net.dogpoo.api.core.kakao.model.KakaoChatbotUserModel;
import net.dogpoo.api.core.kakao.service.util.BuildChatGPTAPIConnectUtil;
import net.dogpoo.api.core.kakao.service.util.ChatGPTBuildPromptUtilComponent;
import net.dogpoo.api.core.kakao.service.util.ModelParseUtil;

@Component
public class TutorWelcomeBlockComponent {

	@Autowired TutorWelcomeBlockMapper mapper;
	@Autowired ChatGPTBuildPromptUtilComponent promptUtil;
	
	private final static Logger LOG = LoggerFactory.getLogger(TutorWelcomeBlockComponent.class);
	private final static int DEFAULT_TIMEOUT_MS = 4200;
	
	public HashMap<String, Object> getWelcomeProcess(String userId, String mode) {
		
		long currentMilli = System.currentTimeMillis();
		
		KakaoChatbotUserModel userModel = mapper.getChatbotUser(userId, mode);
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String target = "";
		// 성별 먼저.
		if(null == userModel) {
			mapper.initChatbotUser(userId, mode);
			setTutorName(userId, mode);
			resMap = ModelParseUtil.setResponseAskGenderTextCard();
			target = "GENDER";
		} else {
			String userNm = userModel.getUserNm();
			String gender = userModel.getGender();
			if(null == gender || "".equals(gender)) {
				resMap = ModelParseUtil.setResponseAskGenderTextCard();
				target = "GENDER";
			} else if(null == userNm || "".equals(userNm)) {
				String answer = "당신의 이름도 알려주세요.\n뭐라고 부르면 좋을까요? 별명도 좋습니다!\n당신의 이름을 10자 이내로 써주세요.";
				resMap = ModelParseUtil.setResponseSimpleText(answer, false);
				target = "USERNM";
				mapper.updateUserName(userId, mode, "dXNlck5t");
			}
		}
		
		long workoutMilli = System.currentTimeMillis();
		int runTime = (int) (workoutMilli - currentMilli);
		
		LOG.info("====================================================");
		System.out.println("RUNTIME => " + runTime);
		System.out.println("USERID ==> " + userId);
		System.out.println("BLOCK ===> " + target);
		System.out.println();
		LOG.info("====================================================");
		
		return resMap;
	}
	
	private void setTutorName(String userId, String mode) {
		String setNamePrompt = "스스로 이름을 지어보세요, 정확하게 이름만 말하세요.";
		List<HashMap<String, String>> inputs = promptUtil.getTutorSetNamePromptList(mode, setNamePrompt);
		String responseBody = BuildChatGPTAPIConnectUtil.buildChatGPTAPIConnection(inputs, DEFAULT_TIMEOUT_MS, "gpt-4o-mini");
		String tutorNm = ModelParseUtil.getParsingOutputText(responseBody);
		mapper.updateTutorName(userId, mode, tutorNm);
	}
}