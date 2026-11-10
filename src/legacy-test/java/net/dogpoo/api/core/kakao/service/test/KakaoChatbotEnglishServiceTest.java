package net.dogpoo.api.core.kakao.service.test;

import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.dogpoo.api.core.kakao.service.KakaoChatbotEnglishService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/context/application-context.xml")
public class KakaoChatbotEnglishServiceTest {
	
	private final static Logger logger = LoggerFactory.getLogger(KakaoChatbotEnglishServiceTest.class);
	@Autowired private KakaoChatbotEnglishService service;
	
	@BeforeClass
	public static void before() {
		System.setProperty("serverType", "dev");
		logger.info("ServerType Setting is Completed : " + System.getProperty("serverType"));
	}
	
	@Test
	public void test() {
		HashMap<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("id", "b1c3e4b41285d71d78c335fd68064415d570c80387bd810eb0507b1c2d1b9a3f3e");
		HashMap<String, Object> userReqMap = new HashMap<String, Object>();
		userReqMap.put("user", userMap);
		HashMap<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("userRequest", userReqMap);
//		paramMap.put("utterance", "안녕?");
		HashMap<String, Object> resMap = service.generateTutor(paramMap, "미니");
		System.out.println(resMap);
	}
	
//	@Test
	public void test2() {
		String mode = "TUTOR";
		String question = "안녕?";
		String modelNm = "gpt-4o-mini";
		String answer = service.callChatGPT(mode, question, 10000, modelNm);
		System.out.println(answer);
	}
}