package net.dogpoo.api.core.gesture.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import net.dogpoo.api.common.util.auth.JwtTokenUtil;
import net.dogpoo.api.core.gesture.service.GestureForumThreadPostCommentService;

@CrossOrigin(origins = "*", maxAge=3600)
@Controller
@RequestMapping("/gesture/forum/thread/post/comment")
public class GestureForumThreadPostCommentController {
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	private static final String AUTHORIZATION = "Authorization";
	
	@Autowired GestureForumThreadPostCommentService service;
	@Autowired JwtTokenUtil jwtTokenUtil;
	
	@RequestMapping(value="/insertForumThreadPostComment", method=RequestMethod.POST, produces = JSON_UTF8)
	@ResponseBody
	public HashMap<String, Object> insertForumThreadPostComment(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.insertForumThreadPostComment(userKey, paramMap);
	}
	
	@RequestMapping(value="/updateForumThreadPostComment", method=RequestMethod.POST, produces = JSON_UTF8)
	@ResponseBody
	public HashMap<String, Object> updateForumThreadPostComment(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.updateForumThreadPostComment(userKey, paramMap);
	}
	
	@RequestMapping(value="/getForumThreadPostCommentList", method=RequestMethod.POST)
	@ResponseBody
	public HashMap<String, Object> getForumThreadPostCommentList(@RequestBody HashMap<String, Object> paramMap) {
		return service.getForumThreadPostCommentList(paramMap);
	}
	
	@RequestMapping(value="/deleteForumThreadPostComment", method=RequestMethod.POST, produces = JSON_UTF8)
	@ResponseBody
	public HashMap<String, Object> deleteForumThreadPostComment(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.deleteForumThreadPostComment(userKey, paramMap);
	}

	private HashMap<String, Object> getExpiredTokenResultMap(String userKey) {
		HashMap<String, Object> keyResMap = new HashMap<String, Object>();
		keyResMap.put("code", 6);
		keyResMap.put("msg", "Expired session! Please log in again.");
		return keyResMap;
	}
}
