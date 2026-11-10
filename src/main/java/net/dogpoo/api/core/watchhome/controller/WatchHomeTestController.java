package net.dogpoo.api.core.watchhome.controller;

import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.common.util.IpUtil;
import net.dogpoo.api.core.watchhome.service.WatchHomeTestService;

@RestController
@RequestMapping("/api/watchhome")
public class WatchHomeTestController {
	
	@Autowired WatchHomeTestService service;
	
//	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
//	private static final String FIXED_TOKEN = System.getenv("KAKAO_WEBHOOK_TOKEN");
	
	@RequestMapping(value = "/getServerNo", method = RequestMethod.GET)
	public HashMap<String, Object> getServerNo(HttpServletRequest req) {
		
//		String token = req.getHeader("token");
//		if(!FIXED_TOKEN.equals(token)) {
//			HashMap<String, Object> resMap = new HashMap<String, Object>();
//			resMap.put("code", 9);
//			resMap.put("msg", "NOT_AUTHENTICATED");
//			return resMap;
//		}
		
		String serverIp = IpUtil.getClientIp(req);
		return service.getServerNo(serverIp);
	}
	
	@RequestMapping(value = "/getServerAddr", method = RequestMethod.GET)
	public HashMap<String, Object> getServerAddr(@RequestParam("serverNo") String serverNo) {
		
//		String token = req.getHeader("token");
//		if(!FIXED_TOKEN.equals(token)) {
//			HashMap<String, Object> resMap = new HashMap<String, Object>();
//			resMap.put("code", 9);
//			resMap.put("msg", "NOT_AUTHENTICATED");
//			return resMap;
//		}
		
		return service.getServerAddr(serverNo);
	}
}