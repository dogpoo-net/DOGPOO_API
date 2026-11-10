package net.dogpoo.api.core.telegram.controller;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import net.dogpoo.api.core.telegram.service.TelegramBotTransportMessageService;

@Controller
@RequestMapping("/msg/telegram")
public class TelegramBotBuildDataController {
	
	@Autowired TelegramBotTransportMessageService service;
	
	@RequestMapping(value = "/sendPersonalTelegramMessage", method=RequestMethod.POST)
	@ResponseBody
	public boolean sendPersonalTelegramMessage(HttpServletRequest req, @RequestBody String params) throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(params);
		String originKey = root.get("originKey").toString();
		if(!"6rCV7ISx7ZuI6rOg7LaUISEh".equals(originKey)) {
			return false;
		}
		String ccode = root.get("ccode").toString();
		String msg = root.get("msg").toString();
		return service.transportTelegramMsgMain(ccode, msg, false);
	}
}