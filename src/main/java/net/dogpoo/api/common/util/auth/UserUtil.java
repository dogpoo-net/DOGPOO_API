package net.dogpoo.api.common.util.auth;

import java.util.HashMap;
import java.util.regex.Pattern;

public final class UserUtil {
	
	// 특수문자 체크(없으면 0)
	public static boolean validCheckSpecialLetters(String str) {
		// 공백제거
		Pattern pt = Pattern.compile("[ !@#$%^&*(),.?\":{}|<>]");
		return pt.matcher(str).find();
	}
	
	public static boolean validateUuid(String userUuid) {
		Pattern UUID_REGEX = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
		return UUID_REGEX.matcher(userUuid).matches();
	}
	
	public static HashMap<String, Object> getExpiredTokenResultMap(String userKey) {
		HashMap<String, Object> keyResMap = new HashMap<String, Object>();
		keyResMap.put("code", 6);
		keyResMap.put("msg", "Expired session! Please log in again.");
		return keyResMap;
	}
}