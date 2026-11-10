package net.dogpoo.api.core.gesture.controller;

import java.io.IOException;
import java.util.HashMap;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.common.util.auth.JwtTokenUtil;
import net.dogpoo.api.core.gesture.service.GestureForumThreadPostService;

@RestController
@RequestMapping("/gesture/forum/thread")
public class GestureForumThreadPostController {

    public GestureForumThreadPostController(
            GestureForumThreadPostService service,
            JwtTokenUtil jwtTokenUtil) {
        this.service = service;
        this.jwtTokenUtil = jwtTokenUtil;
    }

	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	private static final String AUTHORIZATION = "Authorization";
	
	private final GestureForumThreadPostService service;
	private final JwtTokenUtil jwtTokenUtil;
	
	@RequestMapping(value="/getForumThreadPostWholeList", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getForumThreadPostWholeList(@RequestBody HashMap<String, Object> paramMap) {
		return service.getForumThreadPostWholeList(paramMap);
	}
	
	@RequestMapping(value="/getGestureForumThreadListSearch", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getGestureForumThreadListSearch(@RequestBody HashMap<String, Object> paramMap) {
		return service.getGestureForumThreadListSearch(paramMap);
	}
	
	@RequestMapping(value="/updateForumThreadCountingViewNum", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> updateForumThreadCountingViewNum(@RequestBody HashMap<String, Object> paramMap) {
		return service.updateForumThreadCountingViewNum(paramMap);
	}
	
	@RequestMapping(value="/getForumThreadHeadData", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getForumThreadHeadData(@RequestBody HashMap<String, Object> paramMap) {
		return service.getForumThreadHeadData(paramMap);
	}
	
	@RequestMapping(value="/getForumThreadPostContent", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getForumThreadPostContent(@RequestBody HashMap<String, Object> paramMap) {
		return service.getForumThreadPostContent(paramMap);
	}
	
	@RequestMapping(value="/getForumHeadThreadContent", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getForumHeadThreadContent(@RequestBody HashMap<String, Object> paramMap) {
		return service.getForumHeadThreadContent(paramMap);
	}
	
	@RequestMapping(value="/getForumThreadCategoryList", method=RequestMethod.POST)
	public HashMap<String, Object> getForumThreadCategoryList(@RequestHeader(value = AUTHORIZATION) String authorization) {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.getForumThreadCategoryList(userKey);
	}
	
	@RequestMapping(value="/insertForumThread", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> insertForumThread(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) throws IOException {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.insertForumThread(userKey, paramMap);
	}
	
	@RequestMapping(value="/insertForumThreadPost", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> insertForumThreadPost(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) throws IOException {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.insertForumThreadPost(userKey, paramMap);
	}
	
	@RequestMapping(value="/insertForumThreadPostLike", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> insertForumThreadPostLike(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.insertForumThreadPostLike(userKey, paramMap);
	}
	
	@RequestMapping(value="/updateForumThread", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> updateForumThread(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) throws IOException {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.updateForumThread(userKey, paramMap);
	}
	
	@RequestMapping(value="/updateForumThreadPost", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> updateForumThreadPost(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) throws IOException {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.updateForumThreadPost(userKey, paramMap);
	}
	
	@RequestMapping(value="/deleteForumThreadPost", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> deleteForumThreadPost(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) throws IOException {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.deleteForumThreadPost(userKey, paramMap);
	}
	
	@RequestMapping(value="/toggleForumThreadLike", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> toggleForumThreadLike(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.toggleForumThreadLike(userKey, paramMap);
	}
	
	@RequestMapping(value="/removeForumThreadWholeData", method=RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> removeForumThreadWholeData(@RequestHeader(value = AUTHORIZATION) String authorization, @RequestBody HashMap<String, Object> paramMap) throws IOException {
		String userKey = jwtTokenUtil.getUserKeyFromJwtToken(authorization);
		if("EXPIRED_KEY".equals(userKey)) return getExpiredTokenResultMap(userKey);
		return service.removeForumThreadWholeData(userKey, paramMap);
	}
	
	private HashMap<String, Object> getExpiredTokenResultMap(String userKey) {
		HashMap<String, Object> keyResMap = new HashMap<String, Object>();
		keyResMap.put("code", 6);
		keyResMap.put("msg", "Expired session! Please log in again.");
		return keyResMap;
	}
}