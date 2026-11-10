package net.dogpoo.api.common.util.auth;

import java.io.IOException;
import java.io.Serializable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {

	private static final long serialVersionUID = -3047313055231777625L;

	// AuthenticationEntryPoint : 인증된 사용자가 SecurityContext에 존재하지도 않고, 어떠한 인증되지 않은
	// 익명의 사용자가 보호된 리소스에 접근하였을 때, 수행되는 EntryPoint 핸들러.
	// WebSecurityConfig 에서 사용
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
	}
}