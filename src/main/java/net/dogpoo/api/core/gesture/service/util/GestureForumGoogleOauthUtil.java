package net.dogpoo.api.core.gesture.service.util;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import net.dogpoo.api.common.util.JsonUtil;

public final class GestureForumGoogleOauthUtil {

	public static HashMap<String, String> getOauthToken(HashMap<String, Object> paramMap) throws IOException {
		
		String code = paramMap.get("code").toString();
		String clientId = paramMap.get("clientId").toString();
		String clientSecret = paramMap.get("clientSecret").toString();
		String redirectUrl = paramMap.get("redirectUrl").toString();
		String grantType = paramMap.get("grantType").toString();
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://oauth2.googleapis.com/token");
		
		String reqBody = "{\"code\": \"" + code + "\", \"client_id\": \"" + clientId + "\", \"client_secret\": \"" + clientSecret + "\", \"redirect_uri\": \"" + redirectUrl + "\", \"grant_type\": \"" + grantType + "\"}";
		StringEntity entity = new StringEntity(reqBody);
		httpPost.setEntity(entity);
		
		CloseableHttpResponse response = null;
		String responseBody = "";
		try {
			response = httpClient.execute(httpPost);
			responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(null != response) {
				response.close();
			}
		}
		HashMap<String, String> accessTokenMap = JsonUtil.jsonParamStrToMap(responseBody);
		String accessToken = accessTokenMap.get("access_token");
		HashMap<String, String> userInfoMap = getOauthUserInfo(httpClient, accessToken);
		return userInfoMap;
	}
	
	private static HashMap<String, String> getOauthUserInfo(CloseableHttpClient httpClient, String accessToken) throws IOException {
		HttpGet httpGet = new HttpGet("https://www.googleapis.com/userinfo/v2/me?access_token=" + accessToken);
		CloseableHttpResponse response = null;
		String responseBody = "";
		try {
			response = httpClient.execute(httpGet);
			responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(null != response) {
				response.close();
			}
			httpClient.close();
		}
		HashMap<String, String> userInfoMap = JsonUtil.jsonParamStrToMap(responseBody);
		return userInfoMap;
	}
}