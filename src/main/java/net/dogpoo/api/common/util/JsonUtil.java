package net.dogpoo.api.common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;

public final class JsonUtil {
	
	public static JSONObject mapToJsonObject(HashMap<String, Object> paramMap) {
		JSONObject jsonObject = new JSONObject(paramMap);
		return jsonObject;
	}
	
	public static String listToJsonString(List<HashMap<String, String>> dataList) {
		String json = new Gson().toJson(dataList);
		return json;
	}
	
	public static HashMap<String, Object> jsonParamStrToObjectMap(String params) {
		HashMap<String, Object> paramMap = new HashMap<String, Object>();
		try {
			paramMap = new ObjectMapper().readValue(params, new TypeReference<HashMap<String, Object>>() {});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return paramMap;
	}
	
	public static HashMap<String, String> jsonParamStrToMap(String params) {
		HashMap<String, String> paramMap = new HashMap<String, String>();
		try {
			paramMap = new ObjectMapper().readValue(params, new TypeReference<HashMap<String, String>>() {});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return paramMap;
	}
	
	public static List<HashMap<String, String>> jsonParamStrToList(String params) {
		List<HashMap<String, String>> paramMap = new ArrayList<HashMap<String, String>>();
		try {
			paramMap = new ObjectMapper().readValue(params, new TypeReference<List<HashMap<String, String>>>() {});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return paramMap;
	}
	
	public static JSONObject parseJsonString(String jsonStr) {
		JSONParser parser = new JSONParser();
		JSONObject root = new JSONObject();
		try {
			root = (JSONObject) parser.parse(jsonStr);
		} catch (ParseException e) {
			System.out.println(jsonStr);
			e.printStackTrace();
		}
		return root;
	}
	
	public static boolean isJsonVaild(String responseBody) {
		if(responseBody.length() == 0) {
			return false;
		}
		String firstWord = responseBody.substring(0, 1);
		if("{".equals(firstWord) || "[".equals(firstWord)) {
			return true;
		} else {
			return false;
		}
	}
}