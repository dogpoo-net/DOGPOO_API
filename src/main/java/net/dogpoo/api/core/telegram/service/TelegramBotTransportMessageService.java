package net.dogpoo.api.core.telegram.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import net.dogpoo.api.core.telegram.mapper.TelegramBotTransportMessageMapper;

@Service
public class TelegramBotTransportMessageService {

    public TelegramBotTransportMessageService(TelegramBotTransportMessageMapper mapper) {
        this.mapper = mapper;
    }

	
	private final TelegramBotTransportMessageMapper mapper;
	private final static Logger LOGGER = LoggerFactory.getLogger(TelegramBotTransportMessageService.class);
	private final static String HOST = "https://api.telegram.org/bot";
	private final static String PATH = System.getenv("TELEGRAM_BOT_PATH");
	
	public HashMap<String, String> getTokenMap(String ccode, boolean isExternal) {
		return isExternal ? mapper.getTelegramBotAccessToken(ccode) : mapper.getTelegramBotAccessTokenForExternal(ccode);
	}

	/**
	 * 
	 * @param ccode 고객코드
	 * @param msg 메시지
	 * @param isExternal 외부 Request인지 판별하는 플래그, true일 때 동냥이 업비트 텔레그램 useYn가 1이면 돌아간다.
	 * @return
	 * @throws IOException
	 */
	public boolean transportTelegramMsgMain(String ccode, String msg, boolean isExternal) throws IOException {
		boolean resultFl = false;
		HashMap<String, String> tokenMap = getTokenMap(ccode, isExternal);
		if(null == tokenMap || null == tokenMap.get("accessKey")) return resultFl;
		
		String accessKey = tokenMap.get("accessKey");
		String chatId = tokenMap.get("chatId");
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String uri = getUri(accessKey, chatId, msg);
		HttpGet httpGet = new HttpGet(uri);
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
			String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
			if(isSuccess(responseBody)) {
				LOGGER.info(ccode + " :: 메시지 전송 성공");
				resultFl = true;
			} else {
				LOGGER.info(ccode + " :: 메시지 전송 실패 => " + responseBody);
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.info(ccode + " :: 메시지 전송 오류");
		} finally {
			if(response != null) {
				response.close();
			}
		}
		httpClient.close();
		return resultFl;
	}
	
	public String getUri(String accessKey, String chatId, String msg) throws UnsupportedEncodingException {
		StringBuffer uri = new StringBuffer(HOST);
		uri.append(accessKey)
		.append(PATH)
		.append(getRequestParam(chatId, msg));
		return uri.toString();
	}
	
	public String getRequestParam(String chatId, String msg) throws UnsupportedEncodingException {
		StringBuffer query = new StringBuffer();
		query.append("?chat_id=")
		.append(chatId)
		.append("&text=")
		.append(URLEncoder.encode(msg, "UTF-8"));
		return query.toString();
	}
	
	private boolean isSuccess(String responseBody) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(responseBody);
		boolean result = (boolean) root.get("ok");
		return result;
	}
}
