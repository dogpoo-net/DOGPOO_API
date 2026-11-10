package net.dogpoo.api.core.convatar.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ConvatarScheduleMapper {
	
	public int insertCurrencyCountryList(HashMap<String, String> countryMap);
	
	@Update("UPDATE CURRENCY_COUNTRY_MAP SET isUse = 0")
	public void updateAllDisable();
	
	@Select("SELECT countryKey, currencyCd FROM CURRENCY_COUNTRY_MAP WHERE isUse")
	public List<HashMap<String, Object>> countryKeyList();
	
	@Update("UPDATE CURRENCY_VOLUME_MAP SET beforeRatio = afterRatio")
	public void copyAfterCurrencyToBefore();

	public int insertCurrencyVolumeData(HashMap<String, Object> currencyRateMap);
}
