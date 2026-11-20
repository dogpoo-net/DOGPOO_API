package net.dogpoo.api.core.weaver.service;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;import net.dogpoo.api.core.weaver.model.WeaverAccount;
class WeaverWorkspaceLifecycleTest {
 WeaverWorkspaceMapper spaces=mock(WeaverWorkspaceMapper.class);WeaverWorkspaceLifecycleMapper store=mock(WeaverWorkspaceLifecycleMapper.class);
 WeaverSessionMapper accounts=mock(WeaverSessionMapper.class);WeaverWithdrawalMapper deletion=mock(WeaverWithdrawalMapper.class);WeaverVerificationMail mail=mock(WeaverVerificationMail.class);
 WeaverWorkspaceLifecycleService service=new WeaverWorkspaceLifecycleService(spaces,store,accounts,deletion,mail);
 @BeforeEach void setup() {
  final var plans = mock(WeaverPolicyMapper.class);
  final var members = mock(WeaverSharingMapper.class);
  ReflectionTestUtils.setField(service, "policy", new WeaverWorkspacePolicyService(plans, spaces, members));
  when(plans.accountPlan("owner")).thenReturn(Map.of("code", "standard", "owned_limit", 10));
  when(plans.owned("owner")).thenReturn(List.of(Map.of("workspaceId", "w"), Map.of("workspaceId", "spare")));
  when(plans.plan("w")).thenReturn(Map.of("code", "standard"));
  when(spaces.revision("w")).thenReturn(1L);
  when(spaces.owner("w")).thenReturn("owner");
  final var account = mock(WeaverAccount.class);
  when(account.email()).thenReturn("owner@example.invalid");
  when(accounts.account("owner")).thenReturn(account);
 }
 @Test void unusedSpaceDeletesImmediatelyWithoutMailOrRetention(){
  var preview=service.preview("owner","w");assertEquals(false,preview.get("requiresEmail"));assertEquals(0,preview.get("retentionDays"));
  var result=service.delete("owner","w",null);assertEquals(false,result.get("archived"));assertEquals(true,result.get("deleted"));assertEquals(0,result.get("retentionDays"));
  verify(store,never()).archive(anyString());verify(spaces).tombstone("w");verify(spaces).deleteWorkspace("w");verify(deletion).enqueue("w","workspace");verifyNoInteractions(mail);
 }
 @Test void usedSpaceCannotBypassVerificationEvenAfterEmptyPreview(){service.preview("owner","w");when(store.used("w")).thenReturn(true);assertEquals(409, assertThrows(ResponseStatusException.class,()->service.delete("owner","w",null)).getStatusCode().value());verify(spaces,never()).deleteWorkspace(anyString());verify(store,never()).archive(anyString());}
 @Test void validCodeArchivesAndConsumesChallengeWithoutDeletingFiles(){when(store.used("w")).thenReturn(true);when(store.valid(eq("w"),eq("owner"),anyString())).thenReturn(1);assertEquals(true,service.delete("owner","w","12345678").get("archived"));verify(store).archive("w");verify(store).clear("w");verifyNoInteractions(deletion);verify(spaces,never()).deleteWorkspace(anyString());}
 @Test void codeIsBoundToOwnerWorkspaceAndCurrentEmail(){when(store.attempt("w","owner")).thenReturn(1);service.verify("owner","w","12345678");verify(store).valid("w","owner",WeaverAuthService.hash("workspace-delete:w:owner:owner@example.invalid:12345678"));assertFalse(service.verify("owner","w","wrong"));}
 @Test void exhaustedAttemptsCannotVerify(){assertFalse(service.verify("owner","w","12345678"));verify(store,never()).valid(anyString(),anyString(),anyString());}
 @Test void nonOwnerCannotRequestDeleteOrRestore(){assertThrows(ResponseStatusException.class,()->service.request("other","w"));assertThrows(ResponseStatusException.class,()->service.delete("other","w",null));assertThrows(ResponseStatusException.class,()->service.restore("other","w"));verifyNoInteractions(mail,deletion);}
 @Test void emailRequestUsesDedicatedPurposeAndCooldown(){when(store.used("w")).thenReturn(true);service.request("owner","w");verify(mail).workspaceDeletion(eq("owner@example.invalid"),matches("[0-9]{8}"));verify(store).issue(eq("w"),eq("owner"),anyString());when(store.recent("w")).thenReturn(1);assertEquals(429, assertThrows(ResponseStatusException.class,()->service.request("owner","w")).getStatusCode().value());}
 @Test void restoreRequiresRemainingRetentionPeriod(){when(store.restore("w")).thenReturn(1);service.restore("owner","w");verify(store).clear("w");when(store.restore("w")).thenReturn(0);assertEquals(410, assertThrows(ResponseStatusException.class,()->service.restore("owner","w")).getStatusCode().value());}
 @Test void purgeRechecksDeadlineWhileLocked(){service.purge("w");verify(spaces,never()).deleteWorkspace(anyString());when(store.expiredOne("w")).thenReturn(1);service.purge("w");verify(spaces).deleteWorkspace("w");verify(deletion).enqueue("w","workspace");}
}
