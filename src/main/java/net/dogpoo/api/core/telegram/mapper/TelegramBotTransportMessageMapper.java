package net.dogpoo.api.core.telegram.mapper;

import java.util.HashMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TelegramBotTransportMessageMapper {

	@Select("SELECT accessKey, chatId FROM USER_TELEGRAM_BOT_TOKEN WHERE ccode = #{ccode} AND useYn LIMIT 1")
	public HashMap<String, String> getTelegramBotAccessToken(@Param("ccode") String ccode);
	
	@Select("SELECT accessKey, chatId FROM USER_TELEGRAM_BOT_TOKEN WHERE ccode = #{ccode} LIMIT 1")
	public HashMap<String, String> getTelegramBotAccessTokenForExternal(@Param("ccode") String ccode);
	
}