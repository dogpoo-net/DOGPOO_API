package net.dogpoo.api.core.homepage.model;

import org.apache.ibatis.type.Alias;

@Alias("DogpooHomeMemberDataModel")
public class DogpooHomeMemberDataModel {
	int idx;
	String memberNm;
	String memberSubNm;
	String position;
	String memberImg;
	String description;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getMemberNm() {
		return memberNm;
	}
	public void setMemberNm(String memberNm) {
		this.memberNm = memberNm;
	}
	public String getMemberSubNm() {
		return memberSubNm;
	}
	public void setMemberSubNm(String memberSubNm) {
		this.memberSubNm = memberSubNm;
	}
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public String getMemberImg() {
		return memberImg;
	}
	public void setMemberImg(String memberImg) {
		this.memberImg = memberImg;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}