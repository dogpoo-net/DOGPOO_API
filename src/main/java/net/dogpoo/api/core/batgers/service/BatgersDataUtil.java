package net.dogpoo.api.core.batgers.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class BatgersDataUtil {
	
	public static List<HashMap<String, Object>> getGamerList(String[] gamers, String[] healers, List<HashMap<String, Object>> wholePlayerList) {
		List<HashMap<String, Object>> gamersModelList = new ArrayList<HashMap<String, Object>>();
		for(String gamer : gamers) {
			int gamerCd = Integer.parseInt(gamer);
			for(HashMap<String, Object> player : wholePlayerList) {
				int playerCd = (int) player.get("playerNo");
				if(gamerCd == playerCd) {
					boolean isHealer = false;
					for(String healer : healers) {
						if(healer.equals(String.valueOf(playerCd))) {
							isHealer = true;
							break;
						}
					}
					player.put("isHealer", isHealer);
					gamersModelList.add(player);
				}
			}
		}
		return gamersModelList;
	}
	
	public static List<String> getGamerNmList(String[] gamers, List<HashMap<String, Object>> wholePlayerList) {
		List<String> gamersNmList = new ArrayList<String>();
		for(String gamer : gamers) {
			int gamerCd = Integer.parseInt(gamer);
			for(HashMap<String, Object> player : wholePlayerList) {
				int playerCd = (int) player.get("playerNo");
				if(gamerCd == playerCd) {
					gamersNmList.add((String) player.get("playerNm"));
				}
			}
		}
		return gamersNmList;
	}
}