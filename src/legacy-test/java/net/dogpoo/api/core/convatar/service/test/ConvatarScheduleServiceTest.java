package net.dogpoo.api.core.convatar.service.test;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.dogpoo.api.core.convatar.service.ConvatarScheduleService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/context/application-context.xml")
public class ConvatarScheduleServiceTest {
	
	private static final Logger logger = LoggerFactory.getLogger(ConvatarScheduleServiceTest.class);

	@Autowired private ConvatarScheduleService service;
	
	@BeforeClass
	public static void before() {
		System.setProperty("serverType", "dev");
		logger.info("ServerType Setting is Completed : " + System.getProperty("serverType"));
	}
	
	@Test
	public void test() throws IOException, ParseException {
		service.executeCurrencyScheduleMain();
	}
}
