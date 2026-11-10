package net.dogpoo.api.core.convatar.service;

import java.util.HashMap;

import org.springframework.stereotype.Service;

import net.dogpoo.api.core.convatar.mapper.ConvatarCurrencyMapper;
import net.dogpoo.api.core.convatar.model.ConvatarCurrencyVolumeModel;

@Service
public class ConvatarCurrencyService {

    public ConvatarCurrencyService(ConvatarCurrencyMapper mapper) {
        this.mapper = mapper;
    }

	private final ConvatarCurrencyMapper mapper;
	
	public HashMap<String, Object> getCountryList() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.getCountryList());
		return resMap;
	}
	
	public HashMap<String, Object> getEachCountryCurrencyVolume(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		ConvatarCurrencyVolumeModel model = mapper.getEachCountryCurrencyVolume(paramMap);
		
		if(null != model) {
			resMap.put("code", 0);
			resMap.put("msg", "OK");
			resMap.put("DATA", model);
		} else {
			resMap.put("code", 1);
			resMap.put("msg", "NOTHING_DATA");
		}
		return resMap;
	}
}