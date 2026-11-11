package net.dogpoo.api.core.weaver.service;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.weaver.mapper.WeaverTestDataMapper;

@Service
public class WeaverTestDataService {
	
	@Autowired WeaverTestDataMapper mapper;
	
	public HashMap<String, Object> insertWeaverTestData(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		int insertCnt = mapper.insertWeaverTestData(paramMap);
		if(insertCnt > 0) {
			resMap.put("msg", "OK");
			resMap.put("code", 0);
		} else {
			resMap.put("msg", "FAILED");
			resMap.put("code", 9);
		}
		return resMap;
	}
	
	public HashMap<String, Object> getWeaverTestData(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		String jsonData = mapper.getWeaverTestData(paramMap);
		if(jsonData.isEmpty()) {
			resMap.put("msg", "FAILED");
			resMap.put("code", 9);
		} else {
			resMap.put("DATA", jsonData);
			resMap.put("msg", "OK");
			resMap.put("code", 0);
		}
		return resMap;
	}
}