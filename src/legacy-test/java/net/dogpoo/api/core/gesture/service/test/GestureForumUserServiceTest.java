package net.dogpoo.api.core.gesture.service.test;

import java.util.HashMap;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;

import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.dogpoo.api.core.gesture.service.GestureForumUserService;
import net.dogpoo.api.core.user.service.util.UserServiceUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/context/application-context.xml")
public class GestureForumUserServiceTest {

	private final static Logger logger = LoggerFactory.getLogger(GestureForumUserServiceTest.class);
	@Autowired private GestureForumUserService service;
	@Autowired private UserServiceUtil util;
	
	@BeforeClass
	public static void before() {
		System.setProperty("serverType", "dev");
		logger.info("ServerType Setting is Completed : " + System.getProperty("serverType"));
	}
	
//	@Test
	public void test() {
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String encodedPassword = passwordEncoder.encode("test12!");
		System.out.println(encodedPassword);
	}
	
//	@Test
	public void test1() {
		HashMap<String, Object> paramMap = new HashMap<String, Object>();
		service.signUp(paramMap);
	}
	
	@Test
	public void mailTest() throws AddressException, MessagingException, ParseException {
//		String userId = "igisug12@gmail.com";
		String userId = "igisug12@hotmail.com";
//		String userId = "igisug12@naver.com";
//		String userId = "help@dogpoo.net";
		String userWaitCd = "igisug12@hotmail.com";
		boolean isC = util.sendSignUpMail(userId, userWaitCd);
		System.out.println(isC);
	}
}
