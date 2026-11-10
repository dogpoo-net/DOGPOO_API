package net.dogpoo.api.core.batgers.controller;

import java.util.HashMap;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.batgers.service.BatgersGameListService;

@RestController
@RequestMapping("/batgers/api/gamelist")
public class BatgersGameListController {

    public BatgersGameListController(BatgersGameListService service) {
        this.service = service;
    }


	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	
	private final BatgersGameListService service;
	
	@RequestMapping(value = "/regGameList", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> regGameList(@RequestBody HashMap<String, Object> paramMap) {
		return service.regGameList(paramMap);
	}
	
	@RequestMapping(value = "/getGameList", method = RequestMethod.POST)
	public HashMap<String, Object> getGameList() {
		return service.getGameList();
	}
	
	@RequestMapping(value = "/removeGameList", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> removeGameList(@RequestBody HashMap<String, Object> paramMap) {
		return service.removeGameList(paramMap);
	}
}