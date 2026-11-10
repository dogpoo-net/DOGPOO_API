package net.dogpoo.api.core.gesture.controller;

import java.io.IOException;
import java.util.HashMap;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.common.util.auth.JwtTokenUtil;
import net.dogpoo.api.common.util.auth.UserUtil;
import net.dogpoo.api.core.gesture.service.GestureForumUserService;
import net.dogpoo.api.core.user.model.UserModel;
import net.dogpoo.api.core.user.service.UserService;

@RestController
@RequestMapping("/gesture/forum/user")
public class GestureForumUserController {

    public GestureForumUserController(
            GestureForumUserService service,
            UserService userService,
            JwtTokenUtil jwtTokenUtil) {
        this.service = service;
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	private static final String AUTHORIZATION = "Authorization";
	private static final String TARGET = "GESTURE";
	
	private final GestureForumUserService service;
	private final UserService userService;
	private final JwtTokenUtil jwtTokenUtil;
	
	@RequestMapping(value = "/checkToken", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> checkToken(@RequestHeader(value = AUTHORIZATION) String authorization) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) {
			resMap.put("code", 1);
			resMap.put("msg", "EXPIRED_KEY");
		} else {
			resMap.put("code", 0);
			resMap.put("msg", "OK");
		}
		return resMap;
	}
	
	@RequestMapping(value = "/getGoogleOauthUserInfo", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getGoogleOauthUserInfo(@RequestBody HashMap<String, Object> paramMap) throws IOException {
		return service.getGoogleOauthUserInfo(paramMap);
	}
	
	@RequestMapping(value = "/signUp", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> signUp(@RequestBody HashMap<String, Object> paramMap) {
		return service.signUp(paramMap);
	}
	
	@RequestMapping(value = "/signUpComplete", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> signUpComplete(@RequestBody HashMap<String, Object> paramMap) {
		return service.signUpComplete(paramMap);
	}
	
	@RequestMapping(value = "/updateMyPage", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> updateMyPage(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		String countryCd = paramMap.get("countryCd").toString();
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(countryCd);
		return service.updateMyPage(userKey, paramMap);
	}
	
	@RequestMapping(value = "/withdrawal", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> withdrawal(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		String countryCd = paramMap.get("countryCd").toString();
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(countryCd);
		return service.doWithdrawal(userKey, paramMap);
	}
	
	@RequestMapping(value = "/initPassSendMail", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> initPassSendMail(@RequestBody HashMap<String, Object> paramMap) {
		return service.initPassSendMail(paramMap);
	}
	
	@RequestMapping(value = "/initPassCodeAuth", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> initPassCodeAuth(@RequestBody HashMap<String, Object> paramMap) {
		return service.initPassCodeAuth(paramMap);
	}
	
	@RequestMapping(value = "/initPassword", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> initPassword(@RequestBody HashMap<String, Object> paramMap) {
		return service.initPassword(paramMap);
	}
	
	@RequestMapping(value = "/getCountryCd", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getCountryCd(@RequestBody HashMap<String, Object> paramMap) {
		return service.getCountryCd(paramMap);
	}
	
	// 로그인(JWT TOKEN 생성)
	// PUBLIC OPEN
	@RequestMapping(value = "/authLogin", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> createAuthenticationToken(@RequestBody HashMap<String, Object> paramMap) throws Exception {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		String username = String.valueOf(paramMap.get("username"));
		String password = String.valueOf(paramMap.get("password"));
		
		String userInput = TARGET + " :: " + username;
		
		int authStat = userService.authenticate(userInput, password);
		
		if (authStat == 0) {
			UserDetails userDetails = userService.loadUserByUsername(userInput);
			UserModel userModel = userService.getUserModel(userInput);
			String userType = userModel.getUserType();
			if("WAIT".equals(userType)) {
				resMsg = "KR".equals(countryCd) ? "가입 승인 대기 중이에요." : "You are waiting to join.";
				
				resMap.put("code", 1);
				resMap.put("msg", resMsg);
				userDetails = null;
				return resMap;
			} else if("WITHDRAW".equals(userType)) {
				resMsg = "KR".equals(countryCd) ? "탈퇴한 회원입니다. 다른 계정을 이용해주세요." : "Member who left. Please use a different account.";
				
				resMap.put("code", 4);
				resMap.put("msg", resMsg);
				userDetails = null;
				return resMap;
			}
			String userKey = userModel.getCcode();
			String nickName = userModel.getUserNm();
			if(null != paramMap.get("userUuid")) {
				String userUuid = paramMap.get("userUuid").toString();
				service.updateUserUuid(userKey, userUuid);
			}
			String token = jwtTokenUtil.generateToken(userDetails, userKey, nickName);
			resMap.put("code", 0);
			resMap.put("msg", "OK");
			resMap.put("jwtToken", token);
			resMap.put("DATA", userModel);
		} else if (authStat == 301) {
			resMsg = "KR".equals(countryCd) ? "비밀번호가 틀립니다." : "The password is wrong.";
			
			resMap.put("code", 2);
			resMap.put("msg", resMsg);
		} else if (authStat == 404) {
			resMsg = "KR".equals(countryCd) ? "사용자 정보를 찾을 수 없어요." : "Nothing user data.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
		} else {
			resMap.put("code", 9);
			resMap.put("msg", "RESULT_AUTH_ERROR");
		}
		return resMap;
	}

	// 로그인(JWT TOKEN 생성)
	// PUBLIC OPEN
	@RequestMapping(value = "/authUuid", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> createUuidAuthenticationToken(@RequestBody HashMap<String, Object> paramMap) throws Exception {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String siteType = paramMap.get("siteType").toString();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		HashMap<String, Object> authMap = new HashMap<String, Object>();
		if(null != paramMap.get("userUuid") && null != paramMap.get("isSocialLogin")) {	// UUID 를 갖고 있으면서 최초 소셜 로그인을 시도할 때.
			String userUuid = String.valueOf(paramMap.get("userUuid"));
			if(!UserUtil.validateUuid(userUuid)) {
				resMsg = "KR".equals(countryCd) ? "UUID 형식이 아닙니다." : "Not in UUID format.";
				
				resMap.put("code", 2);
				resMap.put("msg", resMsg);
				return resMap;
			};
			
			String userId = String.valueOf(paramMap.get("username"));
			String snsSignId = String.valueOf(paramMap.get("oauthSignId"));
			authMap = userService.authenticateDirect(userId, snsSignId);
		} else if(null != paramMap.get("userUuid")) {	// UUID로 로그인 시도할 때.
			String userUuid = String.valueOf(paramMap.get("userUuid"));
			if(!UserUtil.validateUuid(userUuid)) {
				resMsg = "KR".equals(countryCd) ? "UUID 형식이 아닙니다." : "Not in UUID format.";
				
				resMap.put("code", 2);
				resMap.put("msg", resMsg);
				return resMap;
			};
			authMap = userService.authenticateDirect(userUuid);
		} else if(null != paramMap.get("isSocialLogin")) {	// 소셜 로그인 시도할 때.
			String userId = String.valueOf(paramMap.get("username"));
			String snsSignId = String.valueOf(paramMap.get("oauthSignId"));
			authMap = userService.authenticateDirect(userId, snsSignId);
		}
		
		int authStat = (int) authMap.get("resultCd");
		if (authStat == 0) {
			String username = siteType + " :: " + String.valueOf(authMap.get("username"));
			UserDetails userDetails = userService.loadUserByUsername(username);
			UserModel userModel = userService.getUserModel(username);
			String userType = userModel.getUserType();
			if("WAIT".equals(userType)) {
				resMsg = "KR".equals(countryCd) ? "가입 승인 대기 중이에요." : "You are waiting to join.";
				
				resMap.put("code", 1);
				resMap.put("msg", resMsg);
				userDetails = null;
				return resMap;
			} else if("WITHDRAW".equals(userType)) {
				resMsg = "KR".equals(countryCd) ? "탈퇴한 회원입니다. 다른 계정을 이용해주세요." : "Member who left. Please use a different account.";
				
				resMap.put("code", 4);
				resMap.put("msg", resMsg);
				userDetails = null;
				return resMap;
			}
			String userKey = userModel.getCcode();
			String nickName = userModel.getUserNm();
			if(null != paramMap.get("userUuid")) {
				String userUuid = paramMap.get("userUuid").toString();
				service.updateUserUuid(userKey, userUuid);
			}
			String token = jwtTokenUtil.generateToken(userDetails, userKey, nickName);
			resMap.put("code", 0);
			resMap.put("msg", "OK");
			resMap.put("jwtToken", token);
			resMap.put("DATA", userModel);
		} else if (authStat == 301) {
			resMsg = "KR".equals(countryCd) ? "비밀번호가 틀립니다." : "The password is wrong.";
			
			resMap.put("code", 2);
			resMap.put("msg", resMsg);
		} else if (authStat == 401 || authStat == 404) {
			resMsg = "KR".equals(countryCd) ? "처음이시라면 회원가입을 먼저 진행해주세요." : "If it's your first time, please register as a member.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
		} else {
			resMap.put("code", 9);
			resMap.put("msg", "RESULT_AUTH_ERROR");
		}
		return resMap;
		
//		public static final int SUCCESS_RESULT = 0;
//		public static final int ALREADY_RESULT = 201;
//		public static final int NO_DATA_RESULT = 301;
//		public static final int ERROR_RESULT = 401;
//		public static final int JWT_KEY_PARSING_ERROR_RESULT = 402;
//		public static final int NO_PARAMETER_RESULT = 501;
	}
	
	private HashMap<String, Object> getExpiredTokenResultMap(String countryCd) {
		HashMap<String, Object> keyResMap = new HashMap<String, Object>();
		String resMsg = "KR".equals(countryCd) ? "세션이 만료되었습니다. 다시 로그인해주세요." : "Expired session! Please log in again.";
		
		keyResMap.put("code", 6);
		keyResMap.put("msg", resMsg);
		return keyResMap;
	}
	
	@RequestMapping(value = "/countVisitor", method = RequestMethod.POST, produces = JSON_UTF8)
	public void countVisitor() {
		service.countVisitor();
	}
	
	@RequestMapping(value = "/getVisitorCount", method = RequestMethod.GET)
	public HashMap<String, Object> getVisitorCount() {
		return service.getVisitorCount();
	}
}