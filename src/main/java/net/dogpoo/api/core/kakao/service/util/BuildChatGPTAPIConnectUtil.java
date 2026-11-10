package net.dogpoo.api.core.kakao.service.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import net.dogpoo.api.common.util.http.HttpClientUtil;

public final class BuildChatGPTAPIConnectUtil {
	
	private final static String TOKEN = System.getenv("CHATGPT_API_KEY");
	
	public static String buildChatGPTAPIConnection(List<HashMap<String, String>> inputs, int timeoutMilli, String modelNm) {
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(HttpClientUtil.getTimeoutRequestConfig(timeoutMilli)).build();
		String url = "https://api.openai.com/v1/responses";

		HashMap<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("model", modelNm);
		paramMap.put("input", inputs);
		
		String responseBody = HttpClientUtil.requestChatGPTSessionPost(httpClient, url, TOKEN, paramMap);
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return responseBody;
	}
}