package net.dogpoo.api.core.convatar.model;

import org.apache.ibatis.type.Alias;

@Alias("ConvatarNoticeModel")
public class ConvatarNoticeModel {
	int idx;
	String title;
	String contents;
	String writer;
	String regDate;
	String regTimestamp;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContents() {
		return contents;
	}
	public void setContents(String contents) {
		this.contents = contents;
	}
	public String getWriter() {
		return writer;
	}
	public void setWriter(String writer) {
		this.writer = writer;
	}
	public String getRegDate() {
		return regDate;
	}
	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}
	public String getRegTimestamp() {
		return regTimestamp;
	}
	public void setRegTimestamp(String regTimestamp) {
		this.regTimestamp = regTimestamp;
	}
}