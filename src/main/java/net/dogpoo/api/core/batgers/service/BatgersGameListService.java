package net.dogpoo.api.core.batgers.service;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.batgers.mapper.BatgersGameListMapper;

@Service
public class BatgersGameListService {

	@Autowired BatgersGameListMapper mapper;
	
	public HashMap<String, Object> regGameList(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		boolean isExist = mapper.isExistGameList(paramMap);
		if(isExist) {
			int updateCnt = mapper.refreshGameList(paramMap);
			if(updateCnt > 0) {
				resMap.put("msg", "홈리스 준비 완료");
				resMap.put("code", 0);
			} else {
				resMap.put("msg", "입력이 안됩니다. 걍 꺼지셈");
				resMap.put("code", 9);
			}
			return resMap;
		}
		
		int insertCnt = mapper.insertGameList(paramMap);
		if(insertCnt > 0) {
			resMap.put("msg", "홈리스 준비 완료");
			resMap.put("code", 0);
		} else {
			resMap.put("msg", "입력이 안됩니다. 걍 꺼지셈");
			resMap.put("code", 9);
		}
		return resMap;
	}
	
	public HashMap<String, Object> getGameList() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", mapper.getGameList());
		return resMap;
	}
	
	public HashMap<String, Object> removeGameList(HashMap<String, Object> paramMap) {
		int insertCnt = mapper.removeGameList(paramMap);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		if(insertCnt > 0) {
			resMap.put("msg", "삭제되었습니다.");
			resMap.put("code", 0);
		} else {
			resMap.put("msg", "삭제가 안됩니다. 걍 꺼지셈");
			resMap.put("code", 9);
		}
		return resMap;
	}
}