package net.dogpoo.api.core.batgers.controller;

import java.util.HashMap;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.batgers.service.BatgersGamePlayService;

@RestController
@RequestMapping("/batgers/api/gameplay")
public class BatgersGamePlayController {

    public BatgersGamePlayController(BatgersGamePlayService service) {
        this.service = service;
    }

	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	
	private final BatgersGamePlayService service;
	
	@RequestMapping(value = "/getPlayerList", method = RequestMethod.POST)
	public HashMap<String, Object> getPlayerList() {
		return service.getPlayerList();
	}
	
	@RequestMapping(value = "/insertMatchParent", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> insertPlayGameParent(@RequestBody HashMap<String, Object> paramMap) {
		return service.insertMatchParent(paramMap);
	}
	
	@RequestMapping(value = "/getMatchStatus", method = RequestMethod.POST)
	public HashMap<String, Object> getPlayGameStatus() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		try {
			resMap = service.getMatchStatus();
		} catch (Exception e) {
			e.printStackTrace();
			resMap.put("msg", "에러남. 걍 꺼지셈");
			resMap.put("code", 9);
		}
		return resMap;
	}
	
	@RequestMapping(value = "/insertMatchChild", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> insertPlayGameChild(@RequestBody HashMap<String, Object> paramMap) {
		return service.insertMatchChild(paramMap);
	}
	
	@RequestMapping(value = "/finishMatch", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> finishPlayGame(@RequestBody HashMap<String, Object> paramMap) {
		return service.finishMatch(paramMap);
	}
	
	@RequestMapping(value = "/deleteMatchParent", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> deleteMatchParent(@RequestBody HashMap<String, Object> paramMap) {
		return service.deleteMatchParent(paramMap);
	}
	
	@RequestMapping(value = "/deleteMatchChild", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> deleteMatchChild(@RequestBody HashMap<String, Object> paramMap) {
		return service.deleteMatchChild(paramMap);
	}
}