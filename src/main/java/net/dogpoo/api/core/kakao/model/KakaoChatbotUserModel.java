package net.dogpoo.api.core.kakao.model;

public class KakaoChatbotUserModel {
	int idx;
	String userId;
	String mode;
	String userNm;
	String gender;
	String tutorNm;
	String tutorGender;
	boolean isGenTutor;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getUserNm() {
		return userNm;
	}
	public void setUserNm(String userNm) {
		this.userNm = userNm;
	}
	public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public String getTutorNm() {
		return tutorNm;
	}
	public void setTutorNm(String tutorNm) {
		this.tutorNm = tutorNm;
	}
	public String getTutorGender() {
		return tutorGender;
	}
	public void setTutorGender(String tutorGender) {
		this.tutorGender = tutorGender;
	}
	public boolean isGenTutor() {
		return isGenTutor;
	}
	public void setGenTutor(boolean isGenTutor) {
		this.isGenTutor = isGenTutor;
	}
}