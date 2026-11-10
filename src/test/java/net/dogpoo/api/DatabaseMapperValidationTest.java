package net.dogpoo.api;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="DB_VALIDATION_CONFIG",matches=".+")
class DatabaseMapperValidationTest {
 @Test void serverPreparesEveryMappedStatementWithoutExecutingWrites() throws Exception {
  var p=new Properties();try(var in=Files.newInputStream(Path.of(System.getenv("DB_VALIDATION_CONFIG")))){p.loadFromXML(in);}
  String url=p.getProperty("database.url").replaceFirst("(?<=//)[^/]+","127.0.0.1:"+System.getenv("DB_VALIDATION_PORT"));
  var ds=new DriverManagerDataSource(url,p.getProperty("database.username"),p.getProperty("database.password"));
  var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);
  var resolver=new PathMatchingResourcePatternResolver();
  factory.setConfigLocation(resolver.getResource("classpath:mybatis-config.xml"));
  factory.setMapperLocations(resolver.getResources("classpath*:net/dogpoo/api/core/**/mapper/*Mapper.xml"));
  var config=factory.getObject().getConfiguration();
  try(var paths=Files.walk(Path.of("target/classes/net/dogpoo/api/core"))){
   for(var file:paths.filter(x->x.toString().endsWith("Mapper.class")).toList()){
    String name=Path.of("target/classes").relativize(file).toString().replace('/','.').replace(".class","");
    Class<?> type=Class.forName(name);if(!config.hasMapper(type))config.addMapper(type);
   }
  }
  var failures=new ArrayList<String>();int prepared=0;
  try(var connection=ds.getConnection()){
   assertEquals("DOGPOO_DEV",connection.getCatalog(),"Only the development schema is permitted");
   // PREPARE validates DML but never executes it; MariaDB rejects DML PREPARE in read-only mode.
   for(String id:new TreeSet<>(config.getMappedStatementNames())){
    if(!id.contains("."))continue;
    for(String game: id.contains("BatgersStatisticsMapper") ? List.of("PUBG","ETC") : List.of("PUBG")){
     var params=new HashMap<String,Object>();params.put("page",0);params.put("dateRange",1);params.put("gameCd",game);params.put("target","likeCnt");params.put("cnt","+ 1");params.put("orderTarget","threadNo");
     String sql=config.getMappedStatement(id).getBoundSql(params).getSql();
     try(var set=connection.prepareStatement("SET @weaver_validation_sql = ?")){
      set.setString(1,sql);set.execute();
     }
     try(var statement=connection.createStatement()){
      statement.setQueryTimeout(10);
      statement.execute("PREPARE weaver_validation FROM @weaver_validation_sql");
      statement.execute("DEALLOCATE PREPARE weaver_validation");prepared++;
     }catch(SQLException e){failures.add(id+" ["+game+"] SQLState="+e.getSQLState()+" code="+e.getErrorCode()+" "+e.getMessage());}
    }
   }
  }
  try(var session=factory.getObject().openSession()) {
   session.getConnection().setReadOnly(true);
   String prefix="net.dogpoo.api.core.";
   for(String id:List.of("convatar.mapper.ConvatarHomeMapper.getConvatarReleaseNotesList","convatar.mapper.ConvatarHomeMapper.getNoticeModelList","batgers.mapper.BatgersGameHistoryMapper.getGameHistoryList")) {
    try { var rows=session.selectList(prefix+id,Map.of("page",0));System.out.println("Read mapper PASS: "+id+" rows="+rows.size()); }
    catch(Exception e){failures.add("Read mapper FAIL: "+id+" "+e.getClass().getSimpleName());}
   }
   assertNull(session.selectOne(prefix+"user.mapper.UserMapper.getGestureUserInfoByUserId",Map.of("userId","weaver-validation-nonexistent-' OR 1=1 --")));
   System.out.println("Injected username lookup: no matching user");
  }
  try(var session=factory.getObject().openSession()) {
   var connection=session.getConnection();
   try(var ddl=connection.createStatement()) {
    String definition;
    try(var schema=ddl.executeQuery("SHOW CREATE TABLE DOGPOO_DEV.GESTURE_THREAD_POST_COMMENT")){assertTrue(schema.next());definition=schema.getString(2);}
    ddl.execute(definition.replaceFirst("CREATE TABLE", "CREATE TEMPORARY TABLE"));
   }
   // Only this connection sees the temporary table; no permanent table is written.
   String id="net.dogpoo.api.core.gesture.mapper.GestureForumThreadPostCommentMapper.";
   var params=new HashMap<String,Object>();
   params.put("postNo",1);params.put("threadNo",1);params.put("commentWriter",1);params.put("countryCd","KR");
   params.put("comment","한글 ' OR 1=1 --");
   assertEquals(1,session.insert(id+"insertForumThreadPostComment",params));
   try(var query=connection.createStatement();var result=query.executeQuery("SELECT commentNo, comment FROM GESTURE_THREAD_POST_COMMENT")) {
    assertTrue(result.next());params.put("commentNo",result.getLong(1));assertEquals(params.get("comment"),result.getString(2));
   }
   params.put("comment","updated 한글");assertEquals(1,session.update(id+"updateForumThreadPostComment",params));
   assertEquals(1,session.selectList(id+"getForumThreadPostCommentList",params).size());
   assertEquals(1,session.delete(id+"deleteForumThreadPostComment",params));
   assertTrue(session.selectList(id+"getForumThreadPostCommentList",params).isEmpty());
   System.out.println("Temporary-table comment CRUD: PASS");
  }
  System.out.println("DB server prepared statements: "+prepared+"; failures: "+failures.size());
  assertTrue(failures.isEmpty(),String.join("\n",failures));
 }
}
