package net.dogpoo.api.core.test.controller;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.test.service.DogpooConnectionTestService;

@RestController
@RequestMapping("/api")
public class DogpooConnectionTestController {
	
	@Autowired DogpooConnectionTestService service;
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	
	@RequestMapping(value = "/checkPost", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getPostTest(@RequestBody HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("result", "포스트 통신 정상");
		return resMap;
	}
	
	@RequestMapping(value = "/checkGet", method = RequestMethod.GET)
	public HashMap<String, Object> getTest() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("result", "겟 통신 정상");
		return resMap;
	}
	
	@RequestMapping(value = "/postJsonTest", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public String getPostGestureBody(@RequestBody String params) {
		JSONParser parser = new JSONParser();
		JSONObject root = new JSONObject();
		try {
			root = (JSONObject) parser.parse(params);
		} catch (ParseException e) {
			e.printStackTrace();
			return "JSON 형식이 아닙니다.";
		}
		HashMap<String, Object> paramMap = root;
		return service.getPostGestureBody(paramMap);
	}
}