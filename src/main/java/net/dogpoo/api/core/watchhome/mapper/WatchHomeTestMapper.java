package net.dogpoo.api.core.watchhome.mapper;

import java.util.HashMap;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WatchHomeTestMapper {
	
	@Insert("INSERT INTO DOGPOO_WATCHHOME_NODE_MAP (serverIp, serverNo) VALUES (#{serverIp}, #{serverNo})")
	public int insertServerInfo(@Param("serverIp") String serverIp, @Param("serverNo") int serverNo);
	
	@Select("SELECT idx, serverIp, serverNo FROM DOGPOO_WATCHHOME_NODE_MAP WHERE serverIp = #{serverIp}")
	public HashMap<String, Object> getServerInfo(@Param("serverIp") String serverIp);
	
	@Select("SELECT COUNT(*) FROM DOGPOO_WATCHHOME_NODE_MAP WHERE serverNo = #{serverNo}")
	public boolean isDuplicate(@Param("serverNo") int serverNo);
	
	@Select("SELECT serverIp FROM DOGPOO_WATCHHOME_NODE_MAP WHERE serverNo = #{serverNo} LIMIT 1")
	public String getServerAddr(@Param("serverNo") String serverNo);

}
