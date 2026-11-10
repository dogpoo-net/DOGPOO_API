package net.dogpoo.api.core.batgers.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.batgers.mapper.BatgersGamePlayMapper;

@Service
public class BatgersGamePlayService {

    public BatgersGamePlayService(BatgersGamePlayMapper mapper, BatgersDataComponent component) {
        this.mapper = mapper;
        this.component = component;
    }

	private final BatgersGamePlayMapper mapper;
	private final BatgersDataComponent component;
	
	public HashMap<String, Object> getMatchStatus() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		
		List<HashMap<String, Object>> wholePlayerList = mapper.getPlayerList();
		if(mapper.isLiveGame()) {
			
			HashMap<String, Object> currentLiveGameMap = mapper.getCurrentLiveGameParent();
			String parentCd = String.valueOf(currentLiveGameMap.get("idx"));
			String regDate = (String) currentLiveGameMap.get("regDate");
			
			String[] gamers = ((String) currentLiveGameMap.get("gamers")).split("\\|");
			String[] healers = ((String) currentLiveGameMap.get("healers")).split("\\|");
			
			List<HashMap<String, Object>> gamersModelList = BatgersDataUtil.getGamerList(gamers, healers, wholePlayerList);
			
			List<HashMap<String, Object>> currentLiveGameList = mapper.getCurrentLiveGameList(parentCd);
			List<HashMap<String, Object>> statsPubgList = mapper.getPUBGStatsList(parentCd);
			for(HashMap<String, Object> currentLiveGame : currentLiveGameList) {
				int gameCd = ((Number) currentLiveGame.get("gameCd")).intValue();
				if(gameCd != 1) continue;
				
				int matchNo = ((Number) currentLiveGame.get("matchNo")).intValue();
				List<HashMap<String, Object>> eachStatsPubgList = new ArrayList<HashMap<String, Object>>();
				int dealAvg = 0;
				int statCnt = 0;
				for(HashMap<String, Object> statsPubgMap : statsPubgList) {
					if(matchNo == ((Number) statsPubgMap.get("matchNo")).intValue()) {
						if(((Integer) statsPubgMap.get("isHealer")) == 0) {
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
			
			dataMap.put("isLive", true);
			dataMap.put("parentCd", parentCd);
			dataMap.put("currentLiveGameList", currentLiveGameList);
			dataMap.put("currentPlayerList", gamersModelList);
			dataMap.put("matchInfo", component.getPastMatchInfo(parentCd));
			dataMap.put("gameList", mapper.getGameList());
			dataMap.put("regDate", regDate);
			
			resMap.put("msg", "OK");
			resMap.put("code", 0);
			resMap.put("DATA", dataMap);
		} else {
			dataMap.put("isLive", false);
			dataMap.put("wholePlayerList", wholePlayerList);
			
			resMap.put("msg", "OK");
			resMap.put("code", 0);
			resMap.put("DATA", dataMap);
		}
		return resMap;
	}
	
	public HashMap<String, Object> insertMatchChild(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		int insertCnt = mapper.insertMatchChild(paramMap);
		if(insertCnt == 0) {
			resMap.put("msg", "입력이 안됩니다. 걍 꺼지셈");
			resMap.put("code", 9);
			return resMap;
		}
		
		if(insertCnt > 0) {
			resMap.put("msg", "예비 거지가 입력되었습니다.");
			resMap.put("code", 0);
		}
		return resMap;
	}
	
	public HashMap<String, Object> finishMatch(HashMap<String, Object> paramMap) {
		String parentCd = String.valueOf(paramMap.get("parentCd"));
		int updateCnt = mapper.finishMatch(parentCd);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		if(updateCnt > 0) {
			resMap.put("msg", "게임이 종료되었습니다.");
			resMap.put("code", 0);
			resMap.put("DATA", component.getPastMatchInfo(parentCd));
		} else {
			resMap.put("msg", "종료가 안됩니다. 걍 꺼지셈");
			resMap.put("code", 9);
		}
		return resMap;
	}
	
	public HashMap<String, Object> getPlayerList() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", mapper.getPlayerList());
		return resMap;
	}
	
	public HashMap<String, Object> insertMatchParent(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		if(mapper.isLiveGame()) {
			resMap.put("msg", "현재 게임이 진행중입니다.");
			resMap.put("code", 1);
			return resMap;
		}
		
		List<HashMap<String, Object>> params = (List<HashMap<String, Object>>) paramMap.get("gamers");
		String bet = String.valueOf(paramMap.get("bet"));
		
		List<String> gamerCodeList = new ArrayList<String>();
		List<String> healerList = new ArrayList<String>();
		for(HashMap<String, Object> param : params) {
			String playerNo = String.valueOf(param.get("playerNo"));
			gamerCodeList.add(playerNo);
			if(null != param.get("isHealer")) {
				if((boolean) param.get("isHealer")) {
					healerList.add(playerNo);
				}
			}
		}
		
		String gamerCodes = String.join("|", gamerCodeList);
		String healerCodes = String.join("|", healerList);
		HashMap<String, Object> parentParamMap = new HashMap<String, Object>();
		parentParamMap.put("gamers", gamerCodes);
		parentParamMap.put("healers", healerCodes);
		parentParamMap.put("bet", bet);
		int insertCnt = mapper.insertMatchParent(parentParamMap);
		for(HashMap<String, Object> param : params) {
			param.put("parentCd", parentParamMap.get("parentCd"));
			mapper.insertMatchPlayer(param);
		}
		
		if(insertCnt > 0) {
			resMap.put("msg", "게임을 시작하지.");
			resMap.put("code", 0);
		} else {
			resMap.put("msg", "왜 안되는지 모릅니다. 걍 꺼지셈");
			resMap.put("code", 9);
		}
		return resMap;
	}
	
	public HashMap<String, Object> deleteMatchParent(HashMap<String, Object> paramMap) {
		String parentCd = String.valueOf(paramMap.get("parentCd"));
		mapper.deletePUBGMatchHistory(parentCd);
		mapper.deleteMatchChild(parentCd);
		mapper.deleteMatchParent(parentCd);
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "나가리.");
		resMap.put("code", 0);
		return resMap;
	}
	
	public HashMap<String, Object> deleteMatchChild(HashMap<String, Object> paramMap) {
		int matchNo = ((Number) paramMap.get("matchNo")).intValue();
		int gameCd = ((Number) paramMap.get("gameCd")).intValue();
		if(gameCd == 1) mapper.deleteMatchPUBGDetail(matchNo);
		mapper.deleteEachMatchChild(matchNo);
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "뛰어내리셈");
		resMap.put("code", 0);
		return resMap;
	}
	
	public String getPUBGOpenAPITest(String playerId) {
		String responseBody = "";
		try {
			String matchId = BatgersMatchedOpenAPIUtil.getLastMatchedID(playerId);
			responseBody = BatgersMatchedOpenAPIUtil.getMatchedDetailResponseBody(matchId);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return responseBody;
	}
}