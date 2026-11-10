package net.dogpoo.api.core.user.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.user.mapper.UserMapper;
import net.dogpoo.api.core.user.model.JwtRequest;
import net.dogpoo.api.core.user.model.UserModel;

@Service
public class UserService implements UserDetailsService {
	
	@Autowired UserMapper mapper;
	@Autowired @org.springframework.context.annotation.Lazy AuthenticationManager authenticationManager;

	private final static Logger logger = LoggerFactory.getLogger(UserService.class);

	/**
	 * JWT TOKEN 에 사용할 UserDetails 를 만든다.
	 * 
	 * @author : De-Kay
	 * @param userID : 유저 아이디
	 * @return UserDetails
	 **/
	@Override
	public UserDetails loadUserByUsername(String userInput) throws UsernameNotFoundException {
		UserModel userModel = new UserModel();
		
		String[] split = userInput.split(" :: ");
		String target = split[0];
	    String userId = split[1];
	    
		if("GESTURE".equals(target)) {
			userModel = mapper.getGestureUserInfoByUserId(userId);
		} else if("CONVATAR".equals(target)) {
			
		} else if("DOGPOO_HOME".equals(target)) {
			userModel = mapper.getDogpooHomeUserInfoByUserId(userId);
		}
		
		if (userModel != null) {
			return new User(userModel.getUserId(), userModel.getPasswd(), new ArrayList<>());
		} else {
			if (logger.isErrorEnabled()) {
				logger.error("ERROR OCCURED WHILE LOAD BY USERNAME | USER NOT FOUND WITH USER ID : " + userId);
			}
			try {
				throw new UsernameNotFoundException(
						"ERROR OCCURED WHILE LOAD BY USERNAME | USER NOT FOUND WITH USER ID : " + userId);
			} catch (UsernameNotFoundException e) {
//				e.printStackTrace();
				return null;
			}
		}
	}
	
	/**
	 * JWT 토큰에 세팅할 USER KEY 를 조회한다.
	 * 
	 * @author : dykwon@byeongmat.com
	 * @param userID : 유저 아이디
	 * @return userKey
	 **/
	public String getUserKey(String userId) throws UsernameNotFoundException {
		UserModel userModel = mapper.getGestureUserInfoByUserId(userId);
		if (userModel.getCcode() != null) {
			return userModel.getCcode();
		} else {
			if (logger.isErrorEnabled()) {
				logger.error("ERROR OCCURED WHILE GET USER KEY | USER KEY NOT FOUND WITH USER ID : " + userId);
			}
			throw new UsernameNotFoundException(
					"ERROR OCCURED WHILE GET USER KEY | USER KEY NOT FOUND WITH USER ID : " + userId);
		}
	}
	
	/**
	 * JWT 토큰에 세팅할 닉네임을 조회한다.
	 * 
	 * @author : dykwon@byeongmat.com
	 * @param userID : 유저 아이디
	 * @return userKey
	 **/
	public String getNickName(String userId) throws UsernameNotFoundException {
		UserModel userModel = mapper.getGestureUserInfoByUserId(userId);
		if (userModel.getCcode() != null) {
			return userModel.getUserNm();
		} else {
			if (logger.isErrorEnabled()) {
				logger.error("ERROR OCCURED WHILE GET USER KEY | USER KEY NOT FOUND WITH USER ID : " + userId);
			}
			throw new UsernameNotFoundException(
					"ERROR OCCURED WHILE GET USER KEY | USER KEY NOT FOUND WITH USER ID : " + userId);
		}
	}
	
	/**
	 * JWT 토큰에 세팅할 유저 모델을 조회한다.
	 * 
	 * @author : dykwon@byeongmat.com
	 * @param userID : 유저 아이디
	 * @return userKey
	 **/
	public UserModel getUserModel(String userInput) throws UsernameNotFoundException {
		
		UserModel userModel = new UserModel();
		
		String[] split = userInput.split(" :: ");
		String target = split[0];
	    String userId = split[1];
	    
		if("GESTURE".equals(target)) {
			userModel = mapper.getGestureUserInfoByUserId(userId);
		} else if("CONVATAR".equals(target)) {
			
		} else if("DOGPOO_HOME".equals(target)) {
			userModel = mapper.getDogpooHomeUserInfoByUserId(userId);
		}
		
		if(null != userModel) {
			return userModel;
		} else {
			if (logger.isErrorEnabled()) {
				logger.error("ERROR OCCURED WHILE GET USER KEY | USER KEY NOT FOUND WITH USER ID : " + userId);
			}
			try {
				throw new UsernameNotFoundException(
						"ERROR OCCURED WHILE GET USER KEY | USER KEY NOT FOUND WITH USER ID : " + userId);
			} catch (UsernameNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public int authenticate(String username, String password) throws Exception {
		int resultCd = 0;
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
			resultCd = 0;
		} catch (DisabledException e) {
			e.printStackTrace();
			resultCd = 401;
		} catch (BadCredentialsException e) {
			e.printStackTrace();
			resultCd = 301;
		} catch (InternalAuthenticationServiceException e) {
			e.printStackTrace();
			resultCd = 404;
		}
		return resultCd;
	}
	
	public HashMap<String, Object> authenticateDirect(String userUuid) throws Exception {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		JwtRequest reqModel = mapper.getUserIdPasswdModel(userUuid);
		int resultCd = 0;
		if(null == reqModel) {
			resultCd = 401;
			resMap.put("resultCd", resultCd);
			return resMap;
		}
		String username = reqModel.getUsername();
		if(username == null || "".equals(username)) {
			resultCd = 401;
			resMap.put("resultCd", resultCd);
			return resMap;
		}
		resMap.put("resultCd", resultCd);
		resMap.put("username", username);
		return resMap;
	}
	
	public HashMap<String, Object> authenticateDirect(String userId, String snsSignId) throws Exception {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		JwtRequest reqModel = mapper.getSocialUserIdPasswdModel(userId, snsSignId);
		int resultCd = 0;
		if(null == reqModel) {
			resultCd = 401;
			resMap.put("resultCd", resultCd);
			return resMap;
		}
		String username = reqModel.getUsername();
		if(username == null || "".equals(username)) {
			resultCd = 401;
			resMap.put("resultCd", resultCd);
			return resMap;
		}
		resMap.put("resultCd", resultCd);
		resMap.put("username", username);
		return resMap;
	}
}
