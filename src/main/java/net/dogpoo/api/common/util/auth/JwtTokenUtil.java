package net.dogpoo.api.common.util.auth;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtTokenUtil implements Serializable {

	private static final String KEY_NM_NICK_NAME = "nickName";
	private static final String KEY_NM_JWT_SECRET = "${jwt.secret}";
	private static final long serialVersionUID = -3783048998965918392L;

	@Value(KEY_NM_JWT_SECRET)
	private String JWT_SECRET_KEY;

	// 토큰 유효시간 1시간 (ms 단위)
	public static final long JWT_TOKEN_VALIDITY = 86400000;

	private static final String BEARER = "Bearer ";
	public final String JWT_KEY_PARSING_ERROR_RESULT = "402";

	// 토큰에서 username 을 가져온다.
	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}
	
	// 토큰에서 userKey 을 가져온다.
	public String getUserKeyFromToken(String token) {
		return getClaimFromToken(token, Claims::getId);
	}

	// 토큰에서 expiration 을 가져온다.
	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	// Claim에 종류에 따라서 Claim 을 토큰에서 가져온다.
	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	// jwt를 파싱하는데, 시크릿키를 사용하여 파싱한다.
	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser().setSigningKey(JWT_SECRET_KEY).parseClaimsJws(token).getBody();
	}

	// 토큰이 만료되었는지 확인한다.
	private Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}

	// 토큰을 생성한다.
	public String generateToken(UserDetails userDetails, String userKey, String nickName) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(KEY_NM_NICK_NAME, nickName);
		return doGenerateToken(claims, userDetails.getUsername(), userKey);
	}

	// 토큰을 생성한다. 
	// 1. 발행자, 만료, 대상 및 ID와 같은 토큰의 클레임을 정의합니다
	// 2. HS512 알고리즘과 비밀키를 이용하여 JWT에 서명합니다.
	// 3. JWS Compact Serialization
	// (https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-3.1)에
	// 따라 secret 키와 함께 JWT를 URL-안전 문자열로 압축한다.
	private String doGenerateToken(Map<String, Object> claims, String subject, String userKey) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setId(userKey).setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
				.signWith(SignatureAlgorithm.HS512, JWT_SECRET_KEY).compact();
	}

	// 토큰의 유효성을 검사한다. (토큰이 Expired 되지는 않았는지, username이 userDetails의 username과 같은지)
	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = getUsernameFromToken(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}
	
	// 헤더의 Authoriztion 을 받아서
	public String getUserKeyFromJwtToken(String authorization) {
		if (authorization != null && authorization.startsWith(BEARER)) {
			String jwtToken = authorization.substring(7);
			String userKey = "";
			try {
				userKey = getUserKeyFromToken(jwtToken);
			} catch (ExpiredJwtException | MalformedJwtException e) {
				e.printStackTrace();
				userKey = "EXPIRED_KEY";
			}
			return userKey;
		} else {
			return JWT_KEY_PARSING_ERROR_RESULT;
		}
	}
	
	// 헤더의 Authoriztion 을 받아서
	public String getUserIDFromJwtToken(String authorization) {
		if (authorization != null && authorization.startsWith(BEARER)) {
			String jwtToken = authorization.substring(7);
			String userID = getUsernameFromToken(jwtToken);
			return userID;
		} else {
			return JWT_KEY_PARSING_ERROR_RESULT;
		}
	}
}