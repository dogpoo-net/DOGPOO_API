package net.dogpoo.api.core.batgers.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface BatgersGameListMapper {
	
	@Select("SELECT COUNT(*) FROM BATGERS_GAME_LIST WHERE gameNm = #{gameNm}")
	public boolean isExistGameList(HashMap<String, Object> paramMap);
	
	@Insert("INSERT INTO BATGERS_GAME_LIST(gameNm) VALUE (#{gameNm})")
	public int insertGameList(HashMap<String, Object> paramMap);
	
	@Select("SELECT gameNm, isFix, DATE_FORMAT(regDate, '%Y-%m-%d %T') AS regDate FROM BATGERS_GAME_LIST WHERE !isRemove ORDER BY idx")
	public List<HashMap<String, Object>> getGameList();

	@Update("UPDATE BATGERS_GAME_LIST SET isRemove = 1, delDate = NOW() WHERE gameNm = #{gameNm}")
	public int removeGameList(HashMap<String, Object> paramMap);
	
	@Update("UPDATE BATGERS_GAME_LIST SET isRemove = 0, delDate = NULL WHERE gameNm = #{gameNm}")
	public int refreshGameList(HashMap<String, Object> paramMap);

}