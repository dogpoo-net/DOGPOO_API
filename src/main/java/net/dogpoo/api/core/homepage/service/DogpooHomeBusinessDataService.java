package net.dogpoo.api.core.homepage.service;

import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import net.dogpoo.api.core.homepage.mapper.DogpooHomeBusinessDataMapper;
import net.dogpoo.api.core.homepage.model.DogpooHomeBizDataModel;
import net.dogpoo.api.core.homepage.model.DogpooHomeMemberDataModel;
import net.dogpoo.api.core.homepage.model.DogpooHomeWorksModel;

@Service
public class DogpooHomeBusinessDataService {

    public DogpooHomeBusinessDataService(DogpooHomeBusinessDataMapper mapper) {
        this.mapper = mapper;
    }

	private final DogpooHomeBusinessDataMapper mapper;
	
	public HashMap<String, Object> getCrewMembers() {
		List<DogpooHomeMemberDataModel> dataList = mapper.getCrewMemberList();
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", dataList);
		return resMap;
	}
	
	public HashMap<String, Object> getOurWorks() {
		List<DogpooHomeWorksModel> dataList = mapper.getOurWorksList();
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", dataList);
		return resMap;
	}
	
	public HashMap<String, Object> getBizData() {
		DogpooHomeBizDataModel bizModel = mapper.getBizData();
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("msg", "OK");
		resMap.put("code", 0);
		resMap.put("DATA", bizModel);
		return resMap;
	}
}