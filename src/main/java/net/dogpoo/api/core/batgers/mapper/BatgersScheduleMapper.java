package net.dogpoo.api.core.batgers.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface BatgersScheduleMapper {
	
	@Select("SELECT playerNo, playerNm, rating, playerNmPUBG, playerIdPUBG FROM BATGERS_PLAYER_LIST")
	public List<HashMap<String, Object>> getPlayerList();
	
	@Select("SELECT idx, gamers, IFNULL(healers, '') AS healers FROM BATGERS_MATCH_PARENT ORDER BY idx DESC LIMIT 1")
	public HashMap<String, Object> getCurrentGamers();
	
	@Select("SELECT idx, gamers, IFNULL(healers, '') AS healers FROM BATGERS_MATCH_PARENT WHERE idx = ${parentCd}")
	public HashMap<String, Object> getGamers(@Param("parentCd") int parentCd);
	
	@Select("SELECT IFNULL(DATE_ADD(endDate, INTERVAL 30 MINUTE) <= NOW(), 0) AS isEnd FROM BATGERS_MATCH_PARENT ORDER BY idx DESC LIMIT 1")
	public boolean isEnd();
	
	public List<HashMap<String, Object>> getNoProcessedMatchList(@Param("parentCd") int parentCd);
	
	public HashMap<String, Object> getReprocessMatch(@Param("parentCd") int parentCd, @Param("matchNo") int matchNo);
	
	@Select("SELECT COUNT(*) FROM BATGERS_MATCH_CHILD WHERE matchIdPUBG = #{matchId}")
	public boolean checkDuplicateMatchId(@Param("matchId") String matchId);
	
	@Select("SELECT playerNo, playerNm, rating, innerRating, playerNmPUBG, playerIdPUBG FROM BATGERS_PLAYER_LIST WHERE playerNo = ${where}")
	public List<HashMap<String, Object>> getCurrentMatchPlayer(@Param("where") String where);
	
	@Update("UPDATE BATGERS_PLAYER_LIST SET innerRating = ${mmr} WHERE playerNo = #{playerNo}")
	public int updatePlayerRating(@Param("mmr") double mmr, @Param("playerNo") int playerNo);
	
	public int insertMatchPUBGDetail(HashMap<String, Object> paramMap);
	
	public int updateMatchPUBGDetail(HashMap<String, Object> paramMap);
	
	@Update("UPDATE BATGERS_MATCH_CHILD SET matchIdPUBG = #{matchId} WHERE matchNo = #{matchNo}")
	public int registerMatchId(@Param("matchId") String matchId, @Param("matchNo") int matchNo);
	
	@Select("SELECT playerNo FROM BATGERS_PLAYER_LIST WHERE rating != innerRating")
	public List<Integer> getDifferRatingPlayerList();
	
	@Select("SELECT playerNo FROM BATGERS_PLAYER_LIST")
	public List<Integer> getWholePlayerNoList();
	
	@Update("UPDATE BATGERS_PLAYER_LIST SET rating = innerRating WHERE playerNo = #{playerNo}")
	public int updatePlayerMMRScorePUBG(@Param("playerNo") int playerNo);
	
	@Insert("INSERT INTO BATGERS_PLAYER_DAILY_SCORE(playerNo, ratingPUBG) SELECT playerNo, rating FROM BATGERS_PLAYER_LIST WHERE playerNo = #{playerNo}")
	public int insertPlayerDailyMMR(@Param("playerNo") int playerNo);
	
	@Select("SELECT idx, playerId FROM BATGERS_MATCH_PUBG_DETAIL WHERE matchNo = ${matchNo}")
	public List<HashMap<String, Object>> getPUBGPlayerIdList(@Param("matchNo") int matchNo);
	
}
