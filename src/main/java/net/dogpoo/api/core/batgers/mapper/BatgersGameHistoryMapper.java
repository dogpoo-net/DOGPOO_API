package net.dogpoo.api.core.batgers.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import net.dogpoo.api.core.batgers.model.BatgersGameHistoryListModel;

public interface BatgersGameHistoryMapper {
	
	@Select("SELECT playerNo, playerNm, rating FROM BATGERS_PLAYER_LIST")
	public List<HashMap<String, Object>> getPlayerList();
	
	public List<HashMap<String, Object>> getPlayersRelativeRecordList();
	
	public List<HashMap<String, Object>> getPlayersPUBGRelativeRecordList();
	
    @Select("<script><bind name='limit' value='page + 8'/>SELECT idx, gamers, IFNULL(healers, '') AS healers, isLive, bet, DATE_FORMAT(regDate, '%Y-%m-%d %T') AS regDate, DATE_FORMAT(endDate, '%Y-%m-%d %T') AS endDate FROM BATGERS_MATCH_PARENT ORDER BY idx DESC LIMIT #{page}, #{limit}</script>")
	public List<BatgersGameHistoryListModel> getGameHistoryList(@Param("page") int page);

}
