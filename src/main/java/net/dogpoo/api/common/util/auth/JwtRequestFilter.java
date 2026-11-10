package net.dogpoo.api.common.util.auth;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;

import net.dogpoo.api.core.user.service.UserService;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    public JwtRequestFilter(UserService jwtUserDetailsService, JwtTokenUtil jwtTokenUtil) {
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }


	private static final String AUTHORIZATION = "Authorization";
	private static final String TARGET = "target";
	private static final String BEARER = "Bearer ";

	private final UserService jwtUserDetailsService;
	private final JwtTokenUtil jwtTokenUtil;

	//토큰인증(WebSecurityConfig 에서 사용)
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		final String requestTokenHeader = request.getHeader(AUTHORIZATION);
		final String target = request.getHeader(TARGET);
		String username = null;
		String jwtToken = null;
		// JWT Token 은 Bearer token 이다. Bearer 단어를 지우고 토큰만 가져온다.
		if (requestTokenHeader != null && requestTokenHeader.startsWith(BEARER)) {
			// Bearer 단어를 지우기 위해, substring 사용.
			jwtToken = requestTokenHeader.substring(7);
			try {
				// 유저네임을 토큰으로 부터 가져온다.
				username = jwtTokenUtil.getUsernameFromToken(jwtToken);
			} catch (IllegalArgumentException e) {
				logger.debug("Invalid JWT");
			} catch (io.jsonwebtoken.JwtException e) {
				logger.debug("Invalid or expired JWT");
			}
		} else {
//			if (logger.isWarnEnabled()) {
//				logger.warn("JWT Token does not begin with Bearer String");
//			}
		}
		// 토큰을 받아서 확인 한 뒤, username이 null 인지 확인하고, ApplcationManager가
		// SecurityContextHolder(인메모리)에 등록한 authentication이 있는지 확인한다.
		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			// username이 있다면, jwtUserDetailService를 통해 userDetails 를 불러온다.
			String targetUserNm = target + " :: " + username;
			UserDetails userDetails = this.jwtUserDetailsService.loadUserByUsername(targetUserNm);
			// 만일 토큰이 유효하다면(토큰이 Expired 되지는 않았는지, username이 userDetails의 username과 같은지),
			//스프링 시큐리티를 수동으로 authentication 설정하도록 한다.
			if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
				//인증이 완료 된 토큰
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				usernamePasswordAuthenticationToken
						.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				// 컨텍스트에서 인증을 설정한 후, 현재 사용자가 인증되었는지 지정합니다. 따라서, 스프링 보안 구성을 성공적으로 통과합니다.
				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			}
		}
		chain.doFilter(request, response);
	}
}