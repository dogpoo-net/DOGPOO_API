package net.dogpoo.api.core.batgers.service;

import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import net.dogpoo.api.core.batgers.mapper.BatgersStatisticsMapper;

@Service
public class BatgersStatisticsService {

    public BatgersStatisticsService(BatgersStatisticsMapper mapper) {
        this.mapper = mapper;
    }

	
	private final BatgersStatisticsMapper mapper;
	
	public HashMap<String, Object> getGamerTotalStatisticsResult(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", mapper.getGamerTotalStatisticsResult(paramMap));
		return resMap;
	}
	
	public HashMap<String, Object> getGamerSumDefeatCnt() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", mapper.getGamerSumDefeatCnt());
		return resMap;
	}
	
	public HashMap<String, Object> getDailyMMRScoreList() {
		List<HashMap<String, Object>> playerNoList = mapper.getPlayerNoList();
		
		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		for(HashMap<String, Object> playerNoMap : playerNoList) {
			int playerNo = ((Number) playerNoMap.get("playerNo")).intValue();
			List<Integer> dailyScoreList = mapper.getDailyScoreList(playerNo);
			playerNoMap.put("name", playerNoMap.get("playerNm"));
			playerNoMap.put("type", "line");
			playerNoMap.put("smooth", false);
			playerNoMap.put("data", dailyScoreList);
		}
		List<String> dateList = mapper.getGameRegDateList();
		
		dataMap.put("regDates", dateList);
		dataMap.put("series", playerNoList);
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", dataMap);
		return resMap;
	}
}