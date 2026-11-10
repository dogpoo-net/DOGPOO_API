package net.dogpoo.api.core.kakao.model;

import org.apache.ibatis.type.Alias;

@Alias("KakaoChatbotConversationModel")
public class KakaoChatbotConversationModel {
	int idx;
	String mode;
	int chatSeq = 1;
	String userId;
	String input;
	String output;
	int inputTokens;
	int outputTokens;
	int totalTokens;
	int runTime;
	String regDate;
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public int getChatSeq() {
		return chatSeq;
	}
	public void setChatSeq(int chatSeq) {
		this.chatSeq = chatSeq;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getInput() {
		return input;
	}
	public void setInput(String input) {
		this.input = input;
	}
	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	public int getInputTokens() {
		return inputTokens;
	}
	public void setInputTokens(int inputTokens) {
		this.inputTokens = inputTokens;
	}
	public int getOutputTokens() {
		return outputTokens;
	}
	public void setOutputTokens(int outputTokens) {
		this.outputTokens = outputTokens;
	}
	public int getTotalTokens() {
		return totalTokens;
	}
	public void setTotalTokens(int totalTokens) {
		this.totalTokens = totalTokens;
	}
	public int getRunTime() {
		return runTime;
	}
	public void setRunTime(int runTime) {
		this.runTime = runTime;
	}
	public String getRegDate() {
		return regDate;
	}
	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}
}