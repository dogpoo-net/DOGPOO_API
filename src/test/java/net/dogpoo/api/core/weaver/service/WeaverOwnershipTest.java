package net.dogpoo.api.core.weaver.service;
import java.util.*;import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;import net.dogpoo.api.core.weaver.model.*;
class WeaverOwnershipTest {
 WeaverWorkspaceMapper spaces=mock(WeaverWorkspaceMapper.class);WeaverSharingMapper members=mock(WeaverSharingMapper.class);WeaverSessionMapper accounts=mock(WeaverSessionMapper.class);WeaverSharingService service=new WeaverSharingService(spaces,members,accounts);
 final WeaverPolicyMapper plans = mock(WeaverPolicyMapper.class);

 @BeforeEach void configureTransferPolicy() {
  ReflectionTestUtils.setField(service, "policy", new WeaverWorkspacePolicyService(plans, spaces, members));
  when(plans.accountPlan("owner")).thenReturn(Map.of("code", "standard", "owned_limit", 10));
  when(plans.owned("owner")).thenReturn(List.of(Map.of("workspaceId", "space"), Map.of("workspaceId", "spare")));
  when(plans.accountPlan("recipient")).thenReturn(Map.of("code", "standard", "owned_limit", 10));
  when(plans.plan("space")).thenReturn(Map.of("code", "standard", "editor_limit", 2));
  when(plans.editors("space")).thenReturn(List.of("recipient"));
 }

 @Test void permissionsFollowOwnerColumnRatherThanWorkspaceId(){
  when(spaces.owner("original")).thenReturn("recipient");var a=mock(WeaverAccount.class);when(a.email()).thenReturn("r@example.invalid");when(accounts.account("recipient")).thenReturn(a);
  assertEquals("owner",service.access("recipient","original",true).role());
  assertThrows(ResponseStatusException.class,()->service.access("original","original",true));
 }
 @Test void transferReplacesRecipientMembershipWithFormerOwner(){
  when(spaces.owner("space")).thenReturn("owner");when(members.role("space","recipient")).thenReturn("editor");when(accounts.account("recipient")).thenReturn(mock(WeaverAccount.class));
  service.transfer("owner","space","recipient");verify(members).remove("space","recipient");verify(members).join("space","owner","editor");verify(spaces).transfer("space","recipient");
 }
 @Test void cannotTransferToNonmemberOrExceedEditorLimit() {
  when(spaces.owner("space")).thenReturn("owner");
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> service.transfer("owner", "space", "outsider")).getStatusCode().value());

  when(members.role("space", "recipient")).thenReturn("editor");
  when(plans.editors("space")).thenReturn(List.of("first-editor", "recipient"));
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> service.transfer("owner", "space", "recipient")).getStatusCode().value());
  verify(spaces, never()).transfer(anyString(), anyString());
 }

 @Test void withdrawalTransferDoesNotConsumeAnExtraEditorSeat(){
  when(spaces.owner("space")).thenReturn("owner");when(members.role("space","recipient")).thenReturn("editor");when(accounts.account("recipient")).thenReturn(mock(WeaverAccount.class));when(members.editors("space")).thenReturn(1);
  service.transferForWithdrawal("owner","space","recipient");verify(spaces).transfer("space","recipient");verify(members,never()).join(anyString(),anyString(),anyString());
 }
}
