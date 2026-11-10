package net.dogpoo.api.core.gesture.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import net.dogpoo.api.core.gesture.mapper.GestureForumThreadPostMapper;
import net.dogpoo.api.core.gesture.service.util.GestureForumThreadImageContentUtil;
import net.dogpoo.api.core.telegram.service.TelegramBotTransportMessageService;

@Service
public class GestureForumThreadPostService {

    public GestureForumThreadPostService(
            GestureForumThreadPostMapper mapper,
            TelegramBotTransportMessageService telegram) {
        this.mapper = mapper;
        this.telegram = telegram;
    }


	final static String IMAGE_SPLIT_DELIMITER = "<img src=\"http://image.dongnyang.com";
	final static String CONTENT_TYPE_JSON = "application/json;charset=utf-8";
	final static byte THREAD_PAGE_SIZE = 20;
	
	private final GestureForumThreadPostMapper mapper;
	private final TelegramBotTransportMessageService telegram;
	
	/**
	 * 스레드 리스트 조회
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> getForumThreadPostWholeList(HashMap<String, Object> paramMap) {
		String pageNumStr = paramMap.get("pageNum").toString();
		int pageNum = Integer.parseInt(pageNumStr);
		paramMap.put("pageTarget", pageNum * THREAD_PAGE_SIZE);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("CNT", mapper.getGestureForumThreadWholeListCount(paramMap));
		resMap.put("DATA", mapper.getGestureForumThreadWholeList(paramMap));
		return resMap;
	}
	
	/**
	 * 스레드 및 포스트 검색
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> getGestureForumThreadListSearch(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		if(null == paramMap.get("searchKeyword") || paramMap.get("searchKeyword").toString().length() < 2) {
			resMsg = "KR".equals(countryCd) ? "키워드가 없거나 너무 짧습니다." : "Search keyword is too short.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		String pageNumStr = paramMap.get("pageNum").toString();
		int pageNum = Integer.parseInt(pageNumStr);
		paramMap.put("pageTarget", pageNum * THREAD_PAGE_SIZE);
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("CNT", mapper.getGestureForumThreadListSearchCount(paramMap));
		resMap.put("DATA", mapper.getGestureForumThreadListSearch(paramMap));
		return resMap;
	}
	
	/**
	 * 스레드 단건 조회
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> getForumThreadHeadData(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.getForumThreadHeadData(paramMap));
		return resMap;
	}
	
	/**
	 * 스레드 조회수 카운팅
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> updateForumThreadCountingViewNum(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.updateForumThreadCountingViewNum(paramMap));
		return resMap;
	}
	
	/**
	 * 스레드 포스트 조회
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> getForumThreadPostContent(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.getGestureForumThreadPost(paramMap));
		return resMap;
	}
	
	/**
	 * 스레드 카테고리 리스트 조회
	 * @return
	 */
	public HashMap<String, Object> getForumThreadCategoryList(String userKey) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.getForumThreadCategoryList(userKey));
		return resMap;
	}
	
	/**
	 * 스레드 입력
	 * @param paramMap
	 * @return
	 * @throws IOException 
	 */
	public HashMap<String, Object> insertForumThread(String userKey, HashMap<String, Object> paramMap) throws IOException {
		paramMap.put("threadOwner", userKey);
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		HashMap<String, Object> resMap = getForumThreadInsertValidatedMap("HEAD", paramMap);
		if((int) resMap.get("code") > 0) {
			return resMap;
		}
		
		HashMap<String, Object> imageResMap = GestureForumThreadImageContentUtil.getInputImagePathDeltaType(paramMap.get("threadContent"));
		if((int) imageResMap.get("code") > 0) {
			return imageResMap;
		}
		
		paramMap.put("threadContent", imageResMap.get("content").toString());
		
		int threadRow = mapper.insertForumThread(paramMap);
		if(threadRow == 0) {
			resMsg = "KR".equals(countryCd) ? "게시물 삽입 중 오류 발생." : "Error occurred inserting head thread!";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		int postRow = mapper.insertForumThreadHeadPost(paramMap);
		if(postRow == 0) {
			mapper.deleteForumThread(paramMap);
			resMsg = "KR".equals(countryCd) ? "포스트 삽입 중 오류 발생." : "Error occurred inserting post thread!";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		String telegramMsg = "제스쳐 포럼 알림!\n새로운 스레드가 등록되었습니다.\n\n게시물 제목 ==> \n" + paramMap.get("threadTitle").toString();
		telegram.transportTelegramMsgMain("1000000002", telegramMsg, false);
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	/**
	 * 포스트 입력
	 * @param paramMap
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, Object> insertForumThreadPost(String userKey, HashMap<String, Object> paramMap) throws IOException {
		paramMap.put("postWriter", userKey);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		HashMap<String, Object> imageResMap = GestureForumThreadImageContentUtil.getInputImagePathDeltaType(paramMap.get("postContent"));
		if((int) imageResMap.get("code") > 0) {
			return imageResMap;
		}
		
		paramMap.put("postContent", imageResMap.get("content").toString());
		
		int row = mapper.insertForumThreadPost(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "게시물 삽입 중 오류 발생." : "Error occurred inserting head thread!";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	/**
	 * 스레드 삭제
	 * @param paramMap
	 * @return
	 * @throws IOException 
	 */
	public HashMap<String, Object> removeForumThreadWholeData(String userKey, HashMap<String, Object> paramMap) throws IOException {
		paramMap.put("threadOwner", userKey);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		List<String> contents = mapper.getForumThreadHeadPostContentOnly(paramMap);
		for(String content : contents) {
			List<String> imgPathList = GestureForumThreadImageContentUtil.getImagePathList(IMAGE_SPLIT_DELIMITER, content);
			HashMap<String, Object> imgDelResMap = GestureForumThreadImageContentUtil.getDeleteImagePathRequest(imgPathList);
//			int code = (int) imgDelResMap.get("code");
//			String msg = String.valueOf(imgDelResMap.get("msg"));
//			if(code > 1) {
//				resMap.put("code", code);
//				resMap.put("msg", msg);
//				return resMap;
//			}
		}
		
		
		int row = mapper.removeForumThreadWholeData(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "삭제할 게시물이 존재하지 않습니다." : "Thread does not exist to delete.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	/**
	 * 스레드를 수정하는 화면에 뿌려줄 스레드 내용을 불러옴
	 * @return
	 */
	public HashMap<String, Object> getForumHeadThreadContent(HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", mapper.getForumHeadThreadContent(paramMap));
		return resMap;
	}
	
	/**
	 * 스레드 수정
	 * @param paramMap 
	 * @throws IOException 
	 */
	public HashMap<String, Object> updateForumThread(String userKey, HashMap<String, Object> paramMap) throws IOException {
		paramMap.put("threadOwner", userKey);
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		HashMap<String, Object> resMap = getForumThreadInsertValidatedMap("HEAD", paramMap);
		if((int) resMap.get("code") > 0) {
			return resMap;
		}
		
		HashMap<String, Object> imageResMap = GestureForumThreadImageContentUtil.getInputImagePathHtmlType(String.valueOf(paramMap.get("threadContent")));
		if((int) imageResMap.get("code") > 0) {
			return imageResMap;
		}
		
		String oldContent = mapper.getForumThreadPostContentOnly(paramMap);
		String newContent = imageResMap.get("content").toString();
		resMap = GestureForumThreadImageContentUtil.updateContentImageFilter(paramMap, oldContent, newContent);
		if((int) resMap.get("code") > 1) {
			return resMap;
		}
		
		paramMap.put("threadContent", newContent);
		int row = mapper.updateForumThread(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "게시물 삽입 중 오류 발생." : "Error occurred updating head thread!";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	/**
	 * 포스트 수정
	 * @param userKey 
	 * @param paramMap
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, Object> updateForumThreadPost(String userKey, HashMap<String, Object> paramMap) throws IOException {
		paramMap.put("postWriter", userKey);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		HashMap<String, Object> imageResMap = GestureForumThreadImageContentUtil.getInputImagePathHtmlType(String.valueOf(paramMap.get("postContent")));
		if((int) imageResMap.get("code") > 0) {
			return imageResMap;
		}
		
		String oldContent = mapper.getForumThreadPostContentOnly(paramMap);
		String newContent = imageResMap.get("content").toString();
		resMap = GestureForumThreadImageContentUtil.updateContentImageFilter(paramMap, oldContent, newContent);
		if((int) resMap.get("code") > 1) {
			return resMap;
		}
		
		paramMap.put("postContent", newContent);
		int row = mapper.updateForumThreadPost(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "게시물 수정 실패." : "Edit failed.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	/**
	 * 포스트 삭제
	 * @param userKey 
	 * @param paramMap
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, Object> deleteForumThreadPost(String userKey, HashMap<String, Object> paramMap) throws IOException {
		paramMap.put("ccode", userKey);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		String content = mapper.getForumThreadPostContentOnly(paramMap);
		List<String> imgPathList = GestureForumThreadImageContentUtil.getImagePathList(IMAGE_SPLIT_DELIMITER, content);
		
		HashMap<String, Object> imgDelResMap = GestureForumThreadImageContentUtil.getDeleteImagePathRequest(imgPathList);
		int code = (int) imgDelResMap.get("code");
		String msg = String.valueOf(imgDelResMap.get("msg"));
		if(code > 1) {
			resMap.put("code", code);
			resMap.put("msg", msg);
			return resMap;
		}
		
		int row = mapper.deleteForumThreadPost(paramMap);
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "삭제할 포스트가 존재하지 않습니다." : "Post does not exist to delete.";
			
			resMap.put("code", 1);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	/**
	 * 포스트 좋아요 클릭
	 * @param userKey 
	 * @return 
	 */
	public HashMap<String, Object> insertForumThreadPostLike(String userKey, HashMap<String, Object> paramMap) {
		paramMap.put("ccode", userKey);
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		boolean isExist = mapper.getFourmThreadPostLikeDuplicateCheck(paramMap);
		int reqIsLike = (int) paramMap.get("isPostLike");
		
		if(isExist) {
			int isLike = mapper.isPostLike(paramMap);
			mapper.deleteForumThreadPostLike(paramMap);
			
			String thisTarget = (int) isLike == 1 ? "likeCnt" : "dislikeCnt";
			paramMap.put("target", thisTarget);
			String targetCnt = "- 1";
			paramMap.put("cnt", targetCnt);
			int updateRow = mapper.updateForumThreadPostLikeCnt(paramMap);
			if(updateRow > 0) {
				resMap.put("code", 0);
				resMap.put("msg", "OK");
			} else {
				resMap.put("code", 9);
				resMap.put("msg", "ERROR OCCURRED!");
			}
			if(isLike == reqIsLike) {
				return resMap;
			}
		}
		
		String reqTarget = reqIsLike == 1 ? "likeCnt" : "dislikeCnt";
		paramMap.put("target", reqTarget);
		
		int row = mapper.insertForumThreadPostLike(paramMap);
		if(row == 0) {
			resMap.put("code", 9);
			resMap.put("msg", "Value is NULL!");
			return resMap;
		}
		
		String targetCnt = "+ 1";
		paramMap.put("cnt", targetCnt);
		int updateRow = mapper.updateForumThreadPostLikeCnt(paramMap);
		if(updateRow == 0) {
			resMap.put("code", 9);
			resMap.put("msg", "Value is NULL!");
			return resMap;
		}
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		return resMap;
	}
	
	/**
	 * 스레드 좋아요 클릭
	 * @param paramMap
	 * @return
	 */
	public HashMap<String, Object> toggleForumThreadLike(String userKey, HashMap<String, Object> paramMap) {
		paramMap.put("ccode", userKey);
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		boolean isAlreadyLike = mapper.isAlreadyForumThreadLike(paramMap);
		int row = 0;
		if(isAlreadyLike) {
			row = mapper.deleteForumThreadLike(paramMap);
			paramMap.put("cnt", "- 1");
			mapper.updateForumThreadLikeCnt(paramMap);
		} else {
			row = mapper.insertForumThreadLike(paramMap);
			paramMap.put("cnt", "+ 1");
			mapper.updateForumThreadLikeCnt(paramMap);
		}
		
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		if(row == 0) {
			resMsg = "KR".equals(countryCd) ? "데이터를 입력하거나 삭제할 수 없습니다." : "Data could not be entered or deleted.";
			
			resMap.put("code", 8);
			resMap.put("msg", resMsg);
			return resMap;
		}
		
		resMap.put("code", 0);
		resMap.put("msg", "OK");
		resMap.put("DATA", isAlreadyLike);
		return resMap;
	}
	
	public void testtest(String htmlContent) throws IOException {
		GestureForumThreadImageContentUtil.getInputImagePathHtmlType(htmlContent);
	}
	
	private HashMap<String, Object> getForumThreadInsertValidatedMap(String target, HashMap<String, Object> paramMap) {
		HashMap<String, Object> resMap = new HashMap<String, Object>();
		resMap.put("code", 0);
		String countryCd = paramMap.get("countryCd").toString();
		String resMsg = "";
		
		for(HashMap.Entry<String, Object> entry : paramMap.entrySet()) {
			if(null == entry.getValue()) {
				resMsg = "KR".equals(countryCd) ? "입력되지 않은 값이 있습니다." : "Value is NULL!";
				
				resMap.put("code", 8);
				resMap.put("msg", resMsg);
				return resMap;
			}
		}
		
		if("HEAD".equals(target)) {
			String titleLiteral = paramMap.get("threadTitle").toString().replace(" ", "").replace("\t", "").replace("\n", "");
			System.out.println("."+titleLiteral);
			if(titleLiteral.length() == 0) {
				resMsg = "KR".equals(countryCd) ? "제목을 입력해주세요." : "You have to type everything!";
				
				resMap.put("code", 2);
				resMap.put("msg", resMsg);
			}
			if(paramMap.get("threadTitle").toString().length() > 200) {
				resMsg = "KR".equals(countryCd) ? "제목은 200자 이내로 써주세요." : "Title is too long! (Up to 200 characters)";
				
				resMap.put("code", 2);
				resMap.put("msg", resMsg);
			} else if(null == paramMap.get("threadCateNo")) {
				resMsg = "KR".equals(countryCd) ? "카테고리가 입력되지 않았습니다." : "Category value is empty!";
				
				resMap.put("code", 3);
				resMap.put("msg", resMsg);
			}
		} else if("POST".equals(target)) {
			if(paramMap.get("postContent").toString().length() > 20000) {
				resMsg = "KR".equals(countryCd) ? "내용은 20000자 이내로 입력하여 주십시오." : "Content is too long! (Up to 20000 characters)";
				
				resMap.put("code", 2);
				resMap.put("msg", resMsg);
			}
		}
		return resMap;
	}
}