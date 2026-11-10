package net.dogpoo.api.core.gesture.model;

import org.apache.ibatis.type.Alias;

@Alias("GestureForumThreadHeaderModel")
public class GestureForumThreadHeaderModel {
	int threadNo;
	String threadTitle;
	String threadContent;
	int answerCnt;
	int threadViews;
	int threadLikeCnt;
	int threadOwner;
	String threadOwnerNm;
	String threadCountryCd;
	String threadCateNmKr;
	String threadCateNmEn;
	boolean threadIsOpen;
	boolean threadIsPrivate;
	boolean threadIsNotice;
	String threadOpenDate;
	String threadAgoTime;
	String closeDate;
	boolean isThreadLike;
	public int getThreadNo() {
		return threadNo;
	}
	public void setThreadNo(int threadNo) {
		this.threadNo = threadNo;
	}
	public String getThreadTitle() {
		return threadTitle;
	}
	public void setThreadTitle(String threadTitle) {
		this.threadTitle = threadTitle;
	}
	public String getThreadContent() {
		return threadContent;
	}
	public void setThreadContent(String threadContent) {
		this.threadContent = threadContent;
	}
	public int getAnswerCnt() {
		return answerCnt;
	}
	public void setAnswerCnt(int answerCnt) {
		this.answerCnt = answerCnt;
	}
	public int getThreadViews() {
		return threadViews;
	}
	public void setThreadViews(int threadViews) {
		this.threadViews = threadViews;
	}
	public int getThreadLikeCnt() {
		return threadLikeCnt;
	}
	public void setThreadLikeCnt(int threadLikeCnt) {
		this.threadLikeCnt = threadLikeCnt;
	}
	public int getThreadOwner() {
		return threadOwner;
	}
	public void setThreadOwner(int threadOwner) {
		this.threadOwner = threadOwner;
	}
	public String getThreadOwnerNm() {
		return threadOwnerNm;
	}
	public void setThreadOwnerNm(String threadOwnerNm) {
		this.threadOwnerNm = threadOwnerNm;
	}
	public String getThreadCountryCd() {
		return threadCountryCd;
	}
	public void setThreadCountryCd(String threadCountryCd) {
		this.threadCountryCd = threadCountryCd;
	}
	public String getThreadCateNmKr() {
		return threadCateNmKr;
	}
	public void setThreadCateNmKr(String threadCateNmKr) {
		this.threadCateNmKr = threadCateNmKr;
	}
	public String getThreadCateNmEn() {
		return threadCateNmEn;
	}
	public void setThreadCateNmEn(String threadCateNmEn) {
		this.threadCateNmEn = threadCateNmEn;
	}
	public boolean getThreadIsOpen() {
		return threadIsOpen;
	}
	public void setThreadIsOpen(boolean threadIsOpen) {
		this.threadIsOpen = threadIsOpen;
	}
	public boolean getThreadIsPrivate() {
		return threadIsPrivate;
	}
	public void setThreadIsPrivate(boolean threadIsPrivate) {
		this.threadIsPrivate = threadIsPrivate;
	}
	public boolean getThreadIsNotice() {
		return threadIsNotice;
	}
	public void setThreadIsNotice(boolean threadIsNotice) {
		this.threadIsNotice = threadIsNotice;
	}
	public String getThreadOpenDate() {
		return threadOpenDate;
	}
	public void setThreadOpenDate(String threadOpenDate) {
		this.threadOpenDate = threadOpenDate;
	}
	public String getThreadAgoTime() {
		return threadAgoTime;
	}
	public void setThreadAgoTime(String threadAgoTime) {
		this.threadAgoTime = threadAgoTime;
	}
	public String getCloseDate() {
		return closeDate;
	}
	public void setCloseDate(String closeDate) {
		this.closeDate = closeDate;
	}
	public boolean getisThreadLike() {
		return isThreadLike;
	}
	public void setThreadLike(boolean isThreadLike) {
		this.isThreadLike = isThreadLike;
	}
}