package net.dogpoo.api.core.user.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.user.service.UserService;
import net.dogpoo.api.core.user.service.util.UserServiceUtil;

@RestController
@RequestMapping("/user/common")
public class UserController {
	
	@Autowired UserService service;
	@Autowired UserServiceUtil serviceUtil;
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	
	@RequestMapping(value = "/authMyPage", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> authMyPage(@RequestBody HashMap<String, Object> paramMap) throws Exception {
		HashMap<String, Object> resMap = new HashMap<String, Object>();

		String username = String.valueOf(paramMap.get("username"));
		String password = String.valueOf(paramMap.get("password"));
		int authStat = service.authenticate(username, password);
		if(authStat == 0) {
			@SuppressWarnings("unused")
			UserDetails userDetails = service.loadUserByUsername(username);
			resMap.put("code", 0);
			resMap.put("msg", "OK");
		} else if(authStat == 301) {
			resMap.put("code", 1);
			resMap.put("msg", "The password is wrong.");
		} else {
			resMap.put("code", 9);
			resMap.put("msg", "Error during authentication.");
		}
		return resMap;
	}
	
	@RequestMapping(value = "/getMailTest", method = RequestMethod.GET, produces = JSON_UTF8)
	public HashMap<String, Object> getMailTest(@RequestParam("userId") String userId) {
		boolean isC = serviceUtil.sendSignUpMail(userId, "Test");
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("result", isC);
		return resMap;
	}
}