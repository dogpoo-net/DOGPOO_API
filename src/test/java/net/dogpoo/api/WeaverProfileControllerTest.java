package net.dogpoo.api;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
import net.dogpoo.api.core.weaver.controller.WeaverProfileController;
import net.dogpoo.api.core.weaver.service.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class WeaverProfileControllerTest {
 @Test void profileOwnerIsSessionOnlyAndCrossOriginWritesAreRejected() throws Exception {
  var sessions=mock(WeaverSessionService.class);var profiles=mock(WeaverProfileService.class);var limits=mock(WeaverAuthRateLimit.class);
  when(sessions.me("Bearer valid")).thenReturn(Map.of("id","account-a"));when(sessions.me(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  var mvc=MockMvcBuilders.standaloneSetup(new WeaverProfileController(sessions,profiles,limits,"https://weaver.dogpoo.net")).build();
  mvc.perform(post("/weaver/api/auth/session/native/profile/name").header("Authorization","Bearer valid").contentType("application/json").content("{\"name\":\"New\",\"userId\":\"account-b\"}")) .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));verify(profiles).name("account-a","New");
  mvc.perform(post("/weaver/api/auth/session/native/profile/read").contentType("application/json").content("{}")) .andExpect(status().isUnauthorized());
  mvc.perform(post("/weaver/api/auth/session/web/profile/name").header("Origin","https://evil.example").contentType("application/json").content("{\"name\":\"New\"}")) .andExpect(status().isForbidden());
  mvc.perform(post("/weaver/api/auth/session/web/profile/read").header("Origin","https://weaver.dogpoo.net").contentType("application/json").content("{}")) .andExpect(status().isUnauthorized());
 }
}
