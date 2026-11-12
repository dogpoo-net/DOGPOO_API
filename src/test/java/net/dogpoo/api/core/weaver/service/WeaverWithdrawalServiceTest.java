package net.dogpoo.api.core.weaver.service;
import java.util.*;import java.time.*;
import org.junit.jupiter.api.*;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.mapper.*;import net.dogpoo.api.core.weaver.model.*;
class WeaverWithdrawalServiceTest {
 WeaverWithdrawalMapper deletion=mock(WeaverWithdrawalMapper.class);WeaverWorkspaceMapper workspaces=mock(WeaverWorkspaceMapper.class);WeaverSharingMapper members=mock(WeaverSharingMapper.class);WeaverSharingService sharing=mock(WeaverSharingService.class);WeaverSessionMapper accounts=mock(WeaverSessionMapper.class);WeaverVerificationMail mail=mock(WeaverVerificationMail.class);
 WeaverWithdrawalService service=new WeaverWithdrawalService(deletion,workspaces,members,sharing,accounts,mail);
 @BeforeEach void setup(){
  var account=mock(WeaverAccount.class);when(account.email()).thenReturn("delete@example.invalid");when(accounts.account("user")).thenReturn(account);
  when(deletion.verify(eq("user"),anyString())).thenReturn(1);when(deletion.deleteAccount("user")).thenReturn(1);
  when(workspaces.owned("user")).thenReturn(List.of(Map.of("workspaceId","space","role","owner")));when(workspaces.owner("space")).thenReturn("user");
 }
 WeaverWithdrawalRequest request(){var r=new WeaverWithdrawalRequest();r.code="12345678";r.confirmed=true;return r;}
 @Test void everyOwnedWorkspaceRequiresExplicitTransferOrDeletion(){
  var r=request();assertThrows(ResponseStatusException.class,()->service.erase("user",r));verify(deletion,never()).deleteAccount(anyString());
  r.deleteWorkspaces=List.of("other");assertThrows(ResponseStatusException.class,()->service.erase("user",r));verify(workspaces,never()).deleteWorkspace(anyString());
 }
 @Test void deletesOnlyConsentedWorkspacesAndQueuesAllOwnedFiles(){
  var r=request();r.deleteWorkspaces=List.of("space");service.erase("user",r);
  verify(deletion).enqueue("space","workspace");verify(workspaces).deleteWorkspace("space");verify(deletion).enqueue("user","profile");verify(deletion).deleteLegacy("delete@example.invalid");verify(deletion).deleteFlows("user","delete@example.invalid");verify(deletion).deleteReceived("delete@example.invalid");verify(deletion).deleteAccount("user");
 }
 @Test void transferredWorkspaceIsNotDeletedOrQueuedForFileErasure(){
  var r=request();r.transfers=Map.of("space","recipient");service.erase("user",r);
  verify(sharing).transferForWithdrawal("user","space","recipient");verify(workspaces,never()).deleteWorkspace(anyString());verify(deletion,never()).enqueue("space","workspace");verify(deletion).deleteAccount("user");
 }
 @Test void concurrentOwnershipChangeAndMissingConsentPreventDeletion(){
  var r=request();r.deleteWorkspaces=List.of("space");when(workspaces.owner("space")).thenReturn("other");assertThrows(ResponseStatusException.class,()->service.erase("user",r));verify(deletion,never()).deleteAccount(anyString());
  r.confirmed=false;assertThrows(ResponseStatusException.class,()->service.erase("user",r));
 }
 @Test void failedVerificationCountsAttemptsWithoutIssuingDeletion(){
  when(deletion.attempt("user")).thenReturn(1);assertFalse(service.verify("user","bad"));verify(deletion,never()).deleteAccount(anyString());
  when(deletion.attempt("user")).thenReturn(0);assertFalse(service.verify("user","12345678"));
 }
}
