package net.dogpoo.api;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import net.dogpoo.api.common.util.http.HttpClientUtil;
import static org.junit.jupiter.api.Assertions.*;
class HttpUtilityTest {
 @Test void postForwardsHeadersAndPreservesErrorBody() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",exchange->{
   String request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
   byte[] body=(exchange.getRequestHeaders().getFirst("X-Test")+":"+request).getBytes(StandardCharsets.UTF_8);
   exchange.sendResponseHeaders(422,body.length);
   try(var out=exchange.getResponseBody()){out.write(body);}
  });server.start();
  try {
   var payload=new HashMap<String,Object>();payload.put("message","한글");
   var headers=new HashMap<String,String>();headers.put("X-Test","forwarded");
   assertEquals("forwarded:{\"message\":\"한글\"}",HttpClientUtil.post("http://127.0.0.1:"+server.getAddress().getPort()+"/",payload,headers));
  } finally {server.stop(0);}
 }
}
