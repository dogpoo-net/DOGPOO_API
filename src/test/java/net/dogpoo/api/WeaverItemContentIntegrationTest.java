package net.dogpoo.api;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import net.dogpoo.api.core.weaver.mapper.*;

/** Run against the disposable MariaDB created by tool/test-content-split.sh. */
@EnabledIfEnvironmentVariable(named="WEAVER_SPLIT_TEST_URL",matches=".+")
class WeaverItemContentIntegrationTest {
 SqlSessionFactory factory;
 @BeforeEach void setup(){
  var source=new UnpooledDataSource("org.mariadb.jdbc.Driver",System.getenv("WEAVER_SPLIT_TEST_URL"),"root","local-split-test");
  var config=new Configuration(new Environment("test",new JdbcTransactionFactory(),source));
  config.addMapper(WeaverWorkspaceMapper.class);
  factory=new SqlSessionFactoryBuilder().build(config);
 }
 long count(SqlSession session,String sql)throws Exception{try(var statement=session.getConnection().createStatement();var result=statement.executeQuery(sql)){result.next();return result.getLong(1);}}
 @Test void legacyMigrationRetainsPayloadsRevisionsAndUtf8Size()throws Exception{
  try(var session=factory.openSession()){
   var mapper=session.getMapper(WeaverWorkspaceMapper.class);
   var rows=mapper.changes("legacy",0);assertEquals(8,rows.size());
   assertEquals("{\"text\":\"한글 메모 😀\"}",mapper.item("legacy","a").payload());
   assertTrue(mapper.item("legacy","g").deleted());assertNull(mapper.item("legacy","g").payload());
   assertEquals(4,mapper.changes("legacy",4).size());
   long bytes=rows.stream().filter(r->!r.deleted()).mapToLong(r->r.payload().getBytes(StandardCharsets.UTF_8).length).sum();
   assertEquals(bytes,mapper.itemBytes("legacy"));assertEquals(2,mapper.attachmentRecords("legacy").size());
   assertEquals(0,count(session,"SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='WEAVER_WORKSPACE_ITEM' AND COLUMN_NAME='payload'"));
  }
 }
 @Test void mapperRoutesAllTypesDeletesRestoresRollsBackAndCascades()throws Exception{
  String workspace=UUID.randomUUID().toString();
  try(var session=factory.openSession()){
   session.getConnection().createStatement().executeUpdate("INSERT INTO WEAVER_ACCOUNT_WORKSPACE(user_id) VALUES('"+workspace+"')");
   var mapper=session.getMapper(WeaverWorkspaceMapper.class);
   var kinds=List.of("list:elements","list:trash","list:notes","layout","list:memoBoxes","list:memoBoxFolders","map:memoBoxFolderParents","map:colors","scalar","map:futureSetting");
   long expected=0;
   for(int i=0;i<kinds.size();i++){
    String kind=kinds.get(i),key="key"+i,payload="\"한글😀"+i+"\"";
    mapper.put(workspace,key,kind,key,payload,false,i+1);expected+=payload.getBytes(StandardCharsets.UTF_8).length;
    assertEquals(payload,mapper.item(workspace,key).payload());
    assertEquals(1,count(session,"SELECT COUNT(*) FROM "+WeaverItemContentSql.table(kind)+" WHERE workspace_id='"+workspace+"' AND item_key='"+key+"'"));
   }
   assertEquals(expected,mapper.itemBytes(workspace));
   mapper.put(workspace,"key0","list:elements","key0",null,true,20);
   assertNull(mapper.item(workspace,"key0").payload());
   assertEquals(0,count(session,"SELECT COUNT(*) FROM WEAVER_MEMO_CONTENT WHERE workspace_id='"+workspace+"' AND item_key='key0'"));
   mapper.put(workspace,"key0","list:elements","key0","{}",false,21);
   assertEquals("{}",mapper.item(workspace,"key0").payload());
   session.commit();
   mapper.put(workspace,"key0","list:elements","key0","\"discard\"",false,22);session.rollback();
   assertEquals("{}",mapper.item(workspace,"key0").payload());
   session.getConnection().createStatement().executeUpdate("DELETE FROM WEAVER_ACCOUNT_WORKSPACE WHERE user_id='"+workspace+"'");
   for(var kind:kinds)assertEquals(0,count(session,"SELECT COUNT(*) FROM "+WeaverItemContentSql.table(kind)+" WHERE workspace_id='"+workspace+"'"));
   session.commit();
  }
 }
}
