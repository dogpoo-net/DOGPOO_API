package net.dogpoo.api.core.weaver.model;
import java.util.*;import com.fasterxml.jackson.databind.JsonNode;
public class WeaverWorkspaceRequest {
 public String workspaceId,documentId,leaseToken,resourceId; public boolean release, takeover, renewOnly;
 public Map<String,String> documentLeases=Map.of();
 public long since; public String guestId,sourceHash,mediaId,data,mediaType;
 public List<Change> changes=List.of();
 public static class Change {public String kind,id;@tools.jackson.databind.annotation.JsonDeserialize(using=WorkspaceValueDeserializer.class) public JsonNode value;public long baseRevision;public boolean deleted;}
}
