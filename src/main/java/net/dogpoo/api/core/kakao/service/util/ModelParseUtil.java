package net.dogpoo.api.core.kakao.service.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.dogpoo.api.common.util.JsonUtil;

public final class ModelParseUtil {
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, String> getRequestParams(HashMap<String, Object> paramMap) {
		HashMap<String, Object> userReqMap = (HashMap<String, Object>) paramMap.get("userRequest");
		
		String question = "";
		String callbackUrl = "";
		if(null != userReqMap.get("utterance")) question = (String) userReqMap.get("utterance");
		if(null != userReqMap.get("callbackUrl")) callbackUrl = (String) userReqMap.get("callbackUrl");
		
		HashMap<String, Object> userMap = (HashMap<String, Object>) userReqMap.get("user");
		String userId = (String) userMap.get("id");

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("userId", userId);
		params.put("question", question);
		params.put("callbackUrl", callbackUrl);
		
		HashMap<String, Object> actionMap = (HashMap<String, Object>) paramMap.get("action");
		if(null != actionMap) {
			HashMap<String, Object> customParams = (HashMap<String, Object>) actionMap.get("params");
			String mode = (String) customParams.get("mode");
			String memoryLimit = (String) customParams.get("memoryLimit");
			String summaryLimit = (String) customParams.get("summaryLimit");
			String welcomeKey = (String) customParams.get("welcomeKey");
			
			HashMap<String, Object> clientExtra = (HashMap<String, Object>) actionMap.get("clientExtra");
			String gender = (String) clientExtra.get("gender");
			String userLv = (String) clientExtra.get("userLv");
			
			params.put("mode", mode);
			params.put("memoryLimit", memoryLimit);
			params.put("summaryLimit", summaryLimit);
			params.put("welcomeKey", welcomeKey);
			if(null != gender) params.put("gender", gender);
			if(null != userLv) params.put("userLv", userLv);
		}
		
		return params;
	}

	public static String getParsingOutputText(String responseBody) {
		JSONObject root = JsonUtil.parseJsonString(responseBody);
		
		String answer = "";
		try {
			JSONArray outputs = (JSONArray) root.get("output");
			for(int i = 0; i < outputs.size(); i++) {
				JSONObject output = (JSONObject) outputs.get(i);
				String type = (String) output.get("type");
				if("message".equals(type)) {
					String status = (String) output.get("status");
					if("completed".equals(status)) {
						JSONArray contents = (JSONArray) output.get("content");
						JSONObject content = (JSONObject) contents.get(0);
						answer = content.get("text").toString();
						break;
					} else {
						continue;
					}
				} else {
					continue;
				}
			}
		} catch (NullPointerException e) {
			System.out.println(responseBody);
			e.printStackTrace();
		}
		
		return answer;
	}
	
	public static HashMap<String, String> getParsingOutputMap(String responseBody) {
		JSONObject root = JsonUtil.parseJsonString(responseBody);
		
		HashMap<String, String> outputMap = new HashMap<String, String>();
		try {
			JSONArray outputs = (JSONArray) root.get("output");
			JSONObject output = (JSONObject) outputs.get(1);
			JSONArray contents = (JSONArray) output.get("content");
			JSONObject content = (JSONObject) contents.get(0);
			String answer = content.get("text").toString();
			
			JSONObject usage = (JSONObject) root.get("usage");
			String inputTokens = String.valueOf(usage.get("input_tokens"));
			String outputTokens = String.valueOf(usage.get("output_tokens"));
			String totalTokens = String.valueOf(usage.get("total_tokens"));
			
			outputMap.put("answer", answer);
			outputMap.put("inputTokens", inputTokens);
			outputMap.put("outputTokens", outputTokens);
			outputMap.put("totalTokens", totalTokens);
		} catch (NullPointerException e) {
			System.out.println(responseBody);
			e.printStackTrace();
		}
		
		return outputMap;
	}
	
	public static HashMap<String, Object> setResponseSimpleText(String answer, boolean isCallback) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		if(!isCallback) {
			HashMap<String, Object> simpleText = new HashMap<String, Object>();
			simpleText.put("text", answer);
			
			HashMap<String, Object> output = new HashMap<String, Object>();
			output.put("simpleText", simpleText);
			
			List<HashMap<String, Object>> outputs = new ArrayList<HashMap<String, Object>>();
			outputs.add(output);
			
			HashMap<String, Object> template = new HashMap<String, Object>();
			template.put("outputs", outputs);
			resMap.put("template", template);
		}
		
		resMap.put("version", "2.0");
		if(isCallback) resMap.put("useCallback", true);
		
		return resMap;
	}
	
	public static HashMap<String, Object> setCallbackResponseSimpleText(String answer) {
		
		HashMap<String, Object> simpleText = new HashMap<String, Object>();
		simpleText.put("text", answer);
		
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("simpleText", simpleText);
		
		List<HashMap<String, Object>> outputs = new ArrayList<HashMap<String, Object>>();
		outputs.add(output);
		
		HashMap<String, Object> template = new HashMap<String, Object>();
		template.put("outputs", outputs);
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("version", "2.0");
		resMap.put("useCallback", true);
		resMap.put("template", template);
		
		return resMap;
	}
	
	public static HashMap<String, Object> setResponseBasicCard(String answer, boolean isCallback) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		if(!isCallback) {
			HashMap<String, Object> thumbnail = new HashMap<String, Object>();
			thumbnail.put("imageUrl", "https://image.dongnyang.com/resources/dogpoo/tutor/dogpooAI.jpg");
			
			HashMap<String, Object> basicCard = new HashMap<String, Object>();
			basicCard.put("title", "AI 선생님이 생성되었습니다!");
			basicCard.put("description", answer);
			basicCard.put("thumbnail", thumbnail);
			
			HashMap<String, Object> output = new HashMap<String, Object>();
			output.put("basicCard", basicCard);
			
			List<HashMap<String, Object>> outputs = new ArrayList<HashMap<String, Object>>();
			outputs.add(output);
			
			HashMap<String, Object> template = new HashMap<String, Object>();
			template.put("outputs", outputs);
			resMap.put("template", template);
		}
		
		resMap.put("version", "2.0");
		if(isCallback) resMap.put("useCallback", true);
		
		return resMap;
	}
	
	public static HashMap<String, Object> setCallbackResponseBasicCard(String answer) {
		
		HashMap<String, Object> thumbnail = new HashMap<String, Object>();
		thumbnail.put("imageUrl", "https://image.dongnyang.com/resources/dogpoo/tutor/dogpooAI2.jpg");
		thumbnail.put("fixedRatio", true);
		
		HashMap<String, Object> basicCard = new HashMap<String, Object>();
		basicCard.put("title", "AI 선생님이 생성되었습니다!");
		basicCard.put("description", answer);
		basicCard.put("thumbnail", thumbnail);
		
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("basicCard", basicCard);
		
		List<HashMap<String, Object>> outputs = new ArrayList<HashMap<String, Object>>();
		outputs.add(output);
		
		HashMap<String, Object> template = new HashMap<String, Object>();
		template.put("outputs", outputs);
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("version", "2.0");
		resMap.put("useCallback", true);
		resMap.put("template", template);
		
		return resMap;
	}
	
	public static HashMap<String, Object> setResponseAskGenderTextCard() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		List<HashMap<String, Object>> buttons = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> button = new HashMap<String, Object>();
		button.put("action", "block");
		button.put("label", "남");
		button.put("messageText", "남");
		button.put("blockId", "6898a3d4627dea71c795bcaa");
		
		HashMap<String, Object> genderMap = new HashMap<String, Object>();
		genderMap.put("gender", "M");
		button.put("extra", genderMap);
		buttons.add(button);
		
		button = new HashMap<String, Object>();
		button.put("action", "block");
		button.put("label", "여");
		button.put("messageText", "여");
		button.put("blockId", "6898a3d4627dea71c795bcaa");
		
		genderMap = new HashMap<String, Object>();
		genderMap.put("gender", "F");
		button.put("extra", genderMap);
		buttons.add(button);
		
		HashMap<String, Object> textCard = new HashMap<String, Object>();
		textCard.put("title", "도그푸 AI 영어과외에 오신것을 환영합니다!");
		textCard.put("description", "안녕하세요!\n함께 공부를 하기 전에, 당신에 대한 정보가 없네요. 당신의 성별은 뭐죠?");
		textCard.put("buttons", buttons);
		
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("textCard", textCard);
		
		List<HashMap<String, Object>> outputs = new ArrayList<HashMap<String, Object>>();
		outputs.add(output);
		
		HashMap<String, Object> template = new HashMap<String, Object>();
		template.put("outputs", outputs);
		resMap.put("template", template);
		
		resMap.put("version", "2.0");
		
		return resMap;
	}
	
	public static HashMap<String, Object> setResponseAskUserLevelTextCard() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		List<HashMap<String, Object>> buttons = new ArrayList<HashMap<String, Object>>();
		
		for(int i = 1; i <= 10; i++) {
			HashMap<String, Object> button = new HashMap<String, Object>();
			button.put("action", "block");
			button.put("label", i + "");
			button.put("messageText", i + "");
			button.put("blockId", "6898de86f06e075c82f5db0e");
			
			HashMap<String, Object> extra = new HashMap<String, Object>();
			extra.put("userLv", i + "");
			button.put("extra", extra);
			buttons.add(button);
		}
		
		HashMap<String, Object> textCard = new HashMap<String, Object>();
		textCard.put("description", "현재 당신의 영어 레벨은 몇 정도라고 생각하시나요?\n1부터 10까지 수치를 선택해주세요. 정확하지 않아도 괜찮아요!");
		textCard.put("buttons", buttons);
		
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("textCard", textCard);
		
		List<HashMap<String, Object>> outputs = new ArrayList<HashMap<String, Object>>();
		outputs.add(output);
		
		HashMap<String, Object> template = new HashMap<String, Object>();
		template.put("outputs", outputs);
		resMap.put("template", template);
		
		resMap.put("version", "2.0");
		
		return resMap;
	}
	
	public static HashMap<String, Object> setResponseAskUserLevelQuickReplies() {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		List<HashMap<String, Object>> quickReplies = new ArrayList<HashMap<String, Object>>();
		
		for(int i = 1; i <= 10; i++) {
			HashMap<String, Object> quickReply = new HashMap<String, Object>();
			quickReply.put("action", "block");
			quickReply.put("label", i + "");
			quickReply.put("messageText", i + "");
			quickReply.put("blockId", "6898de86f06e075c82f5db0e");
			
			HashMap<String, Object> extra = new HashMap<String, Object>();
			extra.put("userLv", i + "");
			quickReply.put("extra", extra);
			quickReplies.add(quickReply);
		}
		
		HashMap<String, Object> simpleText = new HashMap<String, Object>();
		simpleText.put("text", "현재 당신의 영어 레벨은 몇 정도라고 생각하시나요?\n1부터 10까지 수치를 선택해주세요. 정확하지 않아도 괜찮아요!");
		
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("simpleText", simpleText);
		
		List<HashMap<String, Object>> outputs = new ArrayList<HashMap<String, Object>>();
		outputs.add(output);
		
		HashMap<String, Object> template = new HashMap<String, Object>();
		template.put("quickReplies", quickReplies);
		template.put("outputs", outputs);
		resMap.put("template", template);
		
		resMap.put("version", "2.0");
		
		return resMap;
	}
}