package net.dogpoo.api.core.test.service.test;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.dogpoo.api.core.test.service.DogpooConnectionTestService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/context/application-context.xml")
public class DogpooConnectionTestServiceTest {
	
	private static final Logger logger = LoggerFactory.getLogger(DogpooConnectionTestServiceTest.class);

	@Autowired private DogpooConnectionTestService service;
	
	@BeforeClass
	public static void before() {
		System.setProperty("serverType", "dev");
		logger.info("ServerType Setting is Completed : " + System.getProperty("serverType"));
	}
	
	@Test
	public void test() throws ParseException {
		String body = "{\"isUse\":true}";
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(body);
		HashMap<String, Object> paramMap = root;
		String result = service.getPostGestureBody(paramMap);
		System.out.println(result);
	}

}
