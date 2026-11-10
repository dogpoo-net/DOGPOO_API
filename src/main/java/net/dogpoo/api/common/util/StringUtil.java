package net.dogpoo.api.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtil {

	private static final Logger log = LoggerFactory.getLogger(StringUtil.class);
	
	public static Integer toInteger(Object obj, boolean nnull){
		try {
			if(obj!=null && !"".equals(obj)) return Integer.valueOf((String)obj);
		} catch (Exception e) {
			log.debug(e.getMessage());
		}
		return nnull?0:null;
	}
	
	public static Double toDouble(Object obj, boolean nnull){
		try {
			if(obj!=null && !"".equals(obj)) return Double.valueOf((String)obj);
		} catch (Exception e) {
			log.debug(e.getMessage());
		}
		return nnull?0.0:null;
	}
	
	public static boolean isNumber(String str) {
		return str.matches("[+-]?\\d*(\\.\\d+)?");
	}
	
    public static int generateAuthNo() {
        java.util.Random generator = new java.util.Random();
        generator.setSeed(System.currentTimeMillis());
        return generator.nextInt(1000000) % 1000000;
    }
}
