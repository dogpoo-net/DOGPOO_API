package net.dogpoo.api.core.test.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import net.dogpoo.api.common.util.MailSendUtil;

@Service
public class DogpooMailTestService {

	public void sendMailTest() {
		List<String> toList = new ArrayList<String>();
		toList.add("igisug12@naver.com");
		String subject = "test12";
		String body = "TEST12";
		MailSendUtil.mailSender(toList, subject, body);
	}
	
}
