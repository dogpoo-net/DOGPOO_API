package net.dogpoo.api;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
@SpringBootApplication
@MapperScan("net.dogpoo.api.core.*.mapper")
public class WeaverApplication {
 public static void main(String[] args) { SpringApplication.run(WeaverApplication.class, args); }
}
