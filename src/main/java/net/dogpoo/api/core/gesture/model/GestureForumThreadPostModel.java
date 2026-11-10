package net.dogpoo.api.core.gesture.model;

import org.apache.ibatis.type.Alias;

@Alias("GestureForumThreadPostModel")
public class GestureForumThreadPostModel {
	int postNo;
	int threadNo;
	String content;
	int writer;
	int likeCnt;
	int dislikeCnt;
	Boolean isPostLike;
	Boolean isHeadPost;
	int commentCnt;
	String postWriter;
	String postAgoTime;
	String regDate;
	int navColorVal;
	public int getPostNo() {
		return postNo;
	}
	public void setPostNo(int postNo) {
		this.postNo = postNo;
	}
	public int getThreadNo() {
		return threadNo;
	}
	public void setThreadNo(int threadNo) {
		this.threadNo = threadNo;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getWriter() {
		return writer;
	}
	public void setWriter(int writer) {
		this.writer = writer;
	}
	public int getLikeCnt() {
		return likeCnt;
	}
	public void setLikeCnt(int likeCnt) {
		this.likeCnt = likeCnt;
	}
	public int getDislikeCnt() {
		return dislikeCnt;
	}
	public void setDislikeCnt(int dislikeCnt) {
		this.dislikeCnt = dislikeCnt;
	}
	public Boolean getIsPostLike() {
		return isPostLike;
	}
	public void setIsPostLike(Boolean isPostLike) {
		this.isPostLike = isPostLike;
	}
	public Boolean getIsHeadPost() {
		return isHeadPost;
	}
	public void setIsHeadPost(Boolean isHeadPost) {
		this.isHeadPost = isHeadPost;
	}
	public int getCommentCnt() {
		return commentCnt;
	}
	public void setCommentCnt(int commentCnt) {
		this.commentCnt = commentCnt;
	}
	public String getPostWriter() {
		return postWriter;
	}
	public void setPostWriter(String postWriter) {
		this.postWriter = postWriter;
	}
	public String getPostAgoTime() {
		return postAgoTime;
	}
	public void setPostAgoTime(String postAgoTime) {
		this.postAgoTime = postAgoTime;
	}
	public String getRegDate() {
		return regDate;
	}
	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}
	public int getNavColorVal() {
		return navColorVal;
	}
	public void setNavColorVal(int navColorVal) {
		this.navColorVal = navColorVal;
	}
}