package net.dogpoo.api.core.weaver.service;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** One-process invalidation bus. Reconnect always reconciles from durable DB revisions. */
@Service
public class WeaverWorkspaceEvents implements AutoCloseable {
 private final AtomicBoolean stopped=new AtomicBoolean();
 private final ConcurrentMap<String,Set<Connection>> workspaces=new ConcurrentHashMap<>();
 private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor();
 private final ExecutorService writers=Executors.newVirtualThreadPerTaskExecutor();
 public WeaverWorkspaceEvents(){clock.scheduleWithFixedDelay(this::tick,250,250,TimeUnit.MILLISECONDS);}
 public SseEmitter subscribe(String workspace,Runnable authorize){
  if(stopped.get())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
  var connection=new Connection(workspace,authorize);
  workspaces.compute(workspace,(key,set)->{if(set==null)set=ConcurrentHashMap.newKeySet();set.add(connection);return set;});
  connection.emitter.onCompletion(connection::remove);
  connection.emitter.onTimeout(connection::close);
  connection.emitter.onError(error->connection.remove());
  // Ready triggers a revision read, covering changes before/during subscription.
  connection.dispatch();
  return connection.emitter;
 }
 public void changedAfterCommit(String workspace){
  if(TransactionSynchronizationManager.isSynchronizationActive()){
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
    @Override public void afterCommit(){changed(workspace);}
   });
  }else changed(workspace);
 }
 void changed(String workspace){var connections=workspaces.get(workspace);if(connections!=null)connections.forEach(c->c.dirty.set(true));}
 SseEmitter emitter(){return new SseEmitter(300_000L);}
 private void tick(){workspaces.values().forEach(set->set.forEach(Connection::dispatch));}
 int connectionCount(){return workspaces.values().stream().mapToInt(Set::size).sum();}
 private class Connection {
  final String workspace;final Runnable authorize;
  final SseEmitter emitter=emitter();
  final AtomicBoolean dirty=new AtomicBoolean(),writing=new AtomicBoolean(),closed=new AtomicBoolean();
  volatile boolean ready;volatile long nextHeartbeat;
  Connection(String workspace,Runnable authorize){this.workspace=workspace;this.authorize=authorize;}
  void dispatch(){
   if(stopped.get() || closed.get() || (ready&&!dirty.get()&&System.nanoTime()<nextHeartbeat) || !writing.compareAndSet(false,true))return;
   try{writers.execute(()->{
    try{
     // Revoked sessions/membership must not keep receiving notifications.
     authorize.run();
     boolean changed=dirty.getAndSet(false);
     String event=!ready?"ready":changed?"change":"ping";
     emitter.send(SseEmitter.event().name(event).data("{}"));
     ready=true;nextHeartbeat=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
    }catch(Exception error){close();}finally{writing.set(false);}
   });}catch(RejectedExecutionException shutdown){close();}
  }
  void remove(){if(closed.compareAndSet(false,true))workspaces.computeIfPresent(workspace,(key,set)->{set.remove(this);return set.isEmpty()?null:set;});}
  void close(){remove();emitter.complete();}
 }
 @org.springframework.context.event.EventListener
 public void stopping(org.springframework.context.event.ContextClosedEvent event){close();}
 @PreDestroy public void close(){if(!stopped.compareAndSet(false,true))return;clock.shutdownNow();workspaces.values().forEach(set->set.forEach(Connection::close));writers.shutdownNow();}
}
