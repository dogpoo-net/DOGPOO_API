package net.dogpoo.api.core.weaver.service;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;
class WeaverWorkspaceManagementTest {
 WeaverWorkspaceMapper spaces=mock(WeaverWorkspaceMapper.class);
 WeaverSharingMapper members=mock(WeaverSharingMapper.class);
 WeaverSessionMapper accounts=mock(WeaverSessionMapper.class);
 WeaverWithdrawalMapper deletion=mock(WeaverWithdrawalMapper.class);
 WeaverSharingService service=new WeaverSharingService(spaces,members,accounts);
 @BeforeEach void setup(){when(spaces.revision("w")).thenReturn(1L);when(spaces.owner("w")).thenReturn("owner");}
 @Test void ownerCanCreateAndRenameWithValidatedName(){
  var result=service.createWorkspace("owner"," 새 공간 ");String id=(String)result.get("workspaceId");
  verify(spaces).create(id,"owner");verify(spaces).initializeName(id,"새 공간");
  service.renameWorkspace("owner","w","새 이름");verify(spaces).rename("w","새 이름");
  assertThrows(ResponseStatusException.class,()->service.createWorkspace("owner"," "));
  assertThrows(ResponseStatusException.class,()->service.renameWorkspace("editor","w","bad"));
 }
 @Test void leavingOnlyRemovesMembershipAndKeepsAccountAndAuthorship(){
  when(members.role("w","editor")).thenReturn("editor");service.leaveWorkspace("editor","w");
  verify(members).remove("w","editor");verifyNoInteractions(accounts,deletion);
  verify(spaces,never()).deleteWorkspace(anyString());
  assertThrows(ResponseStatusException.class,()->service.leaveWorkspace("owner","w"));
  assertThrows(ResponseStatusException.class,()->service.leaveWorkspace("stranger","w"));
 }
 @Test void unknownEmailCannotCreateInvite(){
  when(accounts.account("owner")).thenReturn(new net.dogpoo.api.core.weaver.model.WeaverAccount("owner","owner@example.com",null,null,"Owner"));
  assertThrows(ResponseStatusException.class,()->service.invite("owner","w","missing@example.com","viewer"));
  verify(members,never()).create(anyString(),anyString(),anyString(),anyString(),anyString());
 }
}
