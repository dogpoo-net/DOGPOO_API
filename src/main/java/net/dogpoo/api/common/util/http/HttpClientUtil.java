package net.dogpoo.api.common.util.http;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public final class HttpClientUtil {
	
	private final static Logger LOG = LoggerFactory.getLogger(HttpClientUtil.class);
	
	public static String get(String uri) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(uri);
		
		CloseableHttpResponse response = null;
		String responseBody = "";
		try {
			response = httpClient.execute(httpGet);
			responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(response != null) {
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responseBody;
	}
	
	public static String get(String uri, HashMap<String, String> headerMap) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(uri);
		
		for(Entry<String, String> headerEntry : headerMap.entrySet()) {
			httpGet.addHeader(headerEntry.getKey(), headerEntry.getValue());
		}
		
		CloseableHttpResponse response = null;
		String responseBody = "";
		try {
			response = httpClient.execute(httpGet);
			responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(response != null) {
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responseBody;
	}
	
	public static String post(String uri, HashMap<String, Object> paramMap) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setEntity(new StringEntity(new Gson().toJson(paramMap), "UTF-8"));
		
		CloseableHttpResponse response = null;
		String responseBody = "";
		try {
			response = httpClient.execute(httpPost);
			responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(response != null) {
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responseBody;
	}
	
	public static String post(String uri, HashMap<String, Object> paramMap, HashMap<String, String> headerMap) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setEntity(new StringEntity(new Gson().toJson(paramMap), "UTF-8"));
		
		CloseableHttpResponse response = null;
		String responseBody = "";
		try {
			response = httpClient.execute(httpPost);
			responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(response != null) {
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responseBody;
	}
	
	public static String requestChatGPTSessionPost(CloseableHttpClient httpClient, String uri, String token, HashMap<String, Object> paramMap) {
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setHeader("Authorization", "Bearer " + token);
		
		String jsonParam = new Gson().toJson(paramMap);
		httpPost.setEntity(new StringEntity(jsonParam, "UTF-8"));
		
		CloseableHttpResponse response = null;
		String responseBody = "";
		try {
			response = httpClient.execute(httpPost);
			responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (ConnectTimeoutException e) {
			LOG.error("CHAT_GPT_API_CONNECTION_TIMEOUT_EXCEPTION");
			e.printStackTrace();
			responseBody = "TIMEOUT";
		} catch (SocketTimeoutException e) {
			LOG.error("OPEN_AI_API_SOCKET_TIMEOUT_EXCEPTION");
//			e.printStackTrace();
			responseBody = "TIMEOUT";
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(response != null) response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return responseBody;
	}
	
	private static String getCookieStream(CloseableHttpResponse response) {
		String cookies = Arrays.stream(response.getHeaders("Set-Cookie"))
                .map(h -> h.getValue().split(";", 2)[0]) // "key=value" 부분만 추출
                .collect(Collectors.joining("; "));
		return cookies;
	}
	
	public static RequestConfig getTimeoutRequestConfig(int ms) {
		return RequestConfig.custom()
				.setConnectTimeout(ms)
				.setSocketTimeout(ms)
				.setConnectionRequestTimeout(ms)
				.build();
	}
}