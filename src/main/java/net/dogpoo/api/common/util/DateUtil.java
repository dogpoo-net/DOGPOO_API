package net.dogpoo.api.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateUtil {

	private static final Logger log = LoggerFactory.getLogger(DateUtil.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	public static boolean isAfterDate(String originDate, String compareDate) {
		Date day1 = parse(originDate);
		Date day2 = parse(compareDate);
		return day1.after(day2);
	}
	
	public static boolean isBeforeDate(String originDate, String compareDate) {
		Date day1 = parse(originDate);
		Date day2 = parse(compareDate);
		return day1.before(day2);
	}
	
	public static int compareDate(String originDate, String compareDate) {
		Date day1 = parse(originDate);
		Date day2 = parse(compareDate);
		return day1.compareTo(day2);
	}
	
	public static Date parse(String yyyyMMdd) {
		try {
			if (yyyyMMdd != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				return sdf.parse(yyyyMMdd);
			}
		} catch (ParseException e) {
			log.debug(e.getMessage());
		}
		
		return null;
	}
	
	public static Date parse1(String yyyyMMddHHmm) {
		try {
			if (yyyyMMddHHmm != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				return sdf.parse(yyyyMMddHHmm);
			}
		} catch (ParseException e) {
			log.debug(e.getMessage());
		}
		
		return null;
	}
	
	public static Date parse2(String yyyyMMdd) {
		try {
			if (yyyyMMdd != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				return sdf.parse(yyyyMMdd);
			}
		} catch (ParseException e) {
			log.debug(e.getMessage());
		}
		
		return null;
	}
	
	public static Date parse3(String yyyyM) {
		try {
			if (yyyyM != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M");
				return sdf.parse(yyyyM);
			}
		} catch (ParseException e) {
			log.debug(e.getMessage());
		}
		
		return null;
	}
	
	public static String format1(Date yyyyMMddHHmm) {
		if (yyyyMMddHHmm != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			return sdf.format(yyyyMMddHHmm);
		}
		
		return null;
	}
	
	public static String format2(Date yyyyMMdd) {
		if (yyyyMMdd != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			return sdf.format(yyyyMMdd);
		}
		
		return null;
	}
	
	public static String format3(Date yyyyMMdd) {
		if (yyyyMMdd != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			return sdf.format(yyyyMMdd);
		}
		
		return null;
	}
	
	public static String getNow() {
        SimpleDateFormat f = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
        Date dt = new Date();
        String t = f.format(dt);
        return t;
	}
	
	public static String getCurrentDate() {
		SimpleDateFormat f = new SimpleDateFormat ("yyyy-MM-dd");
		Date dt = new Date();
		String t = f.format(dt);
		return t;
	}
	
	public static String getYesterDate() {
		Calendar c = new GregorianCalendar();
		c.add(Calendar.DATE, -1);
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
		String d = f.format(c.getTime());
		return d;
	}
	
	public static String getGmtToDateTime(String gmt) {
		String[] dateArr = gmt.split("T");
		String Date = dateArr[0];
		String rawTime = dateArr[1];
		String time = rawTime.substring(0, 8);
		StringBuffer dateTimeBuffer = new StringBuffer(Date).append(" ").append(time);
		return dateTimeBuffer.toString();
	}
	
	public static String addHour(String originDate, int h) {
		String result = LocalDateTime.parse(originDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
				.plusHours(h)
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		return result;
	}
	
	public static boolean isWithin4MinutesBefore(String baseTime, String targetTime) {
		LocalDateTime base = LocalDateTime.parse(baseTime, FORMATTER);
		LocalDateTime target = LocalDateTime.parse(targetTime, FORMATTER);
		LocalDateTime threeMinutesBefore = base.minusMinutes(4);
		return !target.isBefore(threeMinutesBefore) && !target.isAfter(base);
	}
	
	public static String minusSeconds(String dateTimeStr, int seconds) {
		LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, FORMATTER);
		return dateTime.minusSeconds(seconds).format(FORMATTER);
	}
}