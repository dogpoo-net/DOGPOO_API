package net.dogpoo.api.core.batgers.controller;

import java.util.HashMap;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.batgers.service.BatgersStatisticsService;

@RestController
@RequestMapping("/batgers/api/statistics")
public class BatgersStatisticsController {

    public BatgersStatisticsController(BatgersStatisticsService service) {
        this.service = service;
    }


	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	private final BatgersStatisticsService service;
	
	@RequestMapping(value = "/getGamerTotalStatisticsResult", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getGamerTotalStatisticsResult(@RequestBody HashMap<String, Object> paramMap) {
		return service.getGamerTotalStatisticsResult(paramMap);
	}
	
	@RequestMapping(value = "/getGamerSumDefeatCnt", method = RequestMethod.POST)
	public HashMap<String, Object> getGamerSumDefeatCnt() {
		return service.getGamerSumDefeatCnt();
	}
	
	@RequestMapping(value = "/getDailyMMRScoreList", method = RequestMethod.POST)
	public HashMap<String, Object> getDailyMMRScoreList() {
		return service.getDailyMMRScoreList();
	}
}