package net.dogpoo.api.core.weaver.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import net.dogpoo.api.core.weaver.service.WeaverOAuthService;
@RestController
@RequestMapping("/weaver/api/auth/{provider:naver|github|gitlab}")
public class WeaverOAuthCallbackController {
 private final WeaverOAuthService service;
 public WeaverOAuthCallbackController(WeaverOAuthService service){this.service=service;}
 @GetMapping(value="/callback",produces="text/html;charset=UTF-8") public ResponseEntity<String> callback(
  @PathVariable String provider,@RequestParam(required=false) String code,@RequestParam(required=false) String state,@RequestParam(required=false) String error){
  String message="인증이 완료되었습니다. 원래 Weaver 앱이나 탭으로 돌아가주세요.";int status=200;
  try{service.callback(provider,code,state,error);}catch(ResponseStatusException e){message="로그인이 취소되었거나 만료되었습니다. Weaver에서 다시 시도해주세요.";status=e.getStatusCode().value();}
  return ResponseEntity.status(status).header("Cache-Control","no-store").header("Referrer-Policy","no-referrer").header("Content-Security-Policy","default-src 'none'; frame-ancestors 'none'")
   .body("<!doctype html><html lang=ko><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>WEAVER 로그인</title><body><h1>WEAVER</h1><p>"+message+"</p></body></html>");
 }
}
