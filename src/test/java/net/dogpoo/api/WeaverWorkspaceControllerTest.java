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
  when(data.read("account-a","a@example.com","account-a",0)).thenReturn(Map.of("changes",List.of(new net.dogpoo.api.core.weaver.model.WeaverWorkspaceItem("list:notes","n","{}",false,1,"","account-a",null)),"revision",1));
  var controller=new WeaverWorkspaceController(sessions,data,"https://weaver.dogpoo.net");var sharing=mock(WeaverSharingService.class);when(sharing.access("account-a",null,false)).thenReturn(new WeaverSharingService.Access("account-a","a@example.com","owner"));org.springframework.test.util.ReflectionTestUtils.setField(controller,"sharing",sharing);var mvc=MockMvcBuilders.standaloneSetup(controller).build();
  mvc.perform(post("/weaver/api/auth/session/native/workspace/read").contentType("application/json").content("{}")) .andExpect(status().isUnauthorized());
  mvc.perform(post("/weaver/api/auth/session/native/workspace/read").header("Authorization","Bearer valid").contentType("application/json").content("{}")) .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.changes[0].authorId").value("account-a"));
  verify(data).read("account-a","a@example.com","account-a",0);
  mvc.perform(post("/weaver/api/auth/session/web/workspace/read").header("Origin","https://evil.example").contentType("application/json").content("{}")) .andExpect(status().isForbidden());
 }
 @Test void jackson3HttpRequestsDecodeWorkspaceValuesWithoutLosingTheirShape()throws Exception{
  var sessions=mock(WeaverSessionService.class);var data=mock(WeaverWorkspaceService.class);var sharing=mock(WeaverSharingService.class);
  when(sessions.me("Bearer test")).thenReturn(Map.of("id","account-a","email","a@example.invalid"));
  when(sharing.access("account-a",null,true)).thenReturn(new WeaverSharingService.Access("account-a","a@example.invalid","owner"));
  when(data.save(eq("account-a"),eq("a@example.invalid"),eq("account-a"),any(),eq(false))).thenReturn(Map.of("revision",1,"changes",List.of()));
  var controller=new WeaverWorkspaceController(sessions,data,"https://weaver.dogpoo.net");org.springframework.test.util.ReflectionTestUtils.setField(controller,"sharing",sharing);
  var mvc=MockMvcBuilders.standaloneSetup(controller).setMessageConverters(new org.springframework.http.converter.json.JacksonJsonHttpMessageConverter()).build();
  mvc.perform(post("/weaver/api/auth/session/native/workspace/save").header("Authorization","Bearer test").contentType("application/json").content("""
   {"changes":[{"kind":"list:elements","id":"thought","value":{"text":"한글 메모","tags":["test"],"flag":true,"nested":{"x":null}}},{"kind":"layout","id":"position","value":[12.5,42]},{"kind":"scalar","id":"version","value":1},{"kind":"list:elements","id":"deleted","value":null,"deleted":true}]}
   """ )).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
  var captured=org.mockito.ArgumentCaptor.forClass(net.dogpoo.api.core.weaver.model.WeaverWorkspaceRequest.class);verify(data).save(eq("account-a"),eq("a@example.invalid"),eq("account-a"),captured.capture(),eq(false));
  var changes=captured.getValue().changes;org.junit.jupiter.api.Assertions.assertEquals("한글 메모",changes.get(0).value.path("text").asText());org.junit.jupiter.api.Assertions.assertTrue(changes.get(0).value.path("flag").asBoolean());org.junit.jupiter.api.Assertions.assertEquals(12.5,changes.get(1).value.get(0).asDouble());org.junit.jupiter.api.Assertions.assertEquals(1,changes.get(2).value.asInt());org.junit.jupiter.api.Assertions.assertNull(changes.get(3).value);
 }
}
