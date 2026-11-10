package net.dogpoo.api.core.kakao.service;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.kakao.mapper.TutorWelcomeBlockMapper;
import net.dogpoo.api.core.kakao.service.util.ModelParseUtil;

@Service
public class TutorWelcomeBlockService {
	
	@Autowired TutorWelcomeBlockMapper mapper;
	@Autowired KakaoChatbotEnglishService chatbot;
	
	public HashMap<String, Object> updateUserGender(HashMap<String, Object> paramMap) {
		HashMap<String, String> params = ModelParseUtil.getRequestParams(paramMap);
		String userId = params.get("userId");
		String mode = params.get("mode");
		String gender = params.get("gender");
		mapper.updateUserGender(userId, mode, gender);
		return chatbot.getChatbotEngTeacherAnswer(paramMap);
	}
}