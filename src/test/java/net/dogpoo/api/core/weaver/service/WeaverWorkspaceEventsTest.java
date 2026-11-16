package net.dogpoo.api.core.weaver.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

class WeaverWorkspaceEventsTest {
 static class Emitter extends SseEmitter {
  final AtomicInteger sent=new AtomicInteger();
  @Override public void send(SseEventBuilder event){sent.incrementAndGet();}
 }
 static class Bus extends WeaverWorkspaceEvents {
  @Override SseEmitter emitter(){return new Emitter();}
 }
 @Test void onlyCommittedWorkspaceInvalidationsAreDeliveredAndBurstsCoalesce(){
  try(var bus=new Bus()) {
   var a=(Emitter)bus.subscribe("a",()->{});var b=(Emitter)bus.subscribe("b",()->{});
   await().atMost(Duration.ofSeconds(2)).until(()->a.sent.get()==1&&b.sent.get()==1);
   TransactionSynchronizationManager.initSynchronization();
   try{
    bus.changedAfterCommit("a");
    assertEquals(1,a.sent.get());
    // Simulate successful commit. Rollback never invokes this callback.
    var callbacks=TransactionSynchronizationManager.getSynchronizations();
    assertEquals(1,callbacks.size());callbacks.forEach(c->c.afterCommit());
   }finally{TransactionSynchronizationManager.clearSynchronization();}
   await().atMost(Duration.ofSeconds(2)).until(()->a.sent.get()==2);
   assertEquals(1,b.sent.get());
   TransactionSynchronizationManager.initSynchronization();
   try{bus.changedAfterCommit("b");}finally{TransactionSynchronizationManager.clearSynchronization();}
   assertEquals(1,b.sent.get());
  }
 }
 @Test void revocationClosesConnectionBeforeSendingChange(){
  try(var bus=new Bus()){
   var authorized=new AtomicBoolean(true);
   var emitter=(Emitter)bus.subscribe("a",()->{if(!authorized.get())throw new SecurityException();});
   await().atMost(Duration.ofSeconds(2)).until(()->emitter.sent.get()==1);
   authorized.set(false);bus.changedAfterCommit("a");
   await().atMost(Duration.ofSeconds(2)).until(()->bus.connectionCount()==0);
   assertEquals(1,emitter.sent.get());
  }
 }
}
