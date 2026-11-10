package net.dogpoo.api;

import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class MapperSecurityTest {
 static Configuration configuration;
 static final String FORUM="net.dogpoo.api.core.gesture.mapper.GestureForumThreadPostMapper.";
 @BeforeAll static void loadActualMappers() throws Exception {
  var factory=new SqlSessionFactoryBean();
  factory.setDataSource(new DriverManagerDataSource("jdbc:mariadb://localhost/unused"));
  var resolver=new PathMatchingResourcePatternResolver();
  factory.setConfigLocation(resolver.getResource("classpath:mybatis-config.xml"));
  factory.setMapperLocations(resolver.getResources("classpath*:net/dogpoo/api/core/**/mapper/*Mapper.xml"));
  configuration=factory.getObject().getConfiguration();
 }
 @Test void searchAndPaginationAreParameters() {
  var params=new HashMap<String,Object>();
  params.put("searchKeyword","' OR 1=1 --"); params.put("orderTarget","threadNo; DROP TABLE users");
  params.put("pageTarget","0 UNION SELECT 1");
  var bound=configuration.getMappedStatement(FORUM+"getGestureForumThreadListSearch").getBoundSql(params);
  assertFalse(bound.getSql().contains("OR 1=1"));
  assertFalse(bound.getSql().contains("DROP TABLE"));
  assertTrue(bound.getSql().contains("CONCAT('%', ?, '%')"));
  assertTrue(bound.getSql().contains("a.openDate"));
  assertTrue(bound.getParameterMappings().stream().anyMatch(p->p.getProperty().equals("searchKeyword")));
  assertTrue(bound.getSql().contains("LIMIT ?, 20"));
 }
 @Test void allowedOrderingRemainsAvailable() {
  for(String column:List.of("threadNo","threadTitle","threadViews","threadLikeCnt","threadOwner","openDate","answerCnt","threadOpenDate")) {
   var sql=configuration.getMappedStatement(FORUM+"getGestureForumThreadWholeList").getBoundSql(Map.of("orderTarget",column)).getSql().replaceAll("\\s+"," ");
   assertTrue(sql.contains("ORDER BY isNotice DESC, "+column+" DESC"),sql);
  }
 }
 @Test void countersUseClosedSetsAndBoundDeltas() {
  for(String column:List.of("likeCnt","dislikeCnt")) for(String delta:List.of("+ 1","- 1")) {
   var bound=configuration.getMappedStatement(FORUM+"updateForumThreadPostLikeCnt").getBoundSql(Map.of("target",column,"cnt",delta,"postNo",1,"threadNo",2));
   assertTrue(bound.getSql().contains(column+" = "+column));
   assertEquals(delta.equals("+ 1")?1:-1,bound.getAdditionalParameter("delta"));
  }
  assertThrows(Exception.class,()->configuration.getMappedStatement(FORUM+"updateForumThreadPostLikeCnt").getBoundSql(Map.of("target","likeCnt = 0 --","cnt","+ 1")));
  assertThrows(Exception.class,()->configuration.getMappedStatement(FORUM+"updateForumThreadLikeCnt").getBoundSql(Map.of("cnt","+ 1; DROP TABLE x")));
 }
 @Test void annotationPaginationKeepsLegacyArithmetic() {
  var bound=configuration.getMappedStatement("net.dogpoo.api.core.batgers.mapper.BatgersGameHistoryMapper.getGameHistoryList").getBoundSql(Map.of("page",8));
  assertEquals(16,bound.getAdditionalParameter("limit"));
  assertTrue(bound.getSql().endsWith("LIMIT ?, ?"));
 }
 @Test void mapperSourcesCannotReintroduceRawSubstitution() throws Exception {
  try(var paths=Files.walk(Path.of("src/main"))) {
   for(var path:paths.filter(Files::isRegularFile).filter(p->p.toString().contains("/mapper/")).toList())
    assertFalse(Files.readString(path).contains("${"),path.toString());
  }
 }
}
