package net.dogpoo.api.core.kakao.controller;

import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.kakao.service.KakaoChatbotEnglishService;

@RestController
@RequestMapping("/kakao/chatbot/english")
public class KakaoChatbotEnglishController {

	@Autowired KakaoChatbotEnglishService service;
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	private static final String FIXED_TOKEN = System.getenv("KAKAO_WEBHOOK_TOKEN");
	
	@RequestMapping(value = "/getEngTeacherAnswer", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getAnswer(HttpServletRequest req, @RequestBody HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String token = req.getHeader("token");
		if(!FIXED_TOKEN.equals(token)) {
			resMap.put("code", 9);
			resMap.put("msg", "NOT_AUTHENTICATED");
			return resMap;
		}
		
		resMap = service.getChatbotEngTeacherAnswer(paramMap);
		return resMap;
	}

//	@RequestMapping(value = "/getWelcomeOutput", method = RequestMethod.POST, produces = JSON_UTF8)
//	public HashMap<String, Object> getWelcomeOutput(HttpServletRequest req, @RequestBody HashMap<String, Object> paramMap) {
//		HashMap<String, Object> resMap = new HashMap<String, Object>();
//		
//		String token = req.getHeader("token");
//		if(!FIXED_TOKEN.equals(token)) {
//			resMap.put("code", 9);
//			resMap.put("msg", "NOT_AUTHENTICATED");
//			return resMap;
//		}
//		
//		return service.getChatbotWelcomeOutput(paramMap);
//	}
	
	@RequestMapping(value = "/removeWholeMemory", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> removeWholeMemory(HttpServletRequest req, @RequestBody HashMap<String, Object> paramMap) {
//		System.out.println(paramMap);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String token = req.getHeader("token");
		if(!FIXED_TOKEN.equals(token)) {
			resMap.put("code", 9);
			resMap.put("msg", "NOT_AUTHENTICATED");
			return resMap;
		}
		
		return service.removeChatbotWholeMemory(paramMap);
	}
	
	@RequestMapping(value = "/getTestNode", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getTestNode(HttpServletRequest req, @RequestBody HashMap<String, Object> paramMap) {
		System.out.println(paramMap);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String token = req.getHeader("token");
		if(!FIXED_TOKEN.equals(token)) {
			resMap.put("code", 9);
			resMap.put("msg", "NOT_AUTHENTICATED");
			return resMap;
		}
		
		return service.getChatbotEngTeacherAnswer(paramMap);
	}
}