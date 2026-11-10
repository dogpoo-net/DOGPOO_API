package net.dogpoo.api.core.homepage.model;

import org.apache.ibatis.type.Alias;

@Alias("DogpooHomeBizDataModel")
public class DogpooHomeBizDataModel {
	String bizNo;
	String bizNm;
	String bizNmEn;
	String bizAddress;
	String ceoNm;
	String sellingNo;
	String csEmail;
	String csPhone;
	public String getBizNo() {
		return bizNo;
	}
	public void setBizNo(String bizNo) {
		this.bizNo = bizNo;
	}
	public String getBizNm() {
		return bizNm;
	}
	public void setBizNm(String bizNm) {
		this.bizNm = bizNm;
	}
	public String getBizNmEn() {
		return bizNmEn;
	}
	public void setBizNmEn(String bizNmEn) {
		this.bizNmEn = bizNmEn;
	}
	public String getBizAddress() {
		return bizAddress;
	}
	public void setBizAddress(String bizAddress) {
		this.bizAddress = bizAddress;
	}
	public String getCeoNm() {
		return ceoNm;
	}
	public void setCeoNm(String ceoNm) {
		this.ceoNm = ceoNm;
	}
	public String getSellingNo() {
		return sellingNo;
	}
	public void setSellingNo(String sellingNo) {
		this.sellingNo = sellingNo;
	}
	public String getCsEmail() {
		return csEmail;
	}
	public void setCsEmail(String csEmail) {
		this.csEmail = csEmail;
	}
	public String getCsPhone() {
		return csPhone;
	}
	public void setCsPhone(String csPhone) {
		this.csPhone = csPhone;
	}
}