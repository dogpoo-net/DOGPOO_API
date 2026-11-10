package net.dogpoo.api.core.watchhome.service;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dogpoo.api.common.util.StringUtil;
import net.dogpoo.api.core.watchhome.mapper.WatchHomeTestMapper;

@Service
public class WatchHomeTestService {
	
	@Autowired WatchHomeTestMapper mapper;
	private static final String SERVER_PORT = "25000";
	
	public HashMap<String, Object> getServerNo(String serverIp) {
		
		HashMap<String, Object> serverMap = mapper.getServerInfo(serverIp);
		String serverNo = "";
		if(null == serverMap) {
			int genNo = StringUtil.generateAuthNo();
			boolean isDuplicate = mapper.isDuplicate(genNo);
			while(isDuplicate) {
				genNo = StringUtil.generateAuthNo();
				isDuplicate = mapper.isDuplicate(genNo);
				if(!isDuplicate) break;
			}
			
			mapper.insertServerInfo(serverIp, genNo);
			serverNo = String.valueOf(genNo);
		} else {
			serverNo = (String) serverMap.get("serverNo");
		}
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("SERVER_IP", serverIp);
		resMap.put("PORT", SERVER_PORT);
		resMap.put("GENERATE_NO", serverNo);
		return resMap;
	}
	
	public HashMap<String, Object> getServerAddr(String serverNo) {
		String serverAddr = mapper.getServerAddr(serverNo);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("CODE", "0");
		resMap.put("MSG", "OK");
		resMap.put("SERVER_IP", serverAddr);
		resMap.put("PORT", SERVER_PORT);
		return resMap;
	}
}