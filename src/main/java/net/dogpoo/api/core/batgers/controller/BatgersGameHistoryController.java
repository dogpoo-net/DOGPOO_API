package net.dogpoo.api.core.batgers.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.batgers.service.BatgersGameHistoryService;

@RestController
@RequestMapping("/batgers/api/history")
public class BatgersGameHistoryController {
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	@Autowired BatgersGameHistoryService service;
	
	@RequestMapping(value = "/getApplyPlayerMMRScore", method = RequestMethod.POST)
	public HashMap<String, Object> getApplyPlayerMMRScore() {
		return service.getApplyPlayerMMRScore();
	}
	
	@RequestMapping(value = "/getGameHistoryList", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getGameHistoryList(@RequestBody HashMap<String, Object> paramMap) {
		return service.getGameHistoryList(paramMap);
	}
	
	@RequestMapping(value = "/getGameHistoryDetailList", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getGameHistoryDetailList(@RequestBody HashMap<String, Object> paramMap) {
		return service.getGameHistoryDetailList(paramMap);
	}
}