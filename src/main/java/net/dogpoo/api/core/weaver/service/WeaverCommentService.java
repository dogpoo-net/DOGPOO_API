package net.dogpoo.api.core.weaver.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
@Service public class WeaverCommentService {
 private final WeaverCommentMapper comments;private final WeaverSharingService sharing;private final WeaverSessionMapper accounts;
 public WeaverCommentService(WeaverCommentMapper comments,WeaverSharingService sharing,WeaverSessionMapper accounts){this.comments=comments;this.sharing=sharing;this.accounts=accounts;}
 @Transactional public Map<String,Object> handle(String actor,WeaverCommentRequest r,boolean write){
  var target=sharing.access(actor,r.workspaceId,write);
  if(r.memoId==null || r.memoId.isBlank() || r.memoId.length()>512 || r.beforeId<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"메모 정보가 올바르지 않습니다.");
  if(comments.exists(target.id(),r.memoId)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"서버에 저장된 메모가 없습니다. 메모를 저장한 뒤 다시 시도해주세요.");
  if(write){
   String content=r.content==null?"":r.content.strip();
   if(content.isEmpty() || content.codePointCount(0,content.length())>4000 || r.requestId==null || !r.requestId.matches("[a-fA-F0-9-]{36}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"댓글은 1~4000자로 입력해주세요.");
   var user=accounts.account(actor);if(user==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);WeaverAuthService.name(user.displayName());
   comments.add(target.id(),r.memoId,actor,r.requestId,content);
   var stored=comments.submitted(actor,r.requestId);
   if(!target.id().equals(stored.get("workspace")) || !r.memoId.equals(stored.get("memo")) || !content.equals(stored.get("content")))throw new ResponseStatusException(HttpStatus.CONFLICT,"댓글 요청이 변경되었습니다. 다시 시도해주세요.");
  }
  return Map.of("comments",comments.list(target.id(),r.memoId,write?Long.MAX_VALUE:r.beforeId));
 }
}
