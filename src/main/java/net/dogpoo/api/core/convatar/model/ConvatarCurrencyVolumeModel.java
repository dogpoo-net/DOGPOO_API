package net.dogpoo.api.core.convatar.model;

import org.apache.ibatis.type.Alias;

@Alias("ConvatarCurrencyVolumeModel")
public class ConvatarCurrencyVolumeModel {
	
	String countryKey;
	String relativeCountryKey;
	double afterRatio;
	double curCurrency;
	double compareYesterday;
	String today;
	public String getCountryKey() {
		return countryKey;
	}
	public void setCountryKey(String countryKey) {
		this.countryKey = countryKey;
	}
	public String getRelativeCountryKey() {
		return relativeCountryKey;
	}
	public void setRelativeCountryKey(String relativeCountryKey) {
		this.relativeCountryKey = relativeCountryKey;
	}
	public double getAfterRatio() {
		return afterRatio;
	}
	public void setAfterRatio(double afterRatio) {
		this.afterRatio = afterRatio;
	}
	public double getCurCurrency() {
		return curCurrency;
	}
	public void setCurCurrency(double curCurrency) {
		this.curCurrency = curCurrency;
	}
	public double getCompareYesterday() {
		return compareYesterday;
	}
	public void setCompareYesterday(double compareYesterday) {
		this.compareYesterday = compareYesterday;
	}
	public String getToday() {
		return today;
	}
	public void setToday(String today) {
		this.today = today;
	}
}
