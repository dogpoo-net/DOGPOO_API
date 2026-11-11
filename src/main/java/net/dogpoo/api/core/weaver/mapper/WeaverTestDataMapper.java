package net.dogpoo.api.core.weaver.mapper;

import java.util.HashMap;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface WeaverTestDataMapper {
	
	@Insert("INSERT INTO WEAVER_TEST_DATA(account, data) VALUES(#{account}, #{jsonData}) ON DUPLICATE KEY UPDATE data = #{jsonData}")
	public int insertWeaverTestData(HashMap<String, Object> paramMap);
	
	@Select("SELECT DATA FROM WEAVER_TEST_DATA WHERE account = #{account}")
	public String getWeaverTestData(HashMap<String, Object> paramMap);
}