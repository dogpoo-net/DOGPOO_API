package net.dogpoo.api.core.homepage.service.test;

import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.dogpoo.api.core.homepage.service.DogpooHomePublicAPIService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/context/application-context.xml")
public class DogpooHomePublicAPIServiceTest {
	
	private static final Logger logger = LoggerFactory.getLogger(DogpooHomePublicAPIServiceTest.class);

	@Autowired private DogpooHomePublicAPIService service;
	
	@BeforeClass
	public static void before() {
		System.setProperty("serverType", "dev");
		logger.info("ServerType Setting is Completed : " + System.getProperty("serverType"));
	}
	
	@Test
	public void test() {
		HashMap<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("contactType", "비즈니스 문의");
		paramMap.put("contacter", "TEST");
		paramMap.put("contacterEmail", "igisug12@naver.com");
		paramMap.put("contacterPhone", "01046467979");
		paramMap.put("contacterUrl", "https://www.youtube.com/channel/UCnQwhSNL1O4cpgtxyvpfg2A");
		paramMap.put("contactMessage", "테스트 문의내용입니다.\n답변을 달지 마세요.");
		
		service.insertContactMessage(paramMap);
	}
}