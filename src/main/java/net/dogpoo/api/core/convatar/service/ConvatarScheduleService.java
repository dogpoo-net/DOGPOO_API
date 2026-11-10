package net.dogpoo.api.core.convatar.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import net.dogpoo.api.common.util.DateUtil;
import net.dogpoo.api.common.util.http.HttpClientUtil;
import net.dogpoo.api.core.convatar.mapper.ConvatarScheduleMapper;

@Service
public class ConvatarScheduleService {
	
	@Autowired ConvatarScheduleMapper mapper;

	private static String serverType = System.getProperty("serverType");
	final static String EXIM_API_AUTH_KEY = System.getenv("EXIM_API_KEY");
	
	@Scheduled(cron="0 50 14 * * ?")
	public void executeCurrencyScheduleMain() throws IOException, ParseException {
		if(!"live".equals(serverType)) return;
		String curDate = DateUtil.getCurrentDate();
		getCurrencyCountryList(curDate);
		getCountryCurrencyMain(curDate);
	}
	
	public void getCurrencyCountryList(String curDate) throws ParseException {
		System.out.println("================ CURRENCY COUNTRY LIST UPDATE START ================");
		
		String uri = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@" + curDate + "/v1/country.json";
		String responseBody = HttpClientUtil.get(uri);
		
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(responseBody);
		mapper.updateAllDisable();
		for(Object nm : root.keySet()) {
			if(nm.toString().length() > 2) {
				continue;
			}
			JSONObject country = (JSONObject) root.get(nm.toString());
			String countryCd = nm.toString();
			String countryNm = country.get("country_name").toString();
			String currencyNm = country.get("currency_name").toString();
			String currencyCd = country.get("currency_code").toString();
			
			if(countryNm.length() > 60) continue;
			if("".equals(currencyCd)) continue;
			
			HashMap<String, String> countryMap = new HashMap<String, String>();
			countryMap.put("countryCd", countryCd);
			countryMap.put("countryNm", countryNm);
			countryMap.put("currencyNm", currencyNm);
			countryMap.put("currencyCd", currencyCd);
			mapper.insertCurrencyCountryList(countryMap);
		}
		
		System.out.println("================ CURRENCY COUNTRY LIST UPDATE END   ================");
	}
	
	public void getCountryCurrencyMain(String curDate) throws ParseException {
		System.out.println("================ CURRENCY MAIN SCHEDULE START ================");
		
		mapper.copyAfterCurrencyToBefore();
		List<HashMap<String, Object>> countryKeyList = mapper.countryKeyList();
		int cnt = 0;
		for(HashMap<String, Object> countryKeyMap : countryKeyList) {
			String countryKey = countryKeyMap.get("countryKey").toString();
			String currencyCd = countryKeyMap.get("currencyCd").toString();
			
			String uri = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@" + curDate + "/v1/currencies/" + currencyCd + ".min.json";
			String responseBody = HttpClientUtil.get(uri);
			
			JSONParser parser = new JSONParser();
			JSONObject root = (JSONObject) parser.parse(responseBody);
			String date = root.get("date").toString();
			
			JSONObject currencyObj = (JSONObject) root.get(currencyCd);
			
			for(HashMap<String, Object> innerCountryKeyMap : countryKeyList) {
				cnt++;
				String relativeCountryKey = innerCountryKeyMap.get("countryKey").toString();
				if(countryKey.equals(relativeCountryKey)) continue;
				
				String relativeCurrencyCd = innerCountryKeyMap.get("currencyCd").toString();
				if(currencyCd.equals(relativeCurrencyCd)) {
					HashMap<String, Object> currencyRateMap = new HashMap<String, Object>();
					currencyRateMap.put("countryKey", countryKey);
					currencyRateMap.put("relativeCountryKey", relativeCountryKey);
					currencyRateMap.put("afterRatio", 1);
					currencyRateMap.put("today", curDate);
					mapper.insertCurrencyVolumeData(currencyRateMap);
					continue;
				}
				
				double rate = Double.parseDouble(String.valueOf(currencyObj.get(relativeCurrencyCd)));
				
				HashMap<String, Object> currencyRateMap = new HashMap<String, Object>();
				currencyRateMap.put("countryKey", countryKey);
				currencyRateMap.put("relativeCountryKey", relativeCountryKey);
				currencyRateMap.put("afterRatio", rate);
				currencyRateMap.put("today", date);
				mapper.insertCurrencyVolumeData(currencyRateMap);
			}
			System.out.println("CURRENCY MAIN :: " + cnt + " ROW UPDATED.");
		}
		System.out.println("================ CURRENCY MAIN SCHEDULE END ================");
	}
}