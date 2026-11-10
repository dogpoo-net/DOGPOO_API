package net.dogpoo.api.core.gesture.service.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class GestureForumThreadImageContentUtil {
	
	final static String IMAGE_SERVER_HOST = "https://image.dongnyang.com";
	final static String IMAGE_INPUT_ENDPOINT = "/dongnyang/image/input";
	final static String IMAGE_DELETE_ENDPOINT = "/dongnyang/image/delete";
	final static String IMAGE_SPLIT_DELIMITER = "<img src=\"https://image.dongnyang.com";
	final static String IMAGE_SPLIT_BLOB_DELIMITER = "<img src=\"";
	final static String CONTENT_TYPE_JSON = "application/json;charset=utf-8";
	
	public static HashMap<String, Object> getInputImagePathHtmlType(String htmlContent) throws IOException {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		List<String> imgBlobList = getImagePathList(IMAGE_SPLIT_BLOB_DELIMITER, htmlContent);
		int code = 0;
		String msg = "";
		for(String imgBlobData : imgBlobList) {
			String dataStr = imgBlobData.substring(0, 11);
			if(!"data:image/".equals(dataStr)) {
				continue;
			}
			String[] blobArr = imgBlobData.split(",");
			String meta = blobArr[0];
			String imageBlob = blobArr[1];
			String dataForm = meta.split(";")[0];
			String ext = dataForm.split("/")[1];
			
			if(!"jpg".equals(ext) && !"png".equals(ext) && !"jpeg".equals(ext) && !"gif".equals(ext)) {
				continue;
			}
			
			HashMap<String, Object> imgMap = new HashMap<String, Object>();
			imgMap.put("src", imgBlobData);
			imgMap.put("brand", "gesture");
			String responseBody = getInputImagePathRequest(httpClient, imgMap);
			JsonObject responseMap = convertResponseBodyToJsonObject(responseBody);
			
			code = responseMap.get("code").getAsInt();
			if(code > 0) {
				msg = responseMap.get("msg").toString();
			}
			String outerPath = String.valueOf(responseMap.get("OUTER_PATH").getAsString());
			StringBuffer outerPathBuff = new StringBuffer(IMAGE_SERVER_HOST);
			outerPathBuff.append(outerPath);
			htmlContent = htmlContent.replace(imgBlobData, outerPathBuff.toString());
		}
		
		httpClient.close();
		
		if(code == 0) {
			resMap.put("code", code);
			resMap.put("msg", msg);
			resMap.put("content", htmlContent);
		} else {
			resMap.put("code", code);
			resMap.put("msg", msg);
		}
		return resMap;
	}
	
	public static HashMap<String, Object> getInputImagePathDeltaType(Object contentMap) throws IOException {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		
		@SuppressWarnings("unchecked")
		HashMap<String, Object> postContent = (HashMap<String, Object>) contentMap;
		@SuppressWarnings("unchecked")
		List<HashMap<String, Object>> ops = (List<HashMap<String, Object>>) postContent.get("ops");
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		StringBuffer textBuffer = new StringBuffer();
		
		int code = 0;
		String msg = "";
		for(HashMap<String, Object> op : ops) {
			String insert = "";
			if(op.get("insert") instanceof String) {
				if(null != op.get("insert")) {
					insert = String.valueOf(op.get("insert"));
				}
			} else {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> imageMap = (HashMap<String, Object>) op.get("insert");
				if(imageMap.containsKey("image")) {
					insert = imageMap.get("image").toString();
					String outerPath = String.valueOf(imageMap.get("image"));
					StringBuffer imageBuffer = new StringBuffer();
					if("data:image/".equals(insert.substring(0, 11))) {
						
						String[] srcArr = insert.split(",");
						String header = srcArr[0];
						String fileTypeHeader = header.substring(header.indexOf("data:") + 5, header.indexOf(";"));
						String[] fileTypeArr = fileTypeHeader.split("/");
						String ext = fileTypeArr[1];
						if(!"jpg".equals(ext) && !"png".equals(ext) && !"jpeg".equals(ext) && !"gif".equals(ext)) {
							continue;
						}
						
						String imageBlob = insert.substring(0, insert.length());
						HashMap<String, Object> imgMap = new HashMap<String, Object>();
						imgMap.put("src", imageBlob);
						imgMap.put("brand", "gesture");
						String responseBody = getInputImagePathRequest(httpClient, imgMap);
						JsonObject responseMap = convertResponseBodyToJsonObject(responseBody);
						
						code = responseMap.get("code").getAsInt();
						if(code > 0) {
							msg = responseMap.get("msg").toString();
						}
						outerPath = String.valueOf(responseMap.get("OUTER_PATH").getAsString());
						imageBuffer.append("<img src=\"")
						.append(IMAGE_SERVER_HOST)
						.append(outerPath)
						.append("\">");
					} else {
						imageBuffer.append("<img src=\"")
						.append(outerPath)
						.append("\">");
					}
					insert = imageBuffer.toString();
				}
			}
			
			textBuffer.append(insert.replace("\n", "<br>"));
		}
		httpClient.close();
		
		if(code == 0) {
			resMap.put("code", code);
			resMap.put("msg", msg);
			resMap.put("content", textBuffer.toString());
		} else {
			resMap.put("code", code);
			resMap.put("msg", msg);
		}
		return resMap;
	}
	
	public static List<String> getImagePathList(String delimiter, String content) {
		List<Integer> indexList = new ArrayList<Integer> ();
		int index = content.indexOf(delimiter);
		
		while(index != -1) {
			indexList.add(index + delimiter.length());
			index = content.indexOf(delimiter, index + delimiter.length());
		}
		
		List<String> imgPathList = new ArrayList<String>();
		for(int idx : indexList) {
			int endIdx = content.indexOf("\"", idx);
			String imgInnerPath = content.substring(idx, endIdx);
			imgPathList.add(imgInnerPath);
		}
		
		return imgPathList;
	}
	
	public static HashMap<String, Object> updateContentImageFilter(HashMap<String, Object> paramMap, String oldContent, String newContent) throws IOException {
		List<String> oldImgPathList = getImagePathList(IMAGE_SPLIT_DELIMITER, oldContent);
		List<String> newImgPathList = getImagePathList(IMAGE_SPLIT_DELIMITER, newContent);
		List<String> removeImgPathList = new ArrayList<String>();
		for(String oldImgPath : oldImgPathList) {
			boolean isFind = false;
			for(String newImgPath : newImgPathList) {
				if(oldImgPath.equals(newImgPath)) {
					isFind = true;
					break;
				}
			}
			if(!isFind) {
				removeImgPathList.add(oldImgPath);
			}
		}
		HashMap<String, Object> imgDelResMap = getDeleteImagePathRequest(removeImgPathList);
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		int code = (int) imgDelResMap.get("code");
		String msg = String.valueOf(imgDelResMap.get("msg"));
		resMap.put("code", code);
		resMap.put("msg", msg);
		return resMap;
	}
	
	public static String getInputImagePathRequest(CloseableHttpClient httpClient, HashMap<String, Object> imgMap) throws IOException {
		HttpPost httpPost = new HttpPost(IMAGE_SERVER_HOST + IMAGE_INPUT_ENDPOINT);
		httpPost.addHeader("Content-Type", CONTENT_TYPE_JSON);
		StringEntity entity = new StringEntity(new Gson().toJson(imgMap));
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
		return responseBody;
	}
	
	public static HashMap<String, Object> getDeleteImagePathRequest(List<String> imgPathList) throws IOException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		HttpPost httpPost = new HttpPost(IMAGE_SERVER_HOST + IMAGE_DELETE_ENDPOINT);
		httpPost.addHeader("Content-Type", CONTENT_TYPE_JSON);
		StringEntity entity = new StringEntity(new Gson().toJson(imgPathList));
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
		httpClient.close();
		
		JsonObject root = convertResponseBodyToJsonObject(responseBody);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", root.get("code").getAsInt());
		resMap.put("msg", root.get("msg").getAsString());
		return resMap;
	}
	
	public static JsonObject convertResponseBodyToJsonObject(String responseBody) {
		JsonParser parser = new JsonParser();
		JsonObject root = new JsonObject();
		try {
			root = (JsonObject) parser.parse(responseBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return root;
	}

}
