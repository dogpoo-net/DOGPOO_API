package net.dogpoo.api.core.batgers.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.batgers.mapper.BatgersGameHistoryMapper;
import net.dogpoo.api.core.batgers.mapper.BatgersGamePlayMapper;
import net.dogpoo.api.core.batgers.model.BatgersGameHistoryListModel;

@Service
public class BatgersGameHistoryService {
	
	@Autowired BatgersGameHistoryMapper mapper;
	@Autowired BatgersGamePlayMapper matchMapper;
	@Autowired BatgersDataComponent component;
	
	public HashMap<String, Object> getApplyPlayerMMRScore() {
		
		List<HashMap<String, Object>> relativeRecordList = mapper.getPlayersPUBGRelativeRecordList();
		List<Integer> parentCdList = relativeRecordList.stream()
				.map(map -> ((Number) map.get("parentCd")).intValue())
				.distinct()
				.collect(Collectors.toList());
		
		HashMap<Integer, Integer> dataMap = new HashMap<Integer, Integer>();
		for(int parentCd : parentCdList) {
			int target = 100;
			List<HashMap<String, Integer>> tempList = new ArrayList<HashMap<String, Integer>>();
			for(HashMap<String, Object> relativeRecordMap : relativeRecordList) {
				HashMap<String, Integer> tempMap = new HashMap<String, Integer>();
				int thisParentCd = ((Number) relativeRecordMap.get("parentCd")).intValue();
				if(parentCd == thisParentCd) {
					int loser = ((Number) relativeRecordMap.get("loser")).intValue();
					int defeatCnt = ((Number) relativeRecordMap.get("defeatCnt")).intValue();
					tempMap.put("loser", loser);
					tempMap.put("defeatCnt", defeatCnt);
					if(target > defeatCnt) {
						target = defeatCnt;
					}
					tempList.add(tempMap);
				}
			}
			
			for(HashMap<String, Integer> divideMap : tempList) {
				int loser = divideMap.get("loser");
				int relativeDefeatCnt = divideMap.get("defeatCnt");
				divideMap.put("defeatCnt", relativeDefeatCnt - target);
				int originCnt = null == dataMap.get(loser) ? 0 : dataMap.get(loser);
				dataMap.put(loser, originCnt + (relativeDefeatCnt - target));
			}
		}
		
		List<HashMap<String, Object>> playerList = mapper.getPlayerList();
		for(HashMap<String, Object> playerMap : playerList) {
			int playerNo = (int) playerMap.get("playerNo");
			int defeatCnt = null == dataMap.get(playerNo) ? 0 : dataMap.get(playerNo);
			playerMap.put("defeatCnt", defeatCnt);
		}
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", playerList);
		return resMap;
	}

	public HashMap<String, Object> getGameHistoryList(HashMap<String, Object> paramMap) {
		int page = ((Number) paramMap.get("page")).intValue() * 8;
		List<HashMap<String, Object>> wholePlayerList = mapper.getPlayerList();
		List<BatgersGameHistoryListModel> dataList = mapper.getGameHistoryList(page);
		for(BatgersGameHistoryListModel model : dataList) {
			String[] gamers = model.getGamers().split("\\|");
			List<String> gamersNmList = BatgersDataUtil.getGamerNmList(gamers, wholePlayerList);
			model.setGamersNmList(gamersNmList);
			model.setGamersNm(String.join("/", gamersNmList));
		}
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", dataList);
		return resMap;
	}
	
	public HashMap<String, Object> getGameHistoryDetailList(HashMap<String, Object> paramMap) {
		String parentCd = String.valueOf(paramMap.get("parentCd"));
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		List<HashMap<String, Object>> currentLiveGameList = matchMapper.getCurrentLiveGameList(parentCd);
		List<HashMap<String, Object>> statsPubgList = matchMapper.getPUBGStatsList(parentCd);
		for(HashMap<String, Object> currentLiveGame : currentLiveGameList) {
			int gameCd = ((Number) currentLiveGame.get("gameCd")).intValue();
			if(gameCd != 1) continue;
			
			int matchNo = ((Number) currentLiveGame.get("matchNo")).intValue();
			List<HashMap<String, Object>> eachStatsPubgList = new ArrayList<HashMap<String, Object>>();
			int dealAvg = 0;
			int statCnt = 0;
			for(HashMap<String, Object> statsPubgMap : statsPubgList) {
				if(matchNo == ((Number) statsPubgMap.get("matchNo")).intValue()) {
					boolean isHealer = ((Number) statsPubgMap.get("isHealer")).intValue() == 0 ? false : true;
					if(!isHealer) {
						statCnt++;
						dealAvg += ((Number) statsPubgMap.get("damageDealt")).intValue();
					}
					eachStatsPubgList.add(statsPubgMap);
				}
			}
			if(dealAvg > 0 && statCnt > 0) {
				dealAvg = (int) dealAvg / statCnt;
			}
			currentLiveGame.put("PUBG_STAT", eachStatsPubgList);
			currentLiveGame.put("dealAvg", dealAvg);
		}
		
		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put("currentLiveGameList", currentLiveGameList);
		dataMap.put("matchInfo", component.getPastMatchInfo(parentCd));
		
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", dataMap);
		return resMap;
	}
}