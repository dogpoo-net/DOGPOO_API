package net.dogpoo.api.core.gesture.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import net.dogpoo.api.common.util.IpUtil;
import net.dogpoo.api.common.util.MailSendUtil;
import net.dogpoo.api.common.util.auth.JwtTokenUtil;
import net.dogpoo.api.common.util.auth.UserUtil;
import net.dogpoo.api.core.gesture.mapper.GestureForumUserMapper;
import net.dogpoo.api.core.gesture.service.util.GestureForumGoogleOauthUtil;
import net.dogpoo.api.core.user.model.UserModel;
import net.dogpoo.api.core.user.service.UserService;
import net.dogpoo.api.core.user.service.util.UserServiceUtil;

@Service
public class GestureForumUserService {

	@Autowired GestureForumUserMapper mapper;
	@Autowired AuthenticationManager authenticationManager;
	@Autowired PasswordEncoder bcryptEncoder;
	@Autowired UserServiceUtil userServiceUtil;
	@Autowired UserService userService;
	@Autowired JwtTokenUtil jwtTokenUtil;
	
	private final static Logger logger = LoggerFactory.getLogger(GestureForumUserService.class);

	Gson gson = new Gson();
	
	public HashMap<String, Object> getGoogleOauthUserInfo(HashMap<String, Object> paramMap) throws IOException {
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		HashMap<String, String> userInfoMap = GestureForumGoogleOauthUtil.getOauthToken(paramMap);
		
		String userId = userInfoMap.get("email");
		String snsSignId = userInfoMap.get("id");
		
		boolean isSignIn = mapper.isSocialSignIn(userId, snsSignId);
		
		logger.info("Logined from GESTURE FORUM :: " + userId);
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", userInfoMap);
		resMap.put("isSignIn", isSignIn);
		return resMap;
	}
	
	public HashMap<String, Object> signUp(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String userId = paramMap.get("username").toString();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		if(userId.length() > 30) {
			resMsg = "KR".equals(countryCd) ? "계정은 30자 이내로 입력해주십시오." : "Account is too long.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		int duplicateUserIdCase = mapper.duplicateCheckUserId(userId);
		if(duplicateUserIdCase > 0) {
			if(duplicateUserIdCase == 1) {
				resMsg = "KR".equals(countryCd) ? "이미 존재하는 이메일 주소." : "Email already exists.";
			} else if(duplicateUserIdCase == 2) {
				resMsg = "KR".equals(countryCd) ? "탈퇴한 회원입니다." : "Member who left.";
			}
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		boolean isValidEmail = MailSendUtil.isValidEmail(userId);
		if(!isValidEmail) {
			resMsg = "KR".equals(countryCd) ? "이메일 형식이 아닙니다." : "This is not an email format.";
			
			resMap.put("code", 2);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		if(null == paramMap.get("nickname")) {
			resMsg = "KR".equals(countryCd) ? "이름을 입력하여 주십시오." : "No name was entered.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		
		String userNm = paramMap.get("nickname").toString();
		boolean isSpecialLetter = UserUtil.validCheckSpecialLetters(userNm);
		if(isSpecialLetter) {
			resMsg = "KR".equals(countryCd) ? "특수문자는 사용할 수 없습니다." : "Special characters are not allowed.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		if(userNm.length() < 2) {
			resMsg = "KR".equals(countryCd) ? "이름이 너무 짧습니다." : "The name is too short.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		} else if(userNm.length() > 20) {
			resMsg = "KR".equals(countryCd) ? "이름은 20자 이내로 입력하여 주십시오." : "The name is too long.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		boolean isDuplicateUserNm = mapper.duplicateCheckUserNm(userNm);
		if(isDuplicateUserNm) {
			resMsg = "KR".equals(countryCd) ? "이미 존재하는 이름." : "Name already exists.";
			
			resMap.put("code", 4);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
//		String userNm = "";
//		for(int i = 0; i < 100; i++) {
//			userNm = String.format("%08d", (int) (Math.random() * 99999999));
//			boolean isDuplicateUserNm = mapper.duplicateCheckUserNm(userNm);
//			if(!isDuplicateUserNm) {
//				break;
//			}
//		}
		
		String userUuid = null;
		if(null != paramMap.get("userUuid")) {
			userUuid = paramMap.get("userUuid").toString();
		}
		
		UserModel userModel = new UserModel();
		
		boolean isSocialLogin = false;
		if(null != paramMap.get("isSocialLogin")) {
			isSocialLogin = (boolean) paramMap.get("isSocialLogin");
		}
		
		if(isSocialLogin) {
			String socialType = paramMap.get("socialType").toString();
			String oauthSignId = paramMap.get("oauthSignId").toString();
			String picture = paramMap.get("picture").toString();
			userModel.setSocialType(socialType);
			userModel.setSnsSignId(oauthSignId);
			userModel.setPicture(picture);
			userModel.setUserType("NORMAL");
		}
		
		userModel.setUserId(userId);
		userModel.setUserNm(userNm);
		userModel.setUserUuid(userUuid);
		userModel.setCountryCd(countryCd);
		
		String socialType = null == paramMap.get("socialType") ? "" : paramMap.get("socialType").toString();
		userModel.setSocialType(socialType);
		String password = paramMap.get("password").toString();
		String passwordConfirm = paramMap.get("passwordConfirm").toString();
		if(!password.equals(passwordConfirm)) {
			resMsg = "KR".equals(countryCd) ? "비밀번호가 다릅니다. 다시 입력해주세요." : "The password is wrong. Please re-enter.";
			
			resMap.put("code", 4);
			resMap.put("msg", resMsg);
			return resMap;
		}
		if(password.length() < 6) {
			resMsg = "KR".equals(countryCd) ? "비밀번호가 너무 짧습니다." : "The password is too short.";
			
			resMap.put("code", 4);
			resMap.put("msg", resMsg);
			return resMap;
		} else if(password.length() > 20) {
			resMsg = "KR".equals(countryCd) ? "비밀번호는 20자 이내로 입력하여 주십시오." : "The password is too long.";
			
			resMap.put("code", 4);
			resMap.put("msg", resMsg);
			return resMap;
		}
		userModel.setPasswd(bcryptEncoder.encode(password));
		
		if(isSocialLogin) {
			int row = mapper.insertSignUpUser(userModel);
			if(row == 0) {
				resMsg = "KR".equals(countryCd) ? "소셜 회원가입 중 유저 정보 입력되지 못함." : "ERROR OCCURRED";
				
				resMap.put("code", 8);
				resMap.put("msg", resMsg);
			} else {
				resMap.put("code", 0);
				resMap.put("msg", "OK");
				resMap.put("isSocialLogin", true);
			}
			return resMap;
		}
		
		userModel.setUserType("WAIT");
		String userWaitCd = UUID.randomUUID().toString().replace("-", "");
		userModel.setUserWaitCd(userWaitCd);
		
		boolean isMailSuccess = userServiceUtil.sendSignUpMail(userId, userWaitCd);
		if(isMailSuccess) {
			int row = mapper.insertSignUpUser(userModel);
			if(row == 0) {
				resMsg = "KR".equals(countryCd) ? "메일을 발신하였으나 유저정보를 입력하지 못함." : "ERROR OCCURRED";
				
				resMap.put("code", 8);
				resMap.put("msg", "ERROR OCCURRED");
				return resMap;
			}
		} else {
			resMsg = "KR".equals(countryCd) ? "확인 메일 발신 실패." : "FAILED MAIL SEND";
			
			resMap.put("code", 7);
			resMap.put("msg", "FAILED MAIL SEND");
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("isSocialLogin", false);
		return resMap;
	}
	
	public HashMap<String, Object> signUpComplete(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		String signUpKey = paramMap.get("signUpKey").toString();
		String userId = paramMap.get("userId").toString();
		String userWaitCd = mapper.getUserWaitCd(userId);
		if(null == userWaitCd) {
			resMsg = "KR".equals(countryCd) ? "확인 코드를 찾을 수 없네요. 다시 시도해주세요." : "Verified code is not find.";
			
			resMap.put("code", 8);
			resMap.put("msg", resMsg);
		} else if(signUpKey.equals(userWaitCd)) {
			resMsg = "KR".equals(countryCd) ? "가입이 승인되었습니다. 포럼에서 로그인해주세요!" : "Your membership has been completed. Log in from the Gesture forum.";
			
			resMap.put("code", 0);
			resMap.put("msg", resMsg);
			mapper.updateSignUpComplete(userId);
		} else {
			resMsg = "KR".equals(countryCd) ? "확인 코드가 틀립니다. 다시 시도해주세요." : "The keys are not the same.";
			
			resMap.put("code", 7);
			resMap.put("msg", resMsg);
		}
		
		return resMap;
	}
	
	public HashMap<String, Object> updateMyPage(String userKey, HashMap<String, Object> paramMap) {
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		if("EXPIRED_KEY".equals(userKey)) {
			HashMap<String, Object> keyResMap = new HashMap<String, Object>();
			resMsg = "KR".equals(countryCd) ? "세션 만료." : "EXPIRED_SESSION";
			
			keyResMap.put("code", 6);
			keyResMap.put("msg", resMsg);
			return keyResMap;
		}
		paramMap.put("ccode", userKey);
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		if((null == paramMap.get("nickname") || "".equals(paramMap.get("nickname"))) && (null == paramMap.get("password") || "".equals(paramMap.get("password")))) {
			resMsg = "KR".equals(countryCd) ? "이름을 입력해 주세요." : "Please enter your name.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		boolean isSpecialLetter = UserUtil.validCheckSpecialLetters(paramMap.get("nickname").toString());
		if(isSpecialLetter) {
			resMsg = "KR".equals(countryCd) ? "특수문자는 사용할 수 없습니다." : "Special characters are not allowed.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		boolean isDuplicateUserNm = mapper.updateMyPageCheckUserNm(paramMap);
		if(isDuplicateUserNm) {
			resMsg = "KR".equals(countryCd) ? "이미 존재하는 이름입니다." : "Name already exists.";
			
			resMap.put("code", 4);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		int row = 0;
		String nickname = "";
		boolean isReturnNm = false; // 네임을 변경 했을 때! 리턴 시켜서 뷰에 반영 시켜줘야함!
		if(null == paramMap.get("password") || "".equals(paramMap.get("password"))) {
			// 네임 변경만
			if(null != paramMap.get("passwordConfirm") && !"".equals(paramMap.get("passwordConfirm"))) {
				resMsg = "KR".equals(countryCd) ? "비밀번호를 입력해주세요." : "Please enter the password.";
				
				resMap.put("code", 2);
				resMap.put("msg", resMsg);
				return resMap;
			}
			nickname = paramMap.get("nickname").toString();
			
			if(nickname.length() < 2) {
				resMsg = "KR".equals(countryCd) ? "이름이 너무 짧습니다." : "The name is too short.";
				
				resMap.put("code", 3);
				resMap.put("msg", resMsg);
				return resMap;
			} else if(nickname.length() > 20) {
				resMsg = "KR".equals(countryCd) ? "이름은 20자 이내로 입력해주세요." : "The name is too long.";
				
				resMap.put("code", 3);
				resMap.put("msg", resMsg);
				return resMap;
			}
			
			boolean isSameNm = mapper.duplicateCheckUserNm(nickname);
			if(isSameNm) {
				resMsg = "KR".equals(countryCd) ? "입력하신 이름은 기존 값과 똑같습니다." : "The value has not changed.";
				
				resMap.put("code", 3);
				resMap.put("msg", resMsg);
				return resMap;
			}
			row = mapper.updateMyPageOnlyUserNm(paramMap);
			isReturnNm = true;
		} else {
			// 비밀번호 변경까지
			String password = paramMap.get("password").toString();
			if(null == paramMap.get("passwordConfirm") || "".equals(paramMap.get("passwordConfirm"))) {
				resMsg = "KR".equals(countryCd) ? "비밀번호를 입력해주세요." : "Please enter the password.";
				
				resMap.put("code", 2);
				resMap.put("msg", resMsg);
				return resMap;
			}
			
			String passwordConfirm = paramMap.get("passwordConfirm").toString();
			if(!password.equals(passwordConfirm)) {
				resMsg = "KR".equals(countryCd) ? "비밀번호가 서로 다릅니다. 다시 시도해주세요." : "The password is wrong. Please re-enter.";
				
				resMap.put("code", 4);
				resMap.put("msg", resMsg);
				return resMap;
			}
			if(password.length() < 6) {
				resMsg = "KR".equals(countryCd) ? "비밀번호가 너무 짧아요. 6자 이상 입력해주세요." : "The password is too short.";
				
				resMap.put("code", 4);
				resMap.put("msg", resMsg);
				return resMap;
			} else if(password.length() > 20) {
				resMsg = "KR".equals(countryCd) ? "비밀번호가 너무 길어요. 20자 이내로 입력해주세요." : "The password is too long.";
				
				resMap.put("code", 4);
				resMap.put("msg", resMsg);
				return resMap;
			}
			String encryptPassword = bcryptEncoder.encode(paramMap.get("password").toString());
			paramMap.put("passwd", encryptPassword);
			if(null == paramMap.get("nickname") || "".equals(paramMap.get("nickname"))) {
				row = mapper.updateMyPageOnlyPasswd(paramMap);
			} else {
				nickname = paramMap.get("nickname").toString();
				if(nickname.length() < 2) {
					resMsg = "KR".equals(countryCd) ? "이름이 너무 짧아요." : "The name is too short.";
					
					resMap.put("code", 3);
					resMap.put("msg", resMsg);
					return resMap;
				} else if(nickname.length() > 20) {
					resMsg = "KR".equals(countryCd) ? "이름이 너무 길어요. 20자 이내로 입력해주세요." : "The name is too long.";
					
					resMap.put("code", 3);
					resMap.put("msg", resMsg);
					return resMap;
				}
				row = mapper.updateMyPage(paramMap);
				isReturnNm = true;
			}
		}
		
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "아무것도 바뀌지 않았습니다." : "The value has not changed.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
		} else {
			resMsg = "KR".equals(countryCd) ? "수정 완료!" : "Updated.";
			
			resMap.put("code", 0);
			resMap.put("msg", resMsg);
			if(isReturnNm) {
				resMap.put("nickname", nickname);
			}
		}
		return resMap;
	}
	
	public HashMap<String, Object> doWithdrawal(String userKey, HashMap<String, Object> paramMap) {
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		if("EXPIRED_KEY".equals(userKey)) {
			resMsg = "KR".equals(countryCd) ? "세션 만료." : "EXPIRED_SESSION";
			
			HashMap<String, Object> keyResMap = new HashMap<String, Object>();
			keyResMap.put("code", 6);
			keyResMap.put("msg", resMsg);
			return keyResMap;
		}
		paramMap.put("ccode", userKey);
		
		mapper.deleteUserLikeHist(paramMap);
		int delRow = mapper.withdrawal(paramMap);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		if(delRow == 0) {
			resMsg = "KR".equals(countryCd) ? "유저 데이터가 없습니다." : "Nothing user data.";
			
			resMap.put("code", 9);
			resMap.put("msg", resMsg);
		} else {
			resMap.put("code", 0);
			resMap.put("msg", "OK");
		}
		return resMap;
	}
	
	public HashMap<String, Object> initPassSendMail(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		if(null == paramMap.get("userId") || "".equals(paramMap.get("userId"))) {
			resMsg = "KR".equals(countryCd) ? "계정을 입력해주세요." : "Please enter your account.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		String userId = paramMap.get("userId").toString();
		int existCase = mapper.duplicateCheckUserId(userId);
		
		if(existCase != 1) {
			resMsg = "KR".equals(countryCd) ? "계정을 찾을 수 없습니다.." : "Account not found..";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		String userType = mapper.getUserType(userId);
		if("WAIT".equals(userType)) {
			resMsg = "KR".equals(countryCd) ? "메일 인증을 먼저 완료해주세요." : "Please complete mail authentication first.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		String initPassCode = String.format("%06d", (int) (Math.random() * 999999));
		
		int row = mapper.updateUserWaitCd(initPassCode, userId);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "알 수 없는 오류." : "Unknown Error.";
			
			resMap.put("code", 9);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		boolean isSendMail = userServiceUtil.sendInitPassMail(userId, initPassCode);
		if(isSendMail) {
			resMap.put("code", 0);
			resMap.put("msg", "OK");
		} else {
			resMsg = "KR".equals(countryCd) ? "알 수 없는 오류." : "Unknown Error.";
			
			resMap.put("code", 9);
			resMap.put("msg", resMsg);
		}
		return resMap;
	}
	
	public HashMap<String, Object> initPassCodeAuth(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		if(null == paramMap.get("userId") || "".equals(paramMap.get("userId"))) {
			resMsg = "KR".equals(countryCd) ? "계정을 입력해주세요." : "Please enter your account.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		if(null == paramMap.get("initPassCode") || "".equals(paramMap.get("initPassCode"))) {
			resMsg = "KR".equals(countryCd) ? "코드를 입력해주세요." : "Please enter the code.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		String userId = paramMap.get("userId").toString();
		String inputInitPassCode = paramMap.get("initPassCode").toString();
		String realInitPassCode = mapper.getUserWaitCd(userId);
		
		if(inputInitPassCode.equals(realInitPassCode)) {
			resMap.put("code", 0);
			resMap.put("msg", "OK");
		} else {
			resMsg = "KR".equals(countryCd) ? "코드가 틀립니다." : "The code is wrong.";
			
			resMap.put("code", 8);
			resMap.put("msg", resMsg);
		}
		
		return resMap;
	}
	
	public HashMap<String, Object> initPassword(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		if(null == paramMap.get("userId") || "".equals(paramMap.get("userId"))) {
			resMsg = "KR".equals(countryCd) ? "계정을 입력해주세요." : "Please enter your account.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		if(null == paramMap.get("initPassCode") || "".equals(paramMap.get("initPassCode"))) {
			resMsg = "KR".equals(countryCd) ? "코드를 입력해주세요." : "Please enter the code.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		if(null == paramMap.get("password") || "".equals(paramMap.get("password")) || null == paramMap.get("passwordConfirm") || "".equals(paramMap.get("passwordConfirm"))) {
			resMsg = "KR".equals(countryCd) ? "변경할 비밀번호를 입력해주세요." : "Please enter the password you want to change.";
			
			resMap.put("code", 3);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		String password = paramMap.get("password").toString();
		String passwordConfirm = paramMap.get("passwordConfirm").toString();
		if(!password.equals(passwordConfirm)) {
			resMsg = "KR".equals(countryCd) ? "비밀번호가 서로 다릅니다. 다시 입력해주세요." : "The password is wrong. Please re-enter.";
			
			resMap.put("code", 4);
			resMap.put("msg", resMsg);
			return resMap;
		}
		if(password.length() < 6) {
			resMsg = "KR".equals(countryCd) ? "비밀번호가 너무 짧아요. 6자 이상 입력해주세요." : "The password is too short.";
			
			resMap.put("code", 4);
			resMap.put("msg", resMsg);
			return resMap;
		} else if(password.length() > 20) {
			resMsg = "KR".equals(countryCd) ? "비밀번호가 너무 길어요. 20자 이내로 입력해주세요." : "The password is too long.";
			
			resMap.put("code", 4);
			resMap.put("msg", resMsg);
			return resMap;
		}
		String userWaitCd = paramMap.get("initPassCode").toString();
		String passwd = bcryptEncoder.encode(password);
		paramMap.put("passwd", passwd);
		paramMap.put("userWaitCd", userWaitCd);
		
		int row = mapper.updateInitPassword(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "비밀번호 변경 실패." : "Password change failed.";
			
			resMap.put("code", 8);
			resMap.put("msg", resMsg);
		} else {
			resMap.put("code", 0);
			resMap.put("msg", "OK");
		}
		return resMap;
	}
	
	public HashMap<String, Object> getCountryCd(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		if(null == paramMap.get("clientIP")) {
			resMap.put("code", 9);
			resMap.put("msg", "ERROR OCCURRED");
			return resMap;
		}
		String ip = paramMap.get("clientIP").toString();
		String countryCd = IpUtil.getCountryCode(ip);
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("countryCd", countryCd);
		return resMap;
	}
	
	public int updateUserUuid(String ccode, String userUuid) {
		return mapper.updateUserUuid(ccode, userUuid);
	}
	
	public HashMap<String, Object> authLogin(HashMap<String, Object> paramMap) throws Exception {
		String target = "GESTURE";
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String countryCd = paramMap.get("countryCd").toString();
		String username = String.valueOf(paramMap.get("username"));
		String password = String.valueOf(paramMap.get("password"));
		
		String resMsg = "";
		UserModel initUserModel = mapper.getGestureUserInfoByUserId(username);
		
		int authStat = userService.authenticate(username, password);
		if (authStat == 0) {
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
				updateUserUuid(userKey, userUuid);
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
	
	public void countVisitor() {
		mapper.updateVisitorCount();
	}
	
	public HashMap<String, Object> getVisitorCount() {
		int count = mapper.getVisitorCount();
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", count);
		return resMap;
	}
}