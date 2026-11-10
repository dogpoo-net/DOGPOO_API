package net.dogpoo.api.core.batgers.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import net.dogpoo.api.common.util.DateUtil;
import net.dogpoo.api.core.batgers.mapper.BatgersScheduleMapper;
import net.dogpoo.api.core.batgers.model.BatgersPlayerMMRModel;

@Service
public class BatgersScheduleService {
	
	private static final int MMR_CORR = 1050;
	private static String serverType = System.getProperty("serverType");
	@Autowired BatgersScheduleMapper mapper;

	@Scheduled(cron = "0 0 10 * * *")
	public void updatePlayerMMRScorePUBG() {
		if(!"live".equals(serverType)) return;
		List<Integer> playerNoList = mapper.getDifferRatingPlayerList();
		if(playerNoList.size() == 0) return;
		List<Integer> wholePlayerList = mapper.getWholePlayerNoList();
		for(int playerNo : wholePlayerList) {
			for(int curPlayerNo : playerNoList) {
				if(playerNo == curPlayerNo) {
					mapper.updatePlayerMMRScorePUBG(playerNo);
					break;
				}
			}
			mapper.insertPlayerDailyMMR(playerNo);
		}
	}
	
	@Scheduled(cron = "0 0/15 * * * *")
	public void getRecentlyPUBGData() {
		if(!"live".equals(serverType)) return;
		boolean isEnd = mapper.isEnd();
		if(isEnd) return;
		
		HashMap<String, Object> curGamersMap = mapper.getCurrentGamers();
		long sleepSeconds = 1000 * 60 * 20;
		try {
			Thread.sleep(sleepSeconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		processPUBGMatchData(curGamersMap);
	}
	
	private void processPUBGMatchData(HashMap<String, Object> curGamersMap) {
		
		int parentCd = ((Number) curGamersMap.get("idx")).intValue();
		List<HashMap<String, Object>> processMatchList = mapper.getNoProcessedMatchList(parentCd);
//		System.out.println(processMatchList);
		if(processMatchList.size() == 0) return;
		
		String[] gamers = ((String) curGamersMap.get("gamers")).split("\\|");
		String[] healers = ((String) curGamersMap.get("healers")).split("\\|");
		if(gamers.length == 5) return;
		
		List<HashMap<String, Object>> wholePlayerList = mapper.getPlayerList();
		List<HashMap<String, Object>> gamersModelList = BatgersDataUtil.getGamerList(gamers, healers, wholePlayerList);
		String playerIdPUBG = (String) gamersModelList.get(0).get("playerIdPUBG");
		List<String> matchIdList = new ArrayList<String>();
		try {
			matchIdList = BatgersMatchedOpenAPIUtil.getMatchedIDList(playerIdPUBG);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		List<String> matchDetailBodyList = new ArrayList<String>();
		for(int i = 0; i < matchIdList.size(); i++) {
			String matchId = matchIdList.get(i);
			boolean isExist = mapper.checkDuplicateMatchId(matchId);
			if(isExist) continue;	// 이미 등록된 매치는 넘어감
			
			String responseBody = BatgersMatchedOpenAPIUtil.getMatchedDetailResponseBody(matchId);
			matchDetailBodyList.add(responseBody);
			if(i > 9) break;
		}
		Collections.reverse(matchDetailBodyList);
		
		boolean isOkay = false;
		for(HashMap<String, Object> matchMap : processMatchList) {
			int matchNo = ((Number) matchMap.get("matchNo")).intValue();
			String regDate = (String) matchMap.get("regDate");
			
			for(int i = 0; i < matchDetailBodyList.size(); i++) {
				String responseBody = matchDetailBodyList.get(i);
				HashMap<String, Object> dateMap = BatgersMatchedOpenAPIUtil.getMatchStartEndTime(responseBody, gamersModelList);
				String startDate = DateUtil.addHour((String) dateMap.get("startDate"), 9);
				int endTime = (int) dateMap.get("endTime");
				String originDate = DateUtil.minusSeconds(regDate, endTime);
				isOkay = DateUtil.isWithin4MinutesBefore(originDate, startDate);
				if(isOkay) {
					System.out.println(isOkay + " :: " + regDate + "(" + endTime + ") :: " + originDate + " :: " + startDate);
					List<String> playerNoList = new ArrayList<String>();
					for(HashMap<String, Object> gamerMap : gamersModelList) {
						playerNoList.add(String.valueOf(gamerMap.get("playerNo")));
					}
					
					String where = String.join(" OR playerNo = ", playerNoList);
					List<HashMap<String, Object>> currentPlayerList = mapper.getCurrentMatchPlayer(where);
					for(HashMap<String, Object> currentPlayerMap : currentPlayerList) {
						boolean isHealer = false;
						String playerNo = String.valueOf(currentPlayerMap.get("playerNo"));
						for(String healer : healers) {
							if(healer.equals(playerNo)) {
								isHealer = true;
								break;
							}
						}
						currentPlayerMap.put("isHealer", isHealer);
					}
					
					List<HashMap<String, Object>> statsList = getBattleGroundMatchedData(responseBody, currentPlayerList);
					String matchId = (String) statsList.get(0).get("matchId");
					mapper.registerMatchId(matchId, matchNo);
					for(HashMap<String, Object> statsMap : statsList) {
						statsMap.put("matchNo", matchNo);
						mapper.insertMatchPUBGDetail(statsMap);
					}
					matchDetailBodyList.remove(i);
					break;
				}
			}
			System.out.println();
			System.out.println("=============================");
			System.out.println();
		}
	}
	
	public List<HashMap<String, Object>> getBattleGroundMatchedData(String responseBody, List<HashMap<String, Object>> gamerList) {
		List<HashMap<String, Object>> statsList = BatgersMatchedOpenAPIUtil.getMatchedDetail(responseBody, gamerList);
			
		List<BatgersPlayerMMRModel> playerList = new ArrayList<BatgersPlayerMMRModel>();
		for(HashMap<String, Object> statsMap : statsList) {
			int playerNo = (int) statsMap.get("playerNo");
			boolean isHealer = (boolean) statsMap.get("isHealer");
			if(isHealer) {
				statsMap.put("damagePenalty", 0);
				continue;
			}
			
			double penaltyOrigin = (double) statsMap.get("rating") - MMR_CORR;
			if(penaltyOrigin < 0) penaltyOrigin = 0;
			int penalty = penaltyOrigin > 0 ? (int) Math.round(penaltyOrigin / 2) : 0;
			statsMap.put("damagePenalty", penalty);
			
			double myDamage = Double.parseDouble((String) statsMap.get("damageDealt")) - penalty;
			if(myDamage < 0) myDamage = 0;
			
			boolean isWin = false;
			
			for(HashMap<String, Object> compareMap : statsList) {
				int comparePlayerNo = (int) compareMap.get("playerNo");
				if(playerNo == comparePlayerNo) continue;
				boolean isCompareHealer = (boolean) compareMap.get("isHealer");
				if(isCompareHealer) continue;
				
				double comparePenaltyOrigin = (double) compareMap.get("rating") - MMR_CORR;
				if(comparePenaltyOrigin < 0) comparePenaltyOrigin = 0;
				int comparePenalty = comparePenaltyOrigin > 0 ? (int) Math.floor(comparePenaltyOrigin / 2) : 0;
				double compareDamage = Double.parseDouble((String) compareMap.get("damageDealt")) - comparePenalty;
				if(compareDamage < 0) compareDamage = 0;
				
				if(myDamage > compareDamage) {
					isWin = true;
					break;
				}
			}
			
			statsMap.put("loserFlag", !isWin);
			
			BatgersPlayerMMRModel model = new BatgersPlayerMMRModel((int) statsMap.get("playerNo"), (String) statsMap.get("playerNm"), (double) statsMap.get("innerRating"), myDamage, isWin);
			playerList.add(model);
		}
		
		List<BatgersPlayerMMRModel> playerResultList = BatgersMMRCalculator.calculateMMR(playerList);
		
		for(BatgersPlayerMMRModel model : playerResultList) {
			int playerNo = model.getPlayerNo();
			mapper.updatePlayerRating(model.getNewMmr(), playerNo);
		}
		
		for(HashMap<String, Object> statsMap : statsList) {
			int myPlayerNo = (int) statsMap.get("playerNo");
			boolean isInclude = false;
			for(BatgersPlayerMMRModel model : playerResultList) {
				int comparePlayerNo = model.getPlayerNo();
				if(myPlayerNo == comparePlayerNo) {
					isInclude = true;
					statsMap.put("mmr", model.getNewMmr());
					statsMap.put("mmrDelta", model.getMmrDelta());
					break;
				}
			}
			
			if(!isInclude) {	// 리스트에 이름없으면 힐러임 (노패배)
				statsMap.put("mmr", statsMap.get("innerRating"));
				statsMap.put("mmrDelta", 0);
				statsMap.put("loserFlag", false);
			}
		}
		
		return statsList;
	}
	
	public void updateFaultSingleMatch(int parentCd, int matchNo) {
		
		HashMap<String, Object> processMatchMap = mapper.getReprocessMatch(parentCd, matchNo);
		HashMap<String, Object> gamersMap = mapper.getGamers(parentCd);
		
		String[] gamers = ((String) gamersMap.get("gamers")).split("\\|");
		String[] healers = ((String) gamersMap.get("healers")).split("\\|");
		if(gamers.length == 5) return;
		
		List<HashMap<String, Object>> wholePlayerList = mapper.getPlayerList();
		List<HashMap<String, Object>> gamersModelList = BatgersDataUtil.getGamerList(gamers, healers, wholePlayerList);
		String playerIdPUBG = (String) gamersModelList.get(0).get("playerIdPUBG");
		List<String> matchIdList = new ArrayList<String>();
		try {
			matchIdList = BatgersMatchedOpenAPIUtil.getMatchedIDList(playerIdPUBG);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		List<String> matchDetailBodyList = new ArrayList<String>();
		for(int i = 0; i < matchIdList.size(); i++) {
			String matchId = matchIdList.get(i);
			boolean isExist = mapper.checkDuplicateMatchId(matchId);
			if(isExist) continue;	// 이미 등록된 매치는 넘어감
			
			String responseBody = BatgersMatchedOpenAPIUtil.getMatchedDetailResponseBody(matchId);
			matchDetailBodyList.add(responseBody);
			if(i > 5) break;
		}
		Collections.reverse(matchDetailBodyList);
		
		boolean isOkay = false;
		String regDate = (String) processMatchMap.get("regDate");
		
		for(int i = 0; i < matchDetailBodyList.size(); i++) {
			String responseBody = matchDetailBodyList.get(i);
			HashMap<String, Object> dateMap = BatgersMatchedOpenAPIUtil.getMatchStartEndTime(responseBody, gamersModelList);
			String startDate = DateUtil.addHour((String) dateMap.get("startDate"), 9);
			int endTime = (int) dateMap.get("endTime");
			String originDate = DateUtil.minusSeconds(regDate, endTime);
			isOkay = DateUtil.isWithin4MinutesBefore(originDate, startDate);
			if(isOkay) {
				System.out.println(isOkay + " :: " + regDate + "(" + endTime + ") :: " + originDate + " :: " + startDate);
				List<String> playerNoList = new ArrayList<String>();
				for(HashMap<String, Object> gamerMap : gamersModelList) {
					playerNoList.add(String.valueOf(gamerMap.get("playerNo")));
				}
				String where = String.join(" OR playerNo = ", playerNoList);
				List<HashMap<String, Object>> currentPlayerList = mapper.getCurrentMatchPlayer(where);
				List<HashMap<String, Object>> statsList = getBattleGroundMatchedData(responseBody, currentPlayerList);
				String matchId = (String) statsList.get(0).get("matchId");
				mapper.registerMatchId(matchId, matchNo);
				
				List<HashMap<String, Object>> playerIdPUBGList = mapper.getPUBGPlayerIdList(matchNo);
				for(HashMap<String, Object> statsMap : statsList) {
					String playerId = (String) statsMap.get("playerId");
					for(HashMap<String, Object> playerIdPUBGMap : playerIdPUBGList) {
						String playerIdIter = (String) playerIdPUBGMap.get("playerId");
						if(playerId.equals(playerIdIter)) {
							int idx = ((Number) playerIdPUBGMap.get("idx")).intValue();
							statsMap.put("idx", idx);
							break;
						}
					}
					statsMap.put("matchNo", matchNo);
					mapper.updateMatchPUBGDetail(statsMap);
				}
				break;
			}
		}
		System.out.println();
		System.out.println("=============================");
		System.out.println();
	}
}