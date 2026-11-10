package net.dogpoo.api;
import java.nio.file.*;
import java.util.*;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import net.dogpoo.api.common.util.auth.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="DB_VALIDATION_CONFIG",matches=".+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"weaver.scheduling.enabled=false","spring.datasource.hikari.read-only=true","spring.datasource.hikari.maximum-pool-size=1","spring.datasource.hikari.minimum-idle=1"})
class DatabaseApplicationValidationTest {
 @LocalServerPort int port;
 @Autowired JwtTokenUtil tokens;
 @Autowired javax.sql.DataSource dataSource;
 @Autowired net.dogpoo.api.core.user.service.UserService users;
 @DynamicPropertySource static void database(DynamicPropertyRegistry registry)throws Exception {
  var path=Path.of(System.getenv("DB_VALIDATION_CONFIG"));var p=new Properties();
  try(var in=Files.newInputStream(path)){p.loadFromXML(in);}
  String url=p.getProperty("database.url").replaceFirst("(?<=//)[^/]+","127.0.0.1:"+System.getenv("DB_VALIDATION_PORT"));
  if(!url.contains("/DOGPOO_DEV"))throw new IllegalStateException("Development DB required");
  var jwt=new Properties();try(var in=Files.newInputStream(path.resolveSibling("application.properties"))){jwt.load(in);}
  registry.add("spring.datasource.url",()->url);
  registry.add("spring.datasource.username",()->p.getProperty("database.username"));
  registry.add("spring.datasource.password",()->p.getProperty("database.password"));
  registry.add("jwt.secret",()->jwt.getProperty("jwt.secret"));
 }
 @Test void publicDatabaseEndpointsReturnLegacyEnvelope()throws Exception {
  try(var client=HttpClient.newHttpClient()) {
   for(String endpoint:List.of("getConvatarReleaseNotesList","getNoticeModelList")) {
    var req=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/dogpoo/convatar/api/home/"+endpoint)).POST(HttpRequest.BodyPublishers.noBody()).build();
    var response=client.send(req,HttpResponse.BodyHandlers.ofString());
    assertEquals(200,response.statusCode(),endpoint);
    var json=new ObjectMapper().readTree(response.body());
    assertEquals(0,json.path("code").asInt(-1));assertEquals("OK",json.path("msg").asText());assertTrue(json.path("DATA").isArray());
   }
  }
 }
 @Test void originalJwtSecretCanStillIssueAndVerifyTokens() {
  var user=User.withUsername("weaver-synthetic-validation").password("unused").authorities("USER").build();
  String token=tokens.generateToken(user,"synthetic","테스트");
  assertTrue(tokens.validateToken(token,user));assertEquals("synthetic",tokens.getUserKeyFromToken(token));
 }
 @Test void authenticationUsesIsolatedTemporaryUser() throws Exception {
  String username="weaver_validation_user";
  String password="temporary-test-password";
  try {
   try(var connection=dataSource.getConnection()) {
    connection.setReadOnly(false);
    try(var statement=connection.createStatement()) {
     String definition;
     try(var schema=statement.executeQuery("SHOW CREATE TABLE DOGPOO_DEV.GESTURE_FORUM_USER")){assertTrue(schema.next());definition=schema.getString(2);}
     statement.execute(definition.replaceFirst("CREATE TABLE", "CREATE TEMPORARY TABLE"));
    }
    try(var insert=connection.prepareStatement("INSERT INTO GESTURE_FORUM_USER (ccode,userId,passwd,userNm,userUuid,userType) VALUES (1,?,?,?,?,'NORMAL')")) {
     insert.setString(1,username);insert.setString(2,new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password));
     insert.setString(3,"validation");insert.setString(4,"weaver-validation-uuid");insert.executeUpdate();
    }
   }
   assertEquals(0,users.authenticate("GESTURE :: "+username,password));
   assertEquals(301,users.authenticate("GESTURE :: "+username,"wrong-password"));
  } finally {
   try(var connection=dataSource.getConnection()) {
    connection.setReadOnly(false);
    try(var statement=connection.createStatement()){statement.execute("DROP TEMPORARY TABLE IF EXISTS GESTURE_FORUM_USER");}
   }
  }
 }
}
