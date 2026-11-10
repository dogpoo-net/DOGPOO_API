package net.dogpoo.api.core.homepage.model;

import org.apache.ibatis.type.Alias;

@Alias("DogpooHomeWorksModel")
public class DogpooHomeWorksModel {
	int idx;
	String workTitle;
	String workSubTitle;
	String workImg;
	String workLink;
	String description;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getWorkTitle() {
		return workTitle;
	}
	public void setWorkTitle(String workTitle) {
		this.workTitle = workTitle;
	}
	public String getWorkSubTitle() {
		return workSubTitle;
	}
	public void setWorkSubTitle(String workSubTitle) {
		this.workSubTitle = workSubTitle;
	}
	public String getWorkImg() {
		return workImg;
	}
	public void setWorkImg(String workImg) {
		this.workImg = workImg;
	}
	public String getWorkLink() {
		return workLink;
	}
	public void setWorkLink(String workLink) {
		this.workLink = workLink;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}