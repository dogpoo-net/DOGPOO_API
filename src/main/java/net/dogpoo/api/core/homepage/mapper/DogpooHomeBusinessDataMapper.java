package net.dogpoo.api.core.homepage.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import net.dogpoo.api.core.homepage.model.DogpooHomeBizDataModel;
import net.dogpoo.api.core.homepage.model.DogpooHomeMemberDataModel;
import net.dogpoo.api.core.homepage.model.DogpooHomeWorksModel;

public interface DogpooHomeBusinessDataMapper {
	
	@Select("SELECT idx, memberNm, memberSubNm, POSITION, memberImg, description FROM DOGPOO_HOME_CREW_MEMBERS WHERE useYn")
	public List<DogpooHomeMemberDataModel> getCrewMemberList();
	
	@Select("SELECT idx, workTitle, workSubTitle, workImg, workLink, description FROM DOGPOO_HOME_WORKS WHERE useYn")
	public List<DogpooHomeWorksModel> getOurWorksList();
	
	@Select("SELECT bizNo, bizNm, bizNmEn, bizAddress, ceoNm, sellingNo, csEmail, csPhone FROM DOGPOO_HOME_BIZ_DATA LIMIT 1")
	public DogpooHomeBizDataModel getBizData();

}
