package net.dogpoo.api.common.util;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpUtil {
	
//	private static final Logger logger = LoggerFactory.getLogger(IpUtil.class);
	
	public static String getClientIp(HttpServletRequest request) {
	    String ip = request.getHeader("X-Forwarded-For");
	    String userAddr = request.getRemoteAddr();
	    String userHost = request.getRemoteHost();
	    System.out.println("userAddr => " + userAddr);
	    System.out.println("userHost => " + userHost);

	    if (ip == null || ip.length() == 0 || "192.168.1.6".equals(ip) || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getHeader("Proxy-Client-IP");
	        System.out.println(ip);
	    } 
	    if (ip == null || ip.length() == 0 || "192.168.1.6".equals(ip) || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getHeader("WL-Proxy-Client-IP");
	        System.out.println(ip);
	    } 
	    if (ip == null || ip.length() == 0 || "192.168.1.6".equals(ip) || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getHeader("HTTP_CLIENT_IP");
	        System.out.println(ip);
	    } 
	    if (ip == null || ip.length() == 0 || "192.168.1.6".equals(ip) || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
	        System.out.println(ip);
	    }
	    if (ip == null || ip.length() == 0 || "192.168.1.6".equals(ip) || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getHeader("X-Real-IP");
	        System.out.println(ip);
	    }
	    if (ip == null || ip.length() == 0 || "192.168.1.6".equals(ip) || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getHeader("X-RealIP");
	        System.out.println(ip);
	    }
	    if (ip == null || ip.length() == 0 || "192.168.1.6".equals(ip) || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getHeader("REMOTE_ADDR");
	        System.out.println(ip);
	    }
	    if (ip == null || ip.length() == 0 || "192.168.1.6".equals(ip) || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getRemoteAddr();
	        System.out.println(ip);
	    }

	    if(ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1")) {
	        InetAddress address = null;
			try {
				address = InetAddress.getLocalHost();
				ip = address.getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
	    }
	    
	    System.out.println("REMOTE_IP => " + ip);

	    return ip;
	}
	
	public static String getCountryCode(String ip) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String host = "https://ip2c.org/";
		HttpGet httpGet = new HttpGet(host + ip);
		
		CloseableHttpResponse response = null;
		String responseBody = "";
		try {
			response = httpClient.execute(httpGet);
			responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String countryCd = "";
		if(responseBody.length() > 0) {
			String[] splitBody = responseBody.split(";");
			String flag = splitBody[0];
			if("1".equals(flag)) {
				countryCd = splitBody[1];
			}
		}
		return countryCd;
	}
}