package net.dogpoo.api.core.convatar.service;

import java.util.HashMap;

import org.springframework.stereotype.Service;

import net.dogpoo.api.core.convatar.mapper.ConvatarHomeMapper;

@Service
public class ConvatarHomeService {

    public ConvatarHomeService(ConvatarHomeMapper mapper) {
        this.mapper = mapper;
    }

	private final ConvatarHomeMapper mapper;
	
	public HashMap<String, Object> getConvatarReleaseNotesList() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.getConvatarReleaseNotesList());
		return resMap;
	}
	
	public HashMap<String, Object> getNoticeModelList() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.getNoticeModelList());
		return resMap;
	}
}