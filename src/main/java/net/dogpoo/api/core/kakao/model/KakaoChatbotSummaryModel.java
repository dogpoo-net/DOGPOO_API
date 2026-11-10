package net.dogpoo.api.core.kakao.model;

import org.apache.ibatis.type.Alias;

@Alias("KakaoChatbotSummaryModel")
public class KakaoChatbotSummaryModel {
	int idx;
	String mode;
	int summarySeq;
	int endSeq;
	String userId;
	String content;
	String regDate;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public int getSummarySeq() {
		return summarySeq;
	}
	public void setSummarySeq(int summarySeq) {
		this.summarySeq = summarySeq;
	}
	public int getEndSeq() {
		return endSeq;
	}
	public void setEndSeq(int endSeq) {
		this.endSeq = endSeq;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getRegDate() {
		return regDate;
	}
	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}
	@Override
	public String toString() {
		return "KakaoChatbotSummaryModel [idx=" + idx + ", summarySeq=" + summarySeq
				+ ", endSeq=" + endSeq + ", userId=" + userId + ", content=" + content + ", regDate=" + regDate + "]";
	}
}