package net.dogpoo.api.core.gesture.model;

import org.apache.ibatis.type.Alias;

@Alias("GestureForumThreadPostCommentModel")
public class GestureForumThreadPostCommentModel {
	int threadNo;
	int postNo;
	int commentNo;
	String comment;
	int commentWriter;
	String commentWriterNm;
	String commentRegDate;
	String commentAgoTime;
	boolean commentIsUpdate;
	int rows = 1;
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
	public int getCommentNo() {
		return commentNo;
	}
	public void setCommentNo(int commentNo) {
		this.commentNo = commentNo;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public int getCommentWriter() {
		return commentWriter;
	}
	public void setCommentWriter(int commentWriter) {
		this.commentWriter = commentWriter;
	}
	public String getCommentWriterNm() {
		return commentWriterNm;
	}
	public void setCommentWriterNm(String commentWriterNm) {
		this.commentWriterNm = commentWriterNm;
	}
	public String getCommentRegDate() {
		return commentRegDate;
	}
	public void setCommentRegDate(String commentRegDate) {
		this.commentRegDate = commentRegDate;
	}
	public String getCommentAgoTime() {
		return commentAgoTime;
	}
	public void setCommentAgoTime(String commentAgoTime) {
		this.commentAgoTime = commentAgoTime;
	}
	public boolean isCommentIsUpdate() {
		return commentIsUpdate;
	}
	public void setCommentIsUpdate(boolean commentIsUpdate) {
		this.commentIsUpdate = commentIsUpdate;
	}
	public int getRows() {
		return rows;
	}
	public void setRows(int rows) {
		this.rows = rows;
	}
}