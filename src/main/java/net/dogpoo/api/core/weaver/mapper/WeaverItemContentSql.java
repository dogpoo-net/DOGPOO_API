package net.dogpoo.api.core.weaver.mapper;
import java.util.Map;
/** Fixed table routing; request values are never interpolated into SQL identifiers. */
public final class WeaverItemContentSql {
 private WeaverItemContentSql() {}
 public static String table(String kind) {
  return switch(kind) {
   case "list:elements", "list:trash" -> "WEAVER_MEMO_CONTENT";
   case "list:notes" -> "WEAVER_DOCUMENT_CONTENT";
   case "layout" -> "WEAVER_CANVAS_LAYOUT";
   case "list:memoBoxes", "list:memoBoxFolders", "list:folders", "list:elementFolders", "list:hashtagFolders", "list:paragraphFolders", "map:memoBoxFolderParents", "map:hashtagFolderAssignments" -> "WEAVER_WORKSPACE_STRUCTURE";
   default -> "WEAVER_WORKSPACE_SETTING";
  };
 }
 public static String put(Map<String,Object> args) {
  return "INSERT INTO "+table((String)args.get("kind"))+"(workspace_id,item_key,payload) VALUES(#{user},#{key},#{payload}) ON DUPLICATE KEY UPDATE payload=VALUES(payload)";
 }
 public static String delete(Map<String,Object> args) {
  return "DELETE FROM "+table((String)args.get("kind"))+" WHERE workspace_id=#{user} AND item_key=#{key}";
 }
 public static final String PAYLOAD="COALESCE(mc.payload,dc.payload,cl.payload,ws.payload,st.payload)";
 public static final String JOINS=" LEFT JOIN WEAVER_MEMO_CONTENT mc ON mc.workspace_id=w.user_id AND mc.item_key=w.item_key"
  +" LEFT JOIN WEAVER_DOCUMENT_CONTENT dc ON dc.workspace_id=w.user_id AND dc.item_key=w.item_key"
  +" LEFT JOIN WEAVER_CANVAS_LAYOUT cl ON cl.workspace_id=w.user_id AND cl.item_key=w.item_key"
  +" LEFT JOIN WEAVER_WORKSPACE_STRUCTURE ws ON ws.workspace_id=w.user_id AND ws.item_key=w.item_key"
  +" LEFT JOIN WEAVER_WORKSPACE_SETTING st ON st.workspace_id=w.user_id AND st.item_key=w.item_key";
}
