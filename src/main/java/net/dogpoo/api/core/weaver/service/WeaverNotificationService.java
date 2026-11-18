package net.dogpoo.api.core.weaver.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import net.dogpoo.api.core.weaver.mapper.WeaverNotificationMapper;
@Service public class WeaverNotificationService {
 private final WeaverNotificationMapper store;
 public WeaverNotificationService(WeaverNotificationMapper store){this.store=store;}
 @Transactional public Map<String,Object> list(String user,long before,boolean countOnly){
  if(before<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  store.invitations(user);store.expired(user);
  return Map.of("items",countOnly?List.of():store.list(user,before),"unread",store.unread(user));
 }
 @Transactional public void read(String user,long id,boolean all){
  if(id<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  if(all)store.readAll(user,id);else store.read(user,id);
 }
 public void comment(String workspace,String memo,String actor,String request){store.comment(workspace,memo,actor,request);}
 public void subscription(String user,String title,String body){store.subscription(user,"subscription:"+UUID.randomUUID(),title,body);}
}
