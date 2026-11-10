package net.dogpoo.api.core.convatar.model;

import org.apache.ibatis.type.Alias;

@Alias("ConvatarCurrencyModel")
public class ConvatarCurrencyModel {
	String countryKey;
	String currencyCd;
	String countryNmEN;
	String currencyNmEN;
	String countryNmKR;
	String currencyNmKR;
	String keywordOne;
	public String getCountryKey() {
		return countryKey;
	}
	public void setCountryKey(String countryKey) {
		this.countryKey = countryKey;
	}
	public String getCurrencyCd() {
		return currencyCd;
	}
	public void setCurrencyCd(String currencyCd) {
		this.currencyCd = currencyCd;
	}
	public String getCountryNmEN() {
		return countryNmEN;
	}
	public void setCountryNmEN(String countryNmEN) {
		this.countryNmEN = countryNmEN;
	}
	public String getCurrencyNmEN() {
		return currencyNmEN;
	}
	public void setCurrencyNmEN(String currencyNmEN) {
		this.currencyNmEN = currencyNmEN;
	}
	public String getCountryNmKR() {
		return countryNmKR;
	}
	public void setCountryNmKR(String countryNmKR) {
		this.countryNmKR = countryNmKR;
	}
	public String getCurrencyNmKR() {
		return currencyNmKR;
	}
	public void setCurrencyNmKR(String currencyNmKR) {
		this.currencyNmKR = currencyNmKR;
	}
	public String getKeywordOne() {
		return keywordOne;
	}
	public void setKeywordOne(String keywordOne) {
		this.keywordOne = keywordOne;
	}
}