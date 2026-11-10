package net.dogpoo.api.core.batgers.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import net.dogpoo.api.core.batgers.mapper.BatgersGamePlayMapper;
import net.dogpoo.api.core.batgers.mapper.BatgersStatisticsMapper;

@Component
public class BatgersDataComponent {

    public BatgersDataComponent(BatgersGamePlayMapper mapper) {
        this.mapper = mapper;
    }

	
	private final BatgersGamePlayMapper mapper;
	
	public HashMap<String, Object> getPastMatchInfo(String parentCd) {
		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.putAll(getMatchInfo(parentCd));
		dataMap.put("matchedTime", mapper.getMatchedTime(parentCd));
		return dataMap;
	}
	
	public HashMap<String, Object> getMatchInfo(String parentCd) {
		HashMap<String, Object> parentMap = mapper.getPastMatchParent(parentCd);
		int bet = (int) parentMap.get("bet");
				
		String[] gamers = ((String) parentMap.get("gamers")).split("\\|");
		String[] healers = ((String) parentMap.get("healers")).split("\\|");
		
		List<HashMap<String, Object>> wholePlayerList = mapper.getPlayerList();
		List<HashMap<String, Object>> gamerList = BatgersDataUtil.getGamerList(gamers, healers, wholePlayerList);
		
		List<HashMap<String, Object>> lastGameDetailList = mapper.getCurrentLiveGameList(parentCd);
		List<HashMap<String, Object>> gamerStatList = new ArrayList<HashMap<String, Object>>();
		
		int totalGameCnt = lastGameDetailList.size();
		int healerCnt = 0;
		for(HashMap<String, Object> gamer : gamerList) {
			boolean isHealer = (boolean) gamer.get("isHealer");
			if(isHealer) healerCnt++;
		}
		
		int betterCnt = gamerList.size() - healerCnt;
		int gameAmount = bet * (betterCnt - 1);
		
		int defeatCntMinimum = 100;
		for(HashMap<String, Object> gamer : gamerList) {
			int defeatCnt = 0;
			int gamerCd = (int) gamer.get("playerNo");
			List<String> defeatGames = new ArrayList<String>();
			for(HashMap<String, Object> lastGameDetail : lastGameDetailList) {
				int loser = (int) lastGameDetail.get("loser");
				if(gamerCd == loser) {
					defeatCnt++;
					defeatGames.add((String) lastGameDetail.get("gameNm"));
				}
			}
			
			int winCnt = totalGameCnt - defeatCnt;
			int winAmount = bet * winCnt;
			int defeatAmount = gameAmount * defeatCnt;
			int earn = winAmount - defeatAmount;
			
			HashMap<String, Integer> defeatGameMap = new HashMap<String, Integer>();
			for(String defeatGame : defeatGames) {
				defeatGameMap.merge(defeatGame, 1, Integer::sum);
			}
			
			boolean isHealer = (boolean) gamer.get("isHealer");
			
			if((defeatCntMinimum > defeatCnt) && !isHealer) {
				defeatCntMinimum = defeatCnt;
			}
			
			int fee = isHealer ? 0 : earn;
			gamer.put("defeatCnt", defeatCnt);
			gamer.put("defeatGame", defeatGameMap);
			gamer.put("fee", fee);
			gamerStatList.add(gamer);
		}
		
		for(HashMap<String, Object> gamer : gamerStatList) {
			boolean isHealer = (boolean) gamer.get("isHealer");
			if(!isHealer) {
				int defeatCnt = (int) gamer.get("defeatCnt") - defeatCntMinimum;
				gamer.put("defeatCnt", defeatCnt);
			}
		}
		
		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put("gamerStatList", gamerStatList);
		dataMap.put("detailModel", lastGameDetailList);
		return dataMap;
	}
}