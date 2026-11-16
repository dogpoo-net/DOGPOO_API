package net.dogpoo.api.core.weaver.service;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
import net.dogpoo.api.core.weaver.model.*;
class WeaverCommentServiceTest {
 WeaverCommentMapper mapper=mock(WeaverCommentMapper.class);WeaverSharingService sharing=mock(WeaverSharingService.class);WeaverSessionMapper users=mock(WeaverSessionMapper.class);
 WeaverCommentService service=new WeaverCommentService(mapper,sharing,users);WeaverCommentRequest r=new WeaverCommentRequest();
 @BeforeEach void setup(){r.workspaceId="w";r.memoId="m";r.content="댓글\n두 번째 줄";r.requestId=UUID.randomUUID().toString();when(sharing.access(eq("u"),eq("w"),anyBoolean())).thenReturn(new WeaverSharingService.Access("w","owner@example.com","editor"));when(mapper.exists("w","m")).thenReturn(1);when(users.account("u")).thenReturn(new WeaverAccount("u","e@example.com","hash",LocalDateTime.now(),"작성자"));when(mapper.submitted("u",r.requestId)).thenAnswer(i->Map.of("workspace","w","memo","m","content",r.content));}
 @Test void writePreservesNewlinesAndReadsLatest(){service.handle("u",r,true);verify(sharing).access("u","w",true);verify(mapper).add("w","m","u",r.requestId,r.content);verify(mapper).list("w","m",Long.MAX_VALUE);}
 @Test void readUsesPaginationAndMembership(){r.beforeId=20;service.handle("u",r,false);verify(sharing).access("u","w",false);verify(mapper).list("w","m",20);verify(mapper,never()).add(any(),any(),any(),any(),any());}
 @Test void forbiddenWorkspaceCannotReadOrWrite(){when(sharing.access(eq("u"),eq("w"),anyBoolean())).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));assertThrows(ResponseStatusException.class,()->service.handle("u",r,false));assertThrows(ResponseStatusException.class,()->service.handle("u",r,true));verifyNoInteractions(mapper);}
 @Test void deletedMemoRejectsComments(){when(mapper.exists("w","m")).thenReturn(0);assertThrows(ResponseStatusException.class,()->service.handle("u",r,true));verify(mapper,never()).add(any(),any(),any(),any(),any());}
 @Test void blankAndOversizedTextRejected(){r.content="  ";assertThrows(ResponseStatusException.class,()->service.handle("u",r,true));r.content="가".repeat(4001);assertThrows(ResponseStatusException.class,()->service.handle("u",r,true));}
}
