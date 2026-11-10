package net.dogpoo.api.core.batgers.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface BatgersGamePlayMapper {
	
	@Select("SELECT playerNo, playerNm, rating, playerNmPUBG, playerIdPUBG FROM BATGERS_PLAYER_LIST")
	public List<HashMap<String, Object>> getPlayerList();
	
	@Select("SELECT idx, gamers, IFNULL(healers, '') AS healers, DATE_FORMAT(regDate, '%Y-%m-%d %T') AS regDate FROM BATGERS_MATCH_PARENT WHERE isLive LIMIT 1")
	public HashMap<String, Object> getCurrentLiveGameParent();
	
    @Select("SELECT DATE_FORMAT(regDate, '%Y-%m-%d %T') AS regDate, DATE_FORMAT(endDate, '%Y-%m-%d %T') AS endDate FROM BATGERS_MATCH_PARENT WHERE idx = #{parentCd}")
	public HashMap<String, String> getMatchedTime(@Param("parentCd") String parentCd);
	
	public List<HashMap<String, Object>> getCurrentLiveGameList(@Param("parentCd") String parentCd);
	
	public List<HashMap<String, Object>> getPUBGStatsList(@Param("parentCd") String parentCd);
	
	public int insertMatchParent(HashMap<String, Object> paramMap);
	
    @Insert("INSERT INTO BATGERS_MATCH_PLAYER (parentCd, playerNo, isHealer) VALUES (#{parentCd}, #{playerNo}, #{isHealer})")
	public int insertMatchPlayer(HashMap<String, Object> paramMap);
	
	@Select("SELECT COUNT(*) FROM BATGERS_MATCH_PARENT WHERE isLive")
	public boolean isLiveGame();
	
	@Select("SELECT idx, gameNm FROM BATGERS_GAME_LIST WHERE !isRemove ORDER BY idx")
	public List<HashMap<String, Object>> getGameList();
	
	public int insertMatchChild(HashMap<String, Object> paramMap);
	
	@Update("UPDATE BATGERS_MATCH_PARENT SET isLive = 0, endDate = NOW() WHERE idx = #{parentCd}")
	public int finishMatch(@Param("parentCd") String parentCd);
	
	@Select("SELECT idx, gamers, IFNULL(healers, '') AS healers, bet, DATE_FORMAT(regDate, '%Y-%m-%d %T') AS regDate FROM BATGERS_MATCH_PARENT WHERE idx = #{parentCd}")
	public HashMap<String, Object> getPastMatchParent(@Param("parentCd") String parentCd);
	
	@Delete("DELETE FROM BATGERS_MATCH_PARENT WHERE idx = #{parentCd}")
	public void deleteMatchParent(@Param("parentCd") String parentCd);
	
	@Delete("DELETE FROM BATGERS_MATCH_CHILD WHERE parentCd = #{parentCd}")
	public void deleteMatchChild(@Param("parentCd") String parentCd);
	
	@Delete("DELETE FROM BATGERS_MATCH_PUBG_DETAIL WHERE matchNo IN(SELECT matchNo FROM BATGERS_MATCH_CHILD WHERE parentCd = #{parentCd})")
	public void deletePUBGMatchHistory(@Param("parentCd") String parentCd);
	
    @Delete("DELETE FROM BATGERS_MATCH_PUBG_DETAIL WHERE matchNo = #{matchNo}")
	public void deleteMatchPUBGDetail(@Param("matchNo") int matchNo);
	
    @Delete("DELETE FROM BATGERS_MATCH_CHILD WHERE matchNo = #{matchNo}")
	public void deleteEachMatchChild(@Param("matchNo") int matchNo);
}
