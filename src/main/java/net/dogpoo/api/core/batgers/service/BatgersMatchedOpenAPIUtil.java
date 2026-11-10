package net.dogpoo.api.core.batgers.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.dogpoo.api.common.util.http.HttpClientUtil;

public final class BatgersMatchedOpenAPIUtil {
	
	private final static String APIKEY = System.getenv("BATGERS_API_KEY");
	private final static String BASE_URL = "https://api.pubg.com/shards/kakao";
	
	public static String getLastMatchedID(String playerId) throws ParseException {
//		String uri = BASE_URL + "/players?filter[playerNames]=vioks159";
		String uri = BASE_URL + "/players/" + playerId;
		HashMap<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("Authorization", "Bearer " + APIKEY);
		headerMap.put("Accept", "application/vnd.api+json");
		String responseBody = HttpClientUtil.get(uri, headerMap);
		
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(responseBody);
		JSONObject data = (JSONObject) root.get("data");
		JSONObject relationShips = (JSONObject) data.get("relationships");
		JSONObject matches = (JSONObject) relationShips.get("matches");
		JSONArray matchDatas = (JSONArray) matches.get("data");
		JSONObject matchData = (JSONObject) matchDatas.get(0);
		String matchId = (String) matchData.get("id");
		return matchId;
	}
	
	public static List<String> getMatchedIDList(String playerId) throws ParseException {
//		String uri = BASE_URL + "/players?filter[playerNames]=vioks159";
		String uri = BASE_URL + "/players/" + playerId;
		HashMap<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("Authorization", "Bearer " + APIKEY);
		headerMap.put("Accept", "application/vnd.api+json");
		String responseBody = HttpClientUtil.get(uri, headerMap);
		
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(responseBody);
		JSONObject data = (JSONObject) root.get("data");
		JSONObject relationShips = (JSONObject) data.get("relationships");
		JSONObject matches = (JSONObject) relationShips.get("matches");
		JSONArray matchDatas = (JSONArray) matches.get("data");
		
		List<String> matchIdList = new ArrayList<String>();
		for(int i = 0; i < matchDatas.size(); i++) {
			JSONObject matchData = (JSONObject) matchDatas.get(i);
			String type = (String) matchData.get("type");
			if("match".equals(type)) {
				String matchId = (String) matchData.get("id");
				matchIdList.add(matchId);
			}
		}
		return matchIdList;
	}
	
	public static String getMatchedDetailResponseBody(String matchId) {
		String uri = BASE_URL + "/matches/" + matchId;
		HashMap<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("Authorization", "Bearer " + APIKEY);
		headerMap.put("Accept", "application/vnd.api+json");
		return HttpClientUtil.get(uri, headerMap);
	}
	
	public static JSONArray getIncludeds(String responseBody) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(responseBody);
		return (JSONArray) root.get("included");
	}
	
	public static HashMap<String, Object> getMatchStartEndTime(String responseBody, List<HashMap<String, Object>> gamerList) {
		JSONParser parser = new JSONParser();
		JSONObject root = new JSONObject();
		try {
			root = (JSONObject) parser.parse(responseBody);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		JSONObject data = (JSONObject) root.get("data");
		JSONObject attributes = (JSONObject) data.get("attributes");
		String startDate = ((String) attributes.get("createdAt")).replace("Z", "").replace("T", " ");
		
		List<Integer> timeList = new ArrayList<Integer>();
		JSONArray includeds = (JSONArray) root.get("included");
		for(HashMap<String, Object> gamerMap : gamerList) {
			
			String playerIdPUBG = (String) gamerMap.get("playerIdPUBG");
			
			for(int i = 0; i < includeds.size(); i++) {
				JSONObject included = (JSONObject) includeds.get(i);
				String type = (String) included.get("type");
				if(!"participant".equals(type)) continue;
				
				JSONObject detail = (JSONObject) included.get("attributes");
				JSONObject stats = (JSONObject) detail.get("stats");
				String playerId = (String) stats.get("playerId");
				if(playerIdPUBG.equals(playerId)) {
					timeList.add(((Number) stats.get("timeSurvived")).intValue());
					break;
				}
			}
		}
		
		int max = Collections.max(timeList);
		
		HashMap<String, Object> dateMap = new HashMap<String, Object>();
		dateMap.put("startDate", startDate);
		dateMap.put("endTime", max);
		return dateMap;
	}
	
	public static List<HashMap<String, Object>> getMatchedDetail(String responseBody, List<HashMap<String, Object>> gamerList) {
		JSONParser parser = new JSONParser();
		JSONObject root = new JSONObject();
		try {
			root = (JSONObject) parser.parse(responseBody);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		JSONObject data = (JSONObject) root.get("data");
		String matchId = (String) data.get("id");
		JSONObject attributes = (JSONObject) data.get("attributes");
		String startDate = ((String) attributes.get("createdAt")).replace("Z", "").replace("T", " ");
		
		List<HashMap<String, Object>> statsList = new ArrayList<HashMap<String, Object>>();
		JSONArray includeds = (JSONArray) root.get("included");
		for(HashMap<String, Object> gamerMap : gamerList) {
			
			int playerNo = (int) gamerMap.get("playerNo");
			String playerNm = (String) gamerMap.get("playerNm");
			double rating = (double) gamerMap.get("rating");
			double innerRating = (double) gamerMap.get("innerRating");
			boolean isHealer = (boolean) gamerMap.get("isHealer");
			String playerNmPUBG = (String) gamerMap.get("playerNmPUBG");
			String playerIdPUBG = (String) gamerMap.get("playerIdPUBG");
			
			for(int i = 0; i < includeds.size(); i++) {
				JSONObject included = (JSONObject) includeds.get(i);
				String type = (String) included.get("type");
				if(!"participant".equals(type)) continue;
				
				JSONObject detail = (JSONObject) included.get("attributes");
				JSONObject stats = (JSONObject) detail.get("stats");
				String playerId = (String) stats.get("playerId");
				if(playerIdPUBG.equals(playerId)) {
					HashMap<String, Object> statsMap = new HashMap<String, Object>();
					statsMap.put("playerNo", playerNo);
					statsMap.put("playerNm", playerNm);
					statsMap.put("rating", rating);
					statsMap.put("innerRating", innerRating);
					statsMap.put("matchId", matchId);
					statsMap.put("isHealer", isHealer);
					statsMap.put("playerNm", playerNmPUBG);
					statsMap.put("playerId", playerIdPUBG);
					statsMap.put("winPlace", String.valueOf(stats.get("winPlace")));
					statsMap.put("damageDealt", String.valueOf(stats.get("damageDealt")));
					statsMap.put("kills", String.valueOf(stats.get("kills")));
					statsMap.put("dbnos", String.valueOf(stats.get("DBNOs")));
					statsMap.put("assists", String.valueOf(stats.get("assists")));
					statsMap.put("revives", String.valueOf(stats.get("revives")));
					statsMap.put("teamKills", String.valueOf(stats.get("teamKills")));
					statsMap.put("timeSurvived", String.valueOf(stats.get("timeSurvived")));
					statsMap.put("startDate", startDate);
					statsList.add(statsMap);
					break;
				}
			}
		}
		return statsList;
	}
}