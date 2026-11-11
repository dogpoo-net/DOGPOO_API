package net.dogpoo.api;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
import net.dogpoo.api.core.weaver.controller.WeaverWorkspaceController;
import net.dogpoo.api.core.weaver.service.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class WeaverWorkspaceControllerTest {
 @Test void ownershipComesOnlyFromSessionAndWebOriginIsRequired() throws Exception {
  var sessions=mock(WeaverSessionService.class);var data=mock(WeaverWorkspaceService.class);
  when(sessions.me("Bearer valid")).thenReturn(Map.of("id","account-a","email","a@example.com"));
  when(sessions.me(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  when(data.read("account-a","a@example.com","account-a",0)).thenReturn(Map.of("changes",List.of(),"revision",0));
  var controller=new WeaverWorkspaceController(sessions,data,"https://weaver.dogpoo.net");var sharing=mock(WeaverSharingService.class);when(sharing.access("account-a",null,false)).thenReturn(new WeaverSharingService.Access("account-a","a@example.com","owner"));org.springframework.test.util.ReflectionTestUtils.setField(controller,"sharing",sharing);var mvc=MockMvcBuilders.standaloneSetup(controller).build();
  mvc.perform(post("/weaver/api/auth/session/native/workspace/read").contentType("application/json").content("{}")) .andExpect(status().isUnauthorized());
  mvc.perform(post("/weaver/api/auth/session/native/workspace/read").header("Authorization","Bearer valid").contentType("application/json").content("{}")) .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
  verify(data).read("account-a","a@example.com","account-a",0);
  mvc.perform(post("/weaver/api/auth/session/web/workspace/read").header("Origin","https://evil.example").contentType("application/json").content("{}")) .andExpect(status().isForbidden());
 }
}
