package net.dogpoo.api.core.kakao.service.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import net.dogpoo.api.common.util.http.HttpClientUtil;
import net.dogpoo.api.core.kakao.mapper.KakaoChatbotEnglishMapper;
import net.dogpoo.api.core.kakao.model.KakaoChatbotConversationModel;

@Service
public class CallbackAsyncConnectUtilService {
	
	@Autowired KakaoChatbotEnglishMapper mapper;
	private final static String TOKEN = System.getenv("CHATGPT_API_KEY");
	private final static Logger LOG = LoggerFactory.getLogger(CallbackAsyncConnectUtilService.class);
	
	@Async
	public void buildChatGPTAPIConnection(List<HashMap<String, String>> inputs, int timeoutMilli, String modelNm, KakaoChatbotConversationModel model) {
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(HttpClientUtil.getTimeoutRequestConfig(timeoutMilli)).build();
		String url = "https://api.openai.com/v1/responses";

		HashMap<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("model", modelNm);
		paramMap.put("input", inputs);
		
		long currentMilli = System.currentTimeMillis();
		String responseBody = HttpClientUtil.requestChatGPTSessionPost(httpClient, url, TOKEN, paramMap);
		long workoutMilli = System.currentTimeMillis();
		int runTime = (int) (workoutMilli - currentMilli);
		
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if("TIMEOUT".equals(responseBody)) {
			model.setOutput(mapper.getRandomErrMsg());
		} else {
			HashMap<String, String> outputMap = ModelParseUtil.getParsingOutputMap(responseBody);
			String answer = outputMap.get("answer");
			int inputTokens = Integer.parseInt(outputMap.get("inputTokens"));
			int outputTokens = Integer.parseInt(outputMap.get("outputTokens"));
			int totalTokens = Integer.parseInt(outputMap.get("totalTokens"));
			model.setOutput(answer);
			model.setInputTokens(inputTokens);
			model.setOutputTokens(outputTokens);
			model.setTotalTokens(totalTokens);
		}
		
		model.setRunTime(runTime);
		mapper.updateChatbotAIAnswerContent(model);
	}
	
	@Async
	public void buildKakaoChatbotCallbackConnection(String callbackUrl, int idx, KakaoChatbotConversationModel model, String target) {
		String answer = "";
		// 타임아웃 시간보다 몇번 더 돈다. 어차피 에러 메시지가 타임아웃으로 테이블에 업데이트 될 것이기 때문에 알아서 break 된다.
		for(int i = 0; i <= 25; i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			answer = mapper.getChatbotAIAnswerContent(idx);
			if(answer != null && answer.length() > 0) {
				HashMap<String, Object> paramMap = new HashMap<String, Object>();
				if("SIMPLE_TEXT".equals(target)) {
					paramMap = ModelParseUtil.setCallbackResponseSimpleText(answer);
				} else {
					paramMap = ModelParseUtil.setCallbackResponseBasicCard(answer);
				}
				HttpClientUtil.post(callbackUrl, paramMap);
				break;
			}
		}
		
		LOG.info("====================================================");
		System.out.println("RUNTIME => " + model.getRunTime());
		System.out.println("USERID ==> " + model.getUserId());
		System.out.println("INPUT ===> " + model.getInput());
		System.out.println("OUTPUT ==> " + answer);
		System.out.println("CALLBACK=> *** CALLBACK ***");
		LOG.info("====================================================");
	}
}