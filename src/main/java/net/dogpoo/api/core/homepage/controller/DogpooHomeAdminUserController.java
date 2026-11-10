package net.dogpoo.api.core.homepage.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.common.util.auth.JwtTokenUtil;
import net.dogpoo.api.core.user.model.UserModel;
import net.dogpoo.api.core.user.service.UserService;

@RestController
@RequestMapping("/api/home/admin/user")
public class DogpooHomeAdminUserController {
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	private static final String TARGET = "DOGPOO_HOME";
	@Autowired UserService service;
	@Autowired JwtTokenUtil jwtTokenUtil;
	
	// 로그인(JWT TOKEN 생성)
	// PUBLIC OPEN
	@RequestMapping(value = "/authLogin", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> createHomeAuthenticationToken(@RequestBody HashMap<String, Object> paramMap) throws Exception {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String resMsg = "";
		String username = String.valueOf(paramMap.get("username"));
		String password = String.valueOf(paramMap.get("password"));
		
		String userInput = TARGET + " :: " + username;
		
		System.out.println("로그인 시도... => " + username);
		int authStat = service.authenticate(userInput, password);
		if (authStat == 0) {
			UserDetails userDetails = service.loadUserByUsername(userInput);
			UserModel userModel = service.getUserModel(userInput);
			String userType = userModel.getUserType();
			if("WAIT".equals(userType)) {
				resMsg = "가입 승인 대기 중이에요.";
				
				resMap.put("code", 1);
				resMap.put("msg", resMsg);
				userDetails = null;
				return resMap;
			} else if("WITHDRAW".equals(userType)) {
				resMsg = "탈퇴한 회원입니다. 다른 계정을 이용해주세요.";
				
				resMap.put("code", 4);
				resMap.put("msg", resMsg);
				userDetails = null;
				return resMap;
			} else if("BAN".equals(userType)) {
				resMsg = "권한이 없는 계정입니다. 고객센터에 문의하세요.";
				
				resMap.put("code", 4);
				resMap.put("msg", resMsg);
				userDetails = null;
				return resMap;
			}
			String userKey = userModel.getCcode();
			String nickName = userModel.getUserNm();
			String token = jwtTokenUtil.generateToken(userDetails, userKey, nickName);
			resMap.put("code", 0);
			resMap.put("msg", "OK");
			resMap.put("jwtToken", token);
			resMap.put("DATA", userModel);
			System.out.println("로그인 됨 => " + username);
		} else if (authStat == 301) {
			resMsg = "비밀번호가 틀립니다.";
			
			resMap.put("code", 2);
			resMap.put("msg", resMsg);
		} else if (authStat == 404) {
			resMsg = "사용자 정보를 찾을 수 없어요.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
		} else {
			resMap.put("code", 9);
			resMap.put("msg", "RESULT_AUTH_ERROR");
		}
		return resMap;
	}
}