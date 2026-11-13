package net.dogpoo.api.core.weaver.service;
import java.util.*;import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import net.dogpoo.api.core.weaver.mapper.WeaverHistoryMapper;
@Service public class WeaverHistoryService {
 private final WeaverHistoryMapper mapper;private final ObjectMapper json=new ObjectMapper();
 public WeaverHistoryService(WeaverHistoryMapper mapper){this.mapper=mapper;}
 public record Difference(List<Map<String,Object>> chunks,int added,int removed){}
 public static Difference difference(String before,String after){
  // Encode Unicode code points as single algorithm symbols, so emoji are not
  // split into surrogate halves and the counts are code points, not UTF-16 units.
  var codes=new LinkedHashMap<Integer,Character>();var reverse=new HashMap<Character,Integer>();
  int[] next={1};java.util.function.IntFunction<Character> symbol=cp->codes.computeIfAbsent(cp,k->{if(next[0]==0xD800)next[0]=0xE000;if(next[0]>=0xFFFE)throw new IllegalArgumentException("Too many distinct characters");char c=(char)next[0]++;reverse.put(c,k);return c;});
  StringBuilder left=new StringBuilder(),right=new StringBuilder();before.codePoints().forEach(cp->left.append(symbol.apply(cp)));after.codePoints().forEach(cp->right.append(symbol.apply(cp)));
  var algorithm=new DiffMatchPatch();algorithm.diffTimeout=1.0f;var differences=algorithm.diffMain(left.toString(),right.toString(),false);
  var chunks=new ArrayList<Map<String,Object>>();int added=0,removed=0,beforeOffset=0,afterOffset=0,beforeLine=1,afterLine=1;
  for(var d:differences){StringBuilder text=new StringBuilder();for(int i=0;i<d.text.length();i++)text.appendCodePoint(reverse.get(d.text.charAt(i)));String value=text.toString(),operation=d.operation.name().toLowerCase();
   chunks.add(Map.of("operation",operation,"text",value,"beforeOffset",beforeOffset,"afterOffset",afterOffset,"beforeLine",beforeLine,"afterLine",afterLine));
   int count=d.text.length(),lines=(int)value.chars().filter(c->c=='\n').count();
   if(d.operation!=DiffMatchPatch.Operation.INSERT){beforeOffset+=count;beforeLine+=lines;}
   if(d.operation!=DiffMatchPatch.Operation.DELETE){afterOffset+=count;afterLine+=lines;}
   if(d.operation==DiffMatchPatch.Operation.INSERT)added+=count;if(d.operation==DiffMatchPatch.Operation.DELETE)removed+=count;
  }
  return new Difference(chunks,added,removed);
 }
 private JsonNode project(String payload){try{return payload==null?json.nullNode():json.readTree(payload).path("project");}catch(Exception e){throw new IllegalArgumentException("Invalid document");}}
 private String text(JsonNode project){if(project.isMissingNode() || project.isNull())return "";if(project.path("document").isTextual())return project.path("document").asText();var parts=new ArrayList<String>();for(var p:project.path("paragraphs"))parts.add(p.path("text").asText());return String.join("\n\n",parts);}
 public void record(String workspace,String actor,String kind,String itemId,String before,String after,String action){
  if(!kind.equals("list:notes"))return;
  var old=project(before);var current=project(after);if((old.isMissingNode()||old.isNull()) && (current.isMissingNode()||current.isNull()))return;
  String previous=text(old),next=text(current);if(previous.equals(next))return;
  String document=current.path("id").asText(old.path("id").asText(itemId)),key=WeaverAuthService.hash(document);
  var diff=difference(previous,next);var row=new HashMap<String,Object>();row.put("workspace",workspace);row.put("key",key);row.put("document",document);row.put("version",mapper.version(workspace,key)+1);row.put("actor",actor);row.put("action",action.equals("migration")||action.equals("import")||action.equals("unlocked_edit")?action:after==null?"delete":before==null?"create":action);row.put("before",previous);row.put("after",next);row.put("added",diff.added());row.put("removed",diff.removed());
  try{row.put("changes",json.writeValueAsString(diff.chunks()));}catch(Exception e){throw new IllegalStateException(e);}mapper.insert(row);
 }
 public List<Map<String,Object>> list(String workspace,String document,long before){if(document==null || document.length()>512)throw new IllegalArgumentException("Document required");return mapper.list(workspace,WeaverAuthService.hash(document),before<=0?Long.MAX_VALUE:before);}
 public long bytes(String workspace){return mapper.bytes(workspace);}
}
