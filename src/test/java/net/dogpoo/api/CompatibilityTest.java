package net.dogpoo.api;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import net.dogpoo.api.core.test.controller.DogpooConnectionTestController;
import net.dogpoo.api.common.util.auth.JwtTokenUtil;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.userdetails.User;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class CompatibilityTest {
 @Test void legacyConnectionEndpoints() throws Exception {
  var mvc=MockMvcBuilders.standaloneSetup(new DogpooConnectionTestController(null)).build();
  mvc.perform(get("/api/checkGet")).andExpect(status().isOk()).andExpect(jsonPath("$.result").value("겟 통신 정상"));
  mvc.perform(post("/api/checkPost").contentType("application/json").content("{}")).andExpect(status().isOk()).andExpect(jsonPath("$.result").value("포스트 통신 정상"));
 }
 @Test void jwtClaimsAndMalformedBearer() {
  var util=new JwtTokenUtil();
  ReflectionTestUtils.setField(util,"JWT_SECRET_KEY",java.util.Base64.getEncoder().encodeToString(new byte[64]));
  var user=User.withUsername("compatibility-user").password("unused").authorities("USER").build();
  String token=util.generateToken(user,"123","nickname");
  assertTrue(util.validateToken(token,user));
  assertEquals("123",util.getUserKeyFromJwtToken("Bearer "+token));
  assertEquals("402",util.getUserKeyFromJwtToken("Bearer"));
 }
}
