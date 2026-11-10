package net.dogpoo.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EndpointContractTest {
 @Test void allLegacyMappingAnnotationsRemainUnchanged() throws Exception {
  var inventory=new ObjectMapper().readTree(Path.of("docs/legacy-endpoints.json").toFile());
  var pattern=Pattern.compile("@(?:RequestMapping|GetMapping|PostMapping)\\([^)]*\\)",Pattern.DOTALL);
  int methods=0;
  for(var controller:inventory) {
   String source=Files.readString(Path.of("src/main/java").resolve(controller.get("controller").asText()));
   var actual=pattern.matcher(source).results().map(m->m.group()).toList();
   var expected=new ArrayList<String>();controller.get("mappings").forEach(n->expected.add(n.asText()));
   assertEquals(expected,actual,controller.get("controller").asText());
   methods+=actual.size()-1;
  }
  assertEquals(72,methods);
 }
}
