package net.dogpoo.api.core.kakao.service.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.dogpoo.api.core.kakao.service.ChatGPTSummaryScheduleService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/context/application-context.xml")
public class ChatGPTSummaryScheduleServiceTest {

	private final static Logger logger = LoggerFactory.getLogger(ChatGPTSummaryScheduleServiceTest.class);
	@Autowired private ChatGPTSummaryScheduleService service;
	
	@BeforeClass
	public static void before() {
		System.setProperty("serverType", "dev");
		logger.info("ServerType Setting is Completed : " + System.getProperty("serverType"));
	}
	
	@Test
	public void test() {
		service.insertSummaryHistoryData(10);
//		service.insertLongtermMemory(5);
	}
}