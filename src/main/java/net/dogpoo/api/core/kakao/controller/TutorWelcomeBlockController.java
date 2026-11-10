package net.dogpoo.api.core.kakao.controller;

import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.kakao.service.TutorWelcomeBlockService;

@RestController
@RequestMapping("/kakao/chatbot/welcome")
public class TutorWelcomeBlockController {

	@Autowired TutorWelcomeBlockService service;
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	private static final String FIXED_TOKEN = System.getenv("KAKAO_WEBHOOK_TOKEN");
	
	@RequestMapping(value = "/setUserGender", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> setUserGender(HttpServletRequest req, @RequestBody HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String token = req.getHeader("token");
		if(!FIXED_TOKEN.equals(token)) {
			resMap.put("code", 9);
			resMap.put("msg", "NOT_AUTHENTICATED");
			return resMap;
		}
		
		return service.updateUserGender(paramMap);
	}
}