package net.dogpoo.api.core.weaver.model;
import java.util.*;import com.fasterxml.jackson.databind.JsonNode;
public class WeaverWorkspaceRequest {
 public long since; public String guestId,sourceHash,mediaId,data,mediaType;
 public List<Change> changes=List.of();
 public static class Change {public String kind,id;public JsonNode value;public long baseRevision;public boolean deleted;}
}
