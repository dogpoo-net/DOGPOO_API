package net.dogpoo.api.core.user.model;

import org.apache.ibatis.type.Alias;

@Alias("UserModel")
public class UserModel {
	String ccode;
	String userId;
	String passwd;
	String userNm;
	String userUuid;
	String userWaitCd;
	String countryCd;
	String picture;
	String userType;
	String socialType;
	String snsSignId;
	public String getCcode() {
		return ccode;
	}
	public void setCcode(String ccode) {
		this.ccode = ccode;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getPasswd() {
		return passwd;
	}
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	public String getUserNm() {
		return userNm;
	}
	public void setUserNm(String userNm) {
		this.userNm = userNm;
	}
	public String getUserUuid() {
		return userUuid;
	}
	public void setUserUuid(String userUuid) {
		this.userUuid = userUuid;
	}
	public String getCountryCd() {
		return countryCd;
	}
	public void setCountryCd(String countryCd) {
		this.countryCd = countryCd;
	}
	public String getUserWaitCd() {
		return userWaitCd;
	}
	public void setUserWaitCd(String userWaitCd) {
		this.userWaitCd = userWaitCd;
	}
	public String getPicture() {
		return picture;
	}
	public void setPicture(String picture) {
		this.picture = picture;
	}
	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}
	public String getSocialType() {
		return socialType;
	}
	public void setSocialType(String socialType) {
		this.socialType = socialType;
	}
	public String getSnsSignId() {
		return snsSignId;
	}
	public void setSnsSignId(String snsSignId) {
		this.snsSignId = snsSignId;
	}
}