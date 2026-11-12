package net.dogpoo.api.core.weaver.model;
import java.util.*;
public class WeaverWithdrawalRequest {
 public String code;
 public boolean confirmed;
 public Map<String,String> transfers=Map.of();
 public List<String> deleteWorkspaces=List.of();
}
