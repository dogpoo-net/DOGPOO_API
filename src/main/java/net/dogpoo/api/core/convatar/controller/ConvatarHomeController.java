package net.dogpoo.api.core.convatar.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.convatar.service.ConvatarHomeService;

@RestController
@RequestMapping("/convatar/api/home")
public class ConvatarHomeController {
	
	@Autowired ConvatarHomeService service;
	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	
	@RequestMapping(value = "/getConvatarReleaseNotesList", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getConvatarReleaseNotesList() {
		return service.getConvatarReleaseNotesList();
	}
	
	@RequestMapping(value = "/getNoticeModelList", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getNoticeModelList() {
		return service.getNoticeModelList();
	}
}
