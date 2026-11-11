package net.dogpoo.api;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import net.dogpoo.api.core.weaver.controller.WeaverTestDataController;
import net.dogpoo.api.core.weaver.service.WeaverTestDataService;
import net.dogpoo.api.core.weaver.mapper.WeaverTestDataMapper;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class WeaverRestoredSourceTest {
 @Test void originalPostContracts() throws Exception {
  var mapper=mock(WeaverTestDataMapper.class);
  when(mapper.insertWeaverTestData(any())).thenReturn(1);
  when(mapper.getWeaverTestData(any())).thenReturn("{\"saved\":true}");
  var service=new WeaverTestDataService();ReflectionTestUtils.setField(service,"mapper",mapper);
  var controller=new WeaverTestDataController();ReflectionTestUtils.setField(controller,"service",service);
  var mvc=MockMvcBuilders.standaloneSetup(controller).build();
  mvc.perform(post("/weaver/api/test/insertWeaverTestData").contentType("application/json").content("{\"account\":\"test\",\"jsonData\":\"{}\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.msg").value("OK"));
  mvc.perform(post("/weaver/api/test/getWeaverTestData").contentType("application/json").content("{\"account\":\"test\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.DATA").value("{\"saved\":true}"));
 }
}
