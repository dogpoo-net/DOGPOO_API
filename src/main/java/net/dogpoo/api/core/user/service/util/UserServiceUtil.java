package net.dogpoo.api.core.user.service.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import net.dogpoo.api.common.util.MailSendUtil;
import net.dogpoo.api.core.user.mapper.UserMapper;

@Component
public class UserServiceUtil {

    public UserServiceUtil(UserMapper mapper) {
        this.mapper = mapper;
    }

	
	private final UserMapper mapper;
	
	public boolean sendInitPassMail(String userId, String initPassCode) {
		HashMap<String, String> mailBodyMap = mapper.getMailPreset("INIT_PASS_CONFIRM_EN");
		String subject = mailBodyMap.get("subject");
		
		// ###### 손대지 말것
		String body = mailBodyMap.get("body").replace("######", initPassCode);
		List<String> ja = new ArrayList<String>();
		ja.add(userId);
		boolean isMailSend = MailSendUtil.mailSender(ja, subject, body);
		
		return isMailSend;
	}
	
	public boolean sendSignUpMail(String userId, String userWaitCd) {
		HashMap<String, String> mailBodyMap = mapper.getMailPreset("SIGN_UP_CONFIRM_EN");
		String subject = mailBodyMap.get("subject");
		
		// ###### 손대지 말것
		String body = mailBodyMap.get("body").replace("######", userWaitCd).replace("$$$$$$", userId);
		List<String> ja = new ArrayList<String>();
		ja.add(userId);
		boolean isMailSend = MailSendUtil.mailSender(ja, subject, body);
		
		return isMailSend;
	}

}
