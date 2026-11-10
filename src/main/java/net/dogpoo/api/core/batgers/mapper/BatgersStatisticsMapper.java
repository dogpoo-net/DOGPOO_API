package net.dogpoo.api.core.batgers.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BatgersStatisticsMapper {
	
	public HashMap<String, Object> getGamerSumDefeatCnt();
	
	@Select("SELECT playerNo, playerNm FROM BATGERS_PLAYER_LIST")
	public List<HashMap<String, Object>> getPlayerNoList();
	
	@Select("SELECT ROUND(ratingPUBG) FROM BATGERS_PLAYER_DAILY_SCORE WHERE playerNo = ${playerNo} LIMIT 30")
	public List<Integer> getDailyScoreList(@Param("playerNo") int playerNo);
	
	@Select("SELECT DATE_FORMAT(regDate, '%m-%d') AS regDate FROM (SELECT regDate FROM BATGERS_PLAYER_DAILY_SCORE GROUP BY regDate ORDER BY regDate DESC LIMIT 30) AS t ORDER BY regDate ASC")
	public List<String> getGameRegDateList();
	
	@Select("SELECT idx, gamers, IFNULL(healers, '') AS healers, bet FROM BATGERS_MATCH_PARENT WHERE idx = #{parentCd}")
	public HashMap<String, Object> getMatchParent(@Param("parentCd") String parentCd);
	
	@Select("SELECT matchNo, gameCd, loser FROM BATGERS_MATCH_CHILD WHERE parentCd = #{parentCd}")
	public List<HashMap<String, Object>> getMatchedTotalGameList(@Param("parentCd") String parentCd);
	
	public List<HashMap<String, Object>> getGamerTotalStatisticsResult(HashMap<String, Object> paramMap);
	
//	SELECT 
//	  playerNm,
//	  ROUND(innerRating) AS mmr,
//	  ROUND(IF(innerRating > 1050, (innerRating - 1050) / 2, 0)) AS penalty
//	FROM
//	  BATGERS_PLAYER_LIST AS a
//	  INNER JOIN BATGERS_MATCH_PARENT AS b
//	  ON b.gamer IN(a.playerNo)

}
