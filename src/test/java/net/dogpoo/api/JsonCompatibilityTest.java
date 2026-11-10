package net.dogpoo.api;
import org.junit.jupiter.api.Test;
import net.dogpoo.api.common.util.JsonUtil;
import static org.junit.jupiter.api.Assertions.*;
class JsonCompatibilityTest {
 @Test void numericTypesAndEmptyFallbacksRemainCompatible() {
  var parsed=JsonUtil.jsonParamStrToObjectMap("{\"integer\":1,\"large\":2147483648,\"text\":\"한글\",\"nil\":null}");
  assertInstanceOf(Integer.class,parsed.get("integer"));
  assertInstanceOf(Long.class,parsed.get("large"));
  assertEquals("한글",parsed.get("text"));assertTrue(parsed.containsKey("nil"));
  assertTrue(JsonUtil.jsonParamStrToObjectMap("invalid").isEmpty());
  assertTrue(JsonUtil.jsonParamStrToList("invalid").isEmpty());
 }
}
