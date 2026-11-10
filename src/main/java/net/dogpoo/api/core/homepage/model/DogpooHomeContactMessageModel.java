package net.dogpoo.api.core.homepage.model;

import org.apache.ibatis.type.Alias;

@Alias("DogpooHomeContactMessageModel")
public class DogpooHomeContactMessageModel {
	int idx;
	String contactType;
	String contacter;
	String contacterEmail;
	String contacterPhone;
	String contacterUrl;
	String contactMessage;
	boolean readYn;
	String regDate;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getContactType() {
		return contactType;
	}
	public void setContactType(String contactType) {
		this.contactType = contactType;
	}
	public String getContacter() {
		return contacter;
	}
	public void setContacter(String contacter) {
		this.contacter = contacter;
	}
	public String getContacterEmail() {
		return contacterEmail;
	}
	public void setContacterEmail(String contacterEmail) {
		this.contacterEmail = contacterEmail;
	}
	public String getContacterPhone() {
		return contacterPhone;
	}
	public void setContacterPhone(String contacterPhone) {
		this.contacterPhone = contacterPhone;
	}
	public String getContacterUrl() {
		return contacterUrl;
	}
	public void setContacterUrl(String contacterUrl) {
		this.contacterUrl = contacterUrl;
	}
	public String getContactMessage() {
		return contactMessage;
	}
	public void setContactMessage(String contactMessage) {
		this.contactMessage = contactMessage;
	}
	public boolean isReadYn() {
		return readYn;
	}
	public void setReadYn(boolean readYn) {
		this.readYn = readYn;
	}
	public String getRegDate() {
		return regDate;
	}
	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}
}