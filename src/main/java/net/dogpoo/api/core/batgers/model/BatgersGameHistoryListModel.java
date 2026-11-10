package net.dogpoo.api.core.batgers.model;

import java.util.List;

import org.apache.ibatis.type.Alias;

@Alias("BatgersGameHistoryListModel")
public class BatgersGameHistoryListModel {
	int idx;
	String gamers;
	String healers;
	String gamersNm;
	List<String> gamersNmList;
	boolean isLive;
	int bet;
	String regDate;
	String endDate;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getGamers() {
		return gamers;
	}
	public void setGamers(String gamers) {
		this.gamers = gamers;
	}
	public String getHealers() {
		return healers;
	}
	public void setHealers(String healers) {
		this.healers = healers;
	}
	public String getGamersNm() {
		return gamersNm;
	}
	public void setGamersNm(String gamersNm) {
		this.gamersNm = gamersNm;
	}
	public List<String> getGamersNmList() {
		return gamersNmList;
	}
	public void setGamersNmList(List<String> gamersNmList) {
		this.gamersNmList = gamersNmList;
	}
	public boolean getIsLive() {
		return isLive;
	}
	public void setLive(boolean isLive) {
		this.isLive = isLive;
	}
	public int getBet() {
		return bet;
	}
	public void setBet(int bet) {
		this.bet = bet;
	}
	public String getRegDate() {
		return regDate;
	}
	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
}