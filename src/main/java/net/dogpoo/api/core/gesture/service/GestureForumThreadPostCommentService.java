package net.dogpoo.api.core.gesture.service;

import java.util.HashMap;

import org.springframework.stereotype.Service;

import net.dogpoo.api.core.gesture.mapper.GestureForumThreadPostCommentMapper;

@Service
public class GestureForumThreadPostCommentService {

    public GestureForumThreadPostCommentService(GestureForumThreadPostCommentMapper mapper) {
        this.mapper = mapper;
    }

	private final GestureForumThreadPostCommentMapper mapper;
	
	public HashMap<String, Object> insertForumThreadPostComment(String userKey, HashMap<String, Object> paramMap) {
		paramMap.put("commentWriter", userKey);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		boolean isValidThreadNo = mapper.isValidThreadNo(paramMap);
		if(!isValidThreadNo) {
			resMsg = "KR".equals(countryCd) ? "유효하지 않은 게시물번호." : "ThreadNo is not valid.";
			
			resMap.put("code", 8);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		if(null == paramMap.get("comment")) {
			resMsg = "KR".equals(countryCd) ? "코멘트 내용이 없어요." : "There's no content.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		int len = paramMap.get("comment").toString().length();
		if(len > 1000) {
			resMsg = "KR".equals(countryCd) ? "최대 1000글자까지 쓸 수 있어요." : "Enter up to 1000 characters.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		int row = mapper.insertForumThreadPostComment(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "댓글 삽입 중 오류 발생." : "Error occurred inserting comment!";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	public HashMap<String, Object> updateForumThreadPostComment(String userKey, HashMap<String, Object> paramMap) {
		paramMap.put("commentWriter", userKey);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		if(null == paramMap.get("comment")) {
			resMsg = "KR".equals(countryCd) ? "코멘트 내용이 없어요." : "There's no content.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		int len = paramMap.get("comment").toString().length();
		if(len > 1000) {
			resMsg = "KR".equals(countryCd) ? "최대 1000글자까지 쓸 수 있어요." : "Enter up to 1000 characters.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		int row = mapper.updateForumThreadPostComment(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "댓글 수정 중 오류 발생." : "Error occurred updating comment!";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	public HashMap<String, Object> getForumThreadPostCommentList(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.getForumThreadPostCommentList(paramMap));
		return resMap;
	}
	
	public HashMap<String, Object> deleteForumThreadPostComment(String userKey, HashMap<String, Object> paramMap) {
		paramMap.put("commentWriter", userKey);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		int row = mapper.deleteForumThreadPostComment(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "삭제 실패." : "Failed to delete.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
}
