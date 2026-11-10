package net.dogpoo.api.core.convatar.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.convatar.service.ConvatarCurrencyService;

@RestController
@RequestMapping("/convatar/api/currency")
public class ConvatarCurrencyController {
	
	@Autowired ConvatarCurrencyService service;
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	
	@RequestMapping(value = "/getCountryList", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getCountryList() {
		return service.getCountryList();
	}
	
	@RequestMapping(value = "/getEachCountryCurrencyVolume", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getEachCountryCurrencyVolume(@RequestBody HashMap<String, Object> paramMap) {
		return service.getEachCountryCurrencyVolume(paramMap);
	}
}