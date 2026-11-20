package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.time.*;
import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;import org.springframework.test.util.ReflectionTestUtils;
import net.dogpoo.api.core.weaver.mapper.*;import net.dogpoo.api.core.weaver.model.*;
class WeaverWorkspacePolicyTest {
 WeaverPolicyMapper db=mock(WeaverPolicyMapper.class);WeaverWorkspaceMapper spaces=mock(WeaverWorkspaceMapper.class);WeaverSharingMapper members=mock(WeaverSharingMapper.class);
 WeaverWorkspacePolicyService policy=new WeaverWorkspacePolicyService(db,spaces,members);
 @BeforeEach void setup(){when(spaces.owner("w")).thenReturn("owner");when(spaces.revision("w")).thenReturn(1L);when(db.plan("w")).thenReturn(Map.of("code","free","editor_limit",2));when(db.plans()).thenReturn(List.of(Map.of("code","free")));when(db.accountPlan(anyString())).thenReturn(Map.of("code","free","owned_limit",2));when(db.owned("owner")).thenReturn(List.of(Map.of("workspaceId","w","plan","free")));}
 @Test void suspensionReasonsPreserveCancellationAndNeverSubscribedDistinction(){
  assertEquals("free",policy.sharingSuspensionReason("w"));
  when(db.subscription("owner")).thenReturn(Map.of("cancelAtEnd",false));assertEquals("expired",policy.sharingSuspensionReason("w"));
  when(db.subscription("owner")).thenReturn(Map.of("cancelAtEnd",true));assertEquals("cancelled",policy.sharingSuspensionReason("w"));
  when(db.plan("w")).thenReturn(Map.of("code","standard"));assertNull(policy.sharingSuspensionReason("w"));
 }
 @Test void suspendedMembershipRemainsInCatalogWithoutBecomingDefault(){
  when(members.memberships("guest")).thenReturn(List.of(Map.of("workspaceId","w","role","viewer")));
  var sharing=new WeaverSharingService(spaces,members,mock(WeaverSessionMapper.class));ReflectionTestUtils.setField(sharing,"policy",policy);
  var result=sharing.catalog("guest","guest@test.invalid");assertNull(result.get("defaultWorkspaceId"));
  var rows=(List<?>)result.get("workspaces");assertEquals(1,rows.size());assertEquals(true,((Map<?,?>)rows.getFirst()).get("sharingSuspended"));
  assertThrows(ResponseStatusException.class,()->sharing.access("guest","w",false));
 }
 @Test void joinedMembershipsNeverConsumeOwnedQuota(){when(db.accountPlan(anyString())).thenReturn(Map.of("code","free","owned_limit",1));policy.admission("guest");verifyNoInteractions(members);}
 @Test void ownedLimitBlocksAdmissionAtBoundary(){when(db.accountPlan(anyString())).thenReturn(Map.of("code","free","owned_limit",1));when(db.owned("owner")).thenReturn(List.of(Map.of("workspaceId","w","plan","free")));assertEquals(422,assertThrows(ResponseStatusException.class,()->policy.admission("owner")).getStatusCode().value());}
 @Test void proOwnedLimitIsUnlimited(){when(db.accountPlan("owner")).thenReturn(Map.of("code","pro"));assertNull(policy.ownedLimit("owner"));policy.admission("owner");}
 @Test void freeHasOwnerAndOneEditor(){when(db.editors("w")).thenReturn(List.of("a","b"));assertTrue(policy.editorAllowed("w","a"));assertFalse(policy.editorAllowed("w","b"));when(members.editors("w")).thenReturn(1);assertThrows(ResponseStatusException.class,()->policy.editorSeat("w",1));}
 @Test void paidSpaceAllowsFreeAccountEditorWithUnlimitedPaidSeats(){when(db.plan("w")).thenReturn(Map.of("code","pro"));assertTrue(policy.editorAllowed("w","free-user"));policy.editorSeat("w",1);}
 @Test void restorationRequiresPaidEntitlementAndAvailableSlot() {
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> policy.validateRestore("owner", "w")).getStatusCode().value());
  when(db.plan("w")).thenReturn(Map.of("code", "standard"));
  when(db.accountPlan("owner")).thenReturn(Map.of("code", "standard", "owned_limit", 1));
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> policy.validateRestore("owner", "w")).getStatusCode().value());
  when(db.owned("owner")).thenReturn(List.of(Map.of("workspaceId", "w", "archivedAt", Instant.EPOCH)));
  assertDoesNotThrow(() -> policy.validateRestore("owner", "w"));
 }
 @Test void restoreRejectsStorageAndDocumentOverflow() {
  when(db.plan("w")).thenReturn(Map.of("code", "pro", "storage_limit", 100L, "document_limit", 2));
  when(db.accountPlan("owner")).thenReturn(Map.of("code", "pro"));
  when(db.bytes("w")).thenReturn(101L);
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> policy.validateRestore("owner", "w")).getStatusCode().value());
  when(db.bytes("w")).thenReturn(100L);
  when(db.documents("w")).thenReturn(3L);
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> policy.validateRestore("owner", "w")).getStatusCode().value());
  when(db.documents("w")).thenReturn(2L);
  assertDoesNotThrow(() -> policy.validateRestore("owner", "w"));
 }
 @Test void documentOverflowAllowsSameCountEditsButNotNewDocuments(){when(db.plan("w")).thenReturn(Map.of("code","standard","document_limit",1));when(db.documents("w")).thenReturn(3L);policy.validateSave("w",List.of(),0,3);assertThrows(ResponseStatusException.class,()->policy.validateSave("w",List.of(),0,2));}
 @Test void storageOverflowAllowsShrinkAndRejectsGrowth(){when(db.plan("w")).thenReturn(Map.of("code","standard","storage_limit",100L));when(db.bytes("w")).thenReturn(120L);policy.validateSave("w",List.of(),130,0);assertThrows(ResponseStatusException.class,()->policy.validateSave("w",List.of(),110,0));assertThrows(ResponseStatusException.class,()->policy.validateMedia("w",1));}
 @Test void ordinaryUserCannotUseAdminQueriesOrMutations(){assertThrows(ResponseStatusException.class,()->policy.adminUsers("guest",""));assertThrows(ResponseStatusException.class,()->policy.adminRole("guest","owner","admin"));assertThrows(ResponseStatusException.class,()->policy.adminSubscription("guest","w","pro",Instant.now().plusSeconds(1000).toString(),false));verify(db,never()).subscribe(anyString(),anyString(),any(),anyBoolean());}
 @Test void lastAdministratorCannotBeDemoted(){when(db.platformRole("owner")).thenReturn("admin");when(db.admins()).thenReturn(1);assertThrows(ResponseStatusException.class,()->policy.adminRole("owner","owner","user"));verify(db,never()).platformRoleSet(anyString(),anyString());}
 @Test void administratorRoleChangesAreAudited(){when(db.platformRole("owner")).thenReturn("admin");when(db.platformRole("target")).thenReturn("user");policy.adminRole("owner","target","admin");verify(db).platformRoleSet("target","admin");verify(db).audit("owner","account_role","target","user -> admin");}
 @Test void cancellationIsOwnerOnlyAndDoesNotChangeEndDate(){when(db.cancel("owner",true)).thenReturn(1);policy.cancel("owner","w",true);verify(db).cancel("owner",true);verify(db,never()).subscribe(anyString(),anyString(),any(),anyBoolean());assertThrows(ResponseStatusException.class,()->policy.cancel("guest","w",true));}
 @Test void invalidAdminSubscriptionCannotGrantAccess(){when(db.platformRole("owner")).thenReturn("admin");when(db.user("w")).thenReturn(Map.of("id","w"));assertThrows(ResponseStatusException.class,()->policy.adminSubscription("owner","w","pro","yesterday",false));assertThrows(ResponseStatusException.class,()->policy.adminSubscription("owner","w","pro",Instant.now().minusSeconds(1).toString(),false));}
 @Test void newAccountsAreProvisionedExactlyOnce(){when(db.provisioned("new")).thenReturn(0,1);policy.initializeAccount("new");policy.initializeAccount("new");verify(spaces,times(1)).ensure("new");verify(db,times(1)).provision("new");}
 @Test void emptyCatalogDoesNotCreateAWorkspace(){var accounts=mock(WeaverSessionMapper.class);var sharing=new WeaverSharingService(spaces,members,accounts);ReflectionTestUtils.setField(sharing,"policy",policy);assertNull(sharing.catalog("owner","owner@test.invalid").get("defaultWorkspaceId"));verify(spaces,never()).ensure(anyString());verify(spaces,never()).create(anyString(),anyString());}
 @Test void freeOrLastOwnedWorkspaceCannotBeDeleted() {
  final var lifecycle = mock(WeaverWorkspaceLifecycleMapper.class);
  final var deletion = mock(WeaverWithdrawalMapper.class);
  final var service = new WeaverWorkspaceLifecycleService(spaces, lifecycle,
      mock(WeaverSessionMapper.class), deletion, mock(WeaverVerificationMail.class));
  ReflectionTestUtils.setField(service, "policy", policy);
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> service.preview("owner", "w")).getStatusCode().value());
  when(db.accountPlan("owner")).thenReturn(Map.of("code", "standard", "owned_limit", 10));
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> service.delete("owner", "w", "12345678")).getStatusCode().value());
  verify(spaces, never()).deleteWorkspace(anyString());
  verify(lifecycle, never()).archive(anyString());
  verifyNoInteractions(deletion);
 }
 @Test void paidDeletionWithAnotherActiveWorkspaceRetainsThirtyDays() {
  final var lifecycle = mock(WeaverWorkspaceLifecycleMapper.class);
  final var accounts = mock(WeaverSessionMapper.class);
  final var deletion = mock(WeaverWithdrawalMapper.class);
  final var service = new WeaverWorkspaceLifecycleService(spaces, lifecycle, accounts,
      deletion, mock(WeaverVerificationMail.class));
  ReflectionTestUtils.setField(service, "policy", policy);
  when(accounts.account("owner")).thenReturn(new WeaverAccount("owner", "owner@test.invalid", null, null));
  when(db.accountPlan("owner")).thenReturn(Map.of("code", "pro"));
  when(db.owned("owner")).thenReturn(List.of(Map.of("workspaceId", "w"), Map.of("workspaceId", "spare")));
  when(lifecycle.used("w")).thenReturn(true);
  when(lifecycle.valid(eq("w"), eq("owner"), anyString())).thenReturn(1);
  assertEquals(30, service.preview("owner", "w").get("retentionDays"));
  assertEquals(true, service.delete("owner", "w", "12345678").get("archived"));
  verify(lifecycle).archive("w");
  verifyNoInteractions(deletion);
 }
 @Test void editorSelectionRejectsOutsidersAndKeepsOwnerSeat(){when(members.members("w")).thenReturn(List.of(Map.of("userId","a"),Map.of("userId","b")));assertThrows(ResponseStatusException.class,()->policy.chooseEditors("owner","w",List.of("a","b")));assertThrows(ResponseStatusException.class,()->policy.chooseEditors("owner","w",List.of("outsider")));policy.chooseEditors("owner","w",List.of("b"));verify(members).setRole("w","a","viewer");verify(members).setRole("w","b","editor");}
 @Test void creationUsesOwnedCapacityWithoutTheLegacyCooldown() {
  when(db.coolingDown("owner")).thenReturn(1);
  when(db.owned("owner")).thenReturn(List.of());
  assertDoesNotThrow(() -> policy.creation("owner"));
  when(db.owned("owner")).thenReturn(List.of(Map.of("workspaceId", "w")));
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> policy.creation("owner")).getStatusCode().value());
  when(db.accountPlan("owner")).thenReturn(Map.of("code", "standard", "owned_limit", 10));
  assertDoesNotThrow(() -> policy.creation("owner"));
  verify(db, never()).coolingDown("owner");
 }
 @Test void freeCloudSaveAndMediaAreRejected(){assertThrows(ResponseStatusException.class,()->policy.validateSave("w",List.of(),0,0));assertThrows(ResponseStatusException.class,()->policy.validateMedia("w",1));}
 @Test void successfulCreateRecordsTimestampButRejectedCreateDoesNot() {
  final var sharing = new WeaverSharingService(spaces, members, mock(WeaverSessionMapper.class));
  ReflectionTestUtils.setField(sharing, "policy", policy);
  when(db.owned("owner")).thenReturn(List.of());
  sharing.createWorkspace("owner", "First");
  verify(db).created("owner");

  clearInvocations(db, spaces);
  when(db.owned("owner")).thenReturn(List.of(Map.of("workspaceId", "w")));
  assertEquals(422, assertThrows(ResponseStatusException.class,
      () -> sharing.createWorkspace("owner", "Blocked")).getStatusCode().value());
  verify(db, never()).created(anyString());
  verify(spaces, never()).create(anyString(), anyString());
 }
}
