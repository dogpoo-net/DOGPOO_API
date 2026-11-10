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
	
    private static final Gson JSON = new Gson();

    private HttpClientUtil() {}

    public static String get(String uri) {
        return get(uri, new HashMap<>());
    }

    public static String get(String uri, HashMap<String, String> headers) {
        var request = new HttpGet(uri);
        headers.forEach(request::setHeader);
        return execute(request);
    }

    public static String post(String uri, HashMap<String, Object> body) {
        return post(uri, body, new HashMap<>());
    }

    public static String post(String uri, HashMap<String, Object> body, HashMap<String, String> headers) {
        var request = new HttpPost(uri);
        request.setHeader("Content-Type", "application/json");
        headers.forEach(request::setHeader);
        request.setEntity(new StringEntity(JSON.toJson(body), java.nio.charset.StandardCharsets.UTF_8));
        return execute(request);
    }

    private static String execute(org.apache.http.client.methods.HttpUriRequest request) {
        // Retain legacy response-body/error conventions, including non-2xx bodies.
        try (var client = HttpClients.createDefault(); var response = client.execute(request)) {
            return response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException failure) {
            // URLs, headers and response bodies may contain credentials or private data.
            LOG.warn("Outbound HTTP request failed: {}", failure.getClass().getSimpleName());
            return "";
        }
    }

	public static String requestChatGPTSessionPost(CloseableHttpClient httpClient, String uri, String token, HashMap<String, Object> paramMap) {
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setHeader("Authorization", "Bearer " + token);
		
		String jsonParam = new Gson().toJson(paramMap);
		httpPost.setEntity(new StringEntity(jsonParam, "UTF-8"));
		
        try (var response = httpClient.execute(httpPost)) {
            return response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (ConnectTimeoutException | SocketTimeoutException timeout) {
            LOG.warn("Chat API request timed out");
            return "TIMEOUT";
        } catch (IOException failure) {
            LOG.warn("Chat API request failed: {}", failure.getClass().getSimpleName());
            return "";
        }
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