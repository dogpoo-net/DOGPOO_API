package net.dogpoo.api.core.gesture.model;

public class GestureForumThreadHeadContentModel {
	int threadNo;
	int postNo;
	String threadTitle;
	int threadCateNo;
	int threadOwner;
	String threadContent;
	boolean threadIsPrivate;
	public int getThreadNo() {
		return threadNo;
	}
	public void setThreadNo(int threadNo) {
		this.threadNo = threadNo;
	}
	public int getPostNo() {
		return postNo;
	}
	public void setPostNo(int postNo) {
		this.postNo = postNo;
	}
	public String getThreadTitle() {
		return threadTitle;
	}
	public void setThreadTitle(String threadTitle) {
		this.threadTitle = threadTitle;
	}
	public int getThreadCateNo() {
		return threadCateNo;
	}
	public void setThreadCateNo(int threadCateNo) {
		this.threadCateNo = threadCateNo;
	}
	public int getThreadOwner() {
		return threadOwner;
	}
	public void setThreadOwner(int threadOwner) {
		this.threadOwner = threadOwner;
	}
	public String getThreadContent() {
		return threadContent;
	}
	public void setThreadContent(String threadContent) {
		this.threadContent = threadContent;
	}
	public boolean getThreadIsPrivate() {
		return threadIsPrivate;
	}
	public void setThreadIsPrivate(boolean threadIsPrivate) {
		this.threadIsPrivate = threadIsPrivate;
	}
}