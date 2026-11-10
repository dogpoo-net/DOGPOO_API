package net.dogpoo.api.core.homepage.controller;

import java.io.IOException;
import java.util.HashMap;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dogpoo.api.core.homepage.service.DogpooHomeBusinessDataService;
import net.dogpoo.api.core.homepage.service.DogpooHomePublicAPIService;

@RestController
@RequestMapping("/api/homepage")
public class DogpooHomePublicAPIController {

    public DogpooHomePublicAPIController(
            DogpooHomePublicAPIService service,
            DogpooHomeBusinessDataService business) {
        this.service = service;
        this.business = business;
    }

	
	private static final String JSON_UTF8 = "application/json;charset=UTF-8";
	private final DogpooHomePublicAPIService service;
	private final DogpooHomeBusinessDataService business;
	
	@RequestMapping(value = "/contact/insertContactMessage", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> insertContactMessage(@RequestBody HashMap<String, Object> paramMap) {
		return service.insertContactMessage(paramMap);
	}
	
	@RequestMapping(value = "/business/getCrewMembers", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getCrewMembers() {
		return business.getCrewMembers();
	}
	
	@RequestMapping(value = "/business/getOurWorks", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getOurWorks() {
		return business.getOurWorks();
	}
	
	@RequestMapping(value = "/business/getBizData", method = RequestMethod.POST, produces = JSON_UTF8)
	public HashMap<String, Object> getBizData() {
		return business.getBizData();
	}
	
	@RequestMapping(value = "/business/getDonationLink", method = RequestMethod.GET)
	public void getDonationLink(HttpServletResponse response) throws IOException {
		response.sendRedirect("https://ko-fi.com/dogpoo");
	}
}