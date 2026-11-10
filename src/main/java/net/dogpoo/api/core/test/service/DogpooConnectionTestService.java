package net.dogpoo.api.core.test.service;

import java.util.HashMap;

import org.springframework.stereotype.Service;

@Service
public class DogpooConnectionTestService {
	
	public String getPostGestureBody(HashMap<String, Object> paramMap) {
		StringBuffer sb = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		if(!paramMap.containsKey("uid")) {
			sb.append("uid가 없습니다.\n");
		} else {
			valueBuffer.append("uid => ").append(paramMap.get("uid")).append("\n");
		}
		if(!paramMap.containsKey("country")) {
			sb.append("국가코드가 없습니다.\n");
		} else {
			valueBuffer.append("country => ").append(paramMap.get("country")).append("\n");
		}
		if(!paramMap.containsKey("isUse")) {
			sb.append("사용여부 값이 없습니다.\n");
		} else {
			valueBuffer.append("isUse => ").append(paramMap.get("isUse")).append("\n");
		}
		
		return sb.append("\n").append(valueBuffer).toString();
	}
}