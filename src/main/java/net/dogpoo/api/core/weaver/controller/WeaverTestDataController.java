package net.dogpoo.api.core.weaver.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.weaver.service.WeaverTestDataService;

@RestController
@RequestMapping("/weaver/api/test")
public class WeaverTestDataController {
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	@Autowired WeaverTestDataService service;
	
	@RequestMapping(value = "/insertWeaverTestData", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> insertWeaverTestData(@RequestBody HashMap<String, Object> paramMap) {
		return service.insertWeaverTestData(paramMap);
	}
	
	@RequestMapping(value = "/getWeaverTestData", method = RequestMethod.POST)
	public HashMap<String, Object> getWeaverTestData(@RequestBody HashMap<String, Object> paramMap) {
		return service.getWeaverTestData(paramMap);
	}

}
