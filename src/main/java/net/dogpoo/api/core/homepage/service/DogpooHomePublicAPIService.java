package net.dogpoo.api.core.homepage.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dogpoo.api.common.util.MailSendUtil;
import net.dogpoo.api.common.util.StringUtil;
import net.dogpoo.api.core.homepage.mapper.DogpooHomePublicAPIMapper;
import net.dogpoo.api.core.homepage.model.DogpooHomeContactMessageModel;

@Service
public class DogpooHomePublicAPIService {
	
	@Autowired DogpooHomePublicAPIMapper mapper;

	public HashMap<String, Object> insertContactMessage(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = getValidateContactMap(paramMap);
		String contactType = (String) paramMap.get("contactType");
		String contacter = (String) paramMap.get("contacter");
		String contacterEmail = (String) paramMap.get("contacterEmail");
		String contacterPhone = (String) paramMap.get("contacterPhone");
		String contacterUrl = (String) paramMap.get("contacterUrl");
		String contactMessage = (String) paramMap.get("contactMessage");
		
		DogpooHomeContactMessageModel model = new DogpooHomeContactMessageModel();
		model.setContactType(contactType);
		model.setContacter(contacter);
		model.setContacterEmail(contacterEmail);
		model.setContacterPhone(contacterPhone);
		model.setContacterUrl(contacterUrl);
		model.setContactMessage(contactMessage);
		int insertCnt = mapper.insertContactMessage(model);
		
		if(insertCnt == 0) {
			resMap.put("code", 2);
			resMap.put("msg", "입력하지 못했어요. 확인 후 다시 시도해주세요.");
		} else {
			resMap.put("code", 0);
			resMap.put("msg", "OK");
		}
		
		List<String> toList = new ArrayList<String>();
		toList.add("dogpoo.net@gmail.com");
		String subject = "[DOGPOO_HOME] 홈페이지에서 작성된 새로운 문의 메시지가 있습니다.";
		String body = "문의 유형: " + contactType + "\n이름: " + contacter + "\n이메일: " + contacterEmail + "\n연락처: " + contacterPhone + "\nURL: " + contacterUrl + "\n\n문의내용: \n" + contactMessage;
		MailSendUtil.mailSender(toList, subject, body);
		
		return resMap;
	}
	
	private HashMap<String, Object> getValidateContactMap(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 1);
		
		String contactType = (String) paramMap.get("contactType");
		if(null == contactType || "".equals(contactType)) {
			resMap.put("msg", "문의 유형을 선택해 주세요.");
			return resMap;
		} else if(!"비즈니스 문의".equals(contactType) && !"광고 문의".equals(contactType) && !"기타 문의".equals(contactType)) {
			resMap.put("msg", "잘못된 문의 유형입니다.");
			return resMap;
		}
		
		String contacter = (String) paramMap.get("contacter");
		if(null == contacter || "".equals(contacter)) {
			resMap.put("msg", "이름을 입력해 주세요.");
			return resMap;
		} else if(contacter.length() > 20) {
			resMap.put("msg", "이름이 너무 길어요. 20자 이내로 입력해주세요.");
			return resMap;
		}
		
		String contacterEmail = (String) paramMap.get("contacterEmail");
		if(null == contacterEmail || "".equals(contacterEmail)) {
			resMap.put("msg", "이메일을 입력해 주세요.");
			return resMap;
		} else if(contacterEmail.length() > 50) {
			resMap.put("msg", "이메일 주소가 너무 길어요. 50자 이내로 작성해주세요.");
			return resMap;
		} else if(contacterEmail.length() < 6) {
			resMap.put("msg", "이메일 주소가 너무 짧아요.");
			return resMap;
		} else if(!contacterEmail.contains("@")) {
			resMap.put("msg", "이메일 형식이 아닌것 같아요. 다시 입력해주세요.");
			return resMap;
		} else if(contacterEmail.contains("@")) {
			String[] emailArr = contacterEmail.split("@");
			if(null == emailArr[0] || "".equals(emailArr[0]) || null == emailArr[1] || "".equals(emailArr[1])) {
				resMap.put("msg", "이메일 형식이 아닌것 같아요. 다시 입력해주세요.");
				return resMap;
			}
		}
		
		String contacterPhone = (String) paramMap.get("contacterPhone");
		if(null == contacterPhone || "".equals(contacterPhone)) {
			resMap.put("msg", "연락처를 입력해 주세요.");
			return resMap;
		} else if(!StringUtil.isNumber(contacterPhone)) {
			resMap.put("msg", "연락처는 숫자만 입력해 주세요.");
			return resMap;
		} else if(contacterPhone.length() < 8) {
			resMap.put("msg", "연락처가 너무 짧아요.");
			return resMap;
		} else if(contacterPhone.length() > 20) {
			resMap.put("msg", "연락처가 너무 길어요. 20자 이내로 작성해주세요.");
			return resMap;
		}
		
		String contacterUrl = (String) paramMap.get("contacterUrl");
		if(null != contacterUrl && !"".equals(contacterUrl)) {
			if(contacterUrl.length() > 255) {
				resMap.put("msg", "URL이 너무 길어요.");
				return resMap;
			}
		}
		
		String contactMessage = (String) paramMap.get("contactMessage");
		if(null != contactMessage && !"".equals(contactMessage)) {
			if(contactMessage.length() > 1000) {
				resMap.put("msg", "문의 내용이 너무 길어요.");
				return resMap;
			} else if(contactMessage.length() < 10) {
				resMap.put("msg", "문의 내용은 최소 10자 이상 입력해주세요.");
				return resMap;
			}
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
}
