package net.dogpoo.api.core.convatar.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Select;

import net.dogpoo.api.core.convatar.model.ConvatarCurrencyModel;
import net.dogpoo.api.core.convatar.model.ConvatarCurrencyVolumeModel;

public interface ConvatarCurrencyMapper {
	
	@Select("SELECT countryKey, currencyCd, countryNmEN, currencyNmEN, countryNmKR, currencyNmKR, keywordOne FROM CURRENCY_COUNTRY_MAP WHERE isUse")
	public List<ConvatarCurrencyModel> getCountryList();
	
	public ConvatarCurrencyVolumeModel getEachCountryCurrencyVolume(HashMap<String, Object> paramMap);

}
