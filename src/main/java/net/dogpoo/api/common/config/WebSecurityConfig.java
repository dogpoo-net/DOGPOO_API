package net.dogpoo.api.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import net.dogpoo.api.common.util.auth.JwtAuthenticationEntryPoint;
import net.dogpoo.api.common.util.auth.JwtRequestFilter;

@Configuration
@EnableWebSecurity
@EnableAsync
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {
	
	// 허용할 주소 목록
	private static final String KEY_NM_JWT_PUBLIC_URL = "${jwt.public.url}";
	
	@Autowired UserDetailsService jwtUserDetailsService;
	@Autowired JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	@Autowired JwtRequestFilter jwtRequestFilter;
	
	@Value(KEY_NM_JWT_PUBLIC_URL)
	private String jwtPublicURL;
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.addAllowedOrigin("http://localhost:3000");
		configuration.addAllowedOrigin("https://dogpoo.net");
		configuration.addAllowedOrigin("https://www.dogpoo.net");
		configuration.addAllowedOrigin("https://gesture.dogpoo.net");
		configuration.addAllowedOrigin("https://convatar.dogpoo.net");
		configuration.addAllowedOrigin("https://batgers.dogpoo.net");
		configuration.addAllowedMethod("*");
		configuration.addAllowedHeader("*");
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
	
 @Bean
 public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  http.csrf(c -> c.disable()).cors(c -> {})
   .authorizeHttpRequests(a -> a.requestMatchers(java.util.Arrays.stream(jwtPublicURL.split(",")).map(path -> (org.springframework.security.web.util.matcher.RequestMatcher) request -> new org.springframework.util.AntPathMatcher().match(path, request.getRequestURI().substring(request.getContextPath().length()))).toArray(org.springframework.security.web.util.matcher.RequestMatcher[]::new)).permitAll().anyRequest().authenticated())
   .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
   .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

 @Bean
 public AuthenticationManager authenticationManagerBean() {
  DaoAuthenticationProvider provider = new DaoAuthenticationProvider(jwtUserDetailsService);
  provider.setPasswordEncoder(passwordEncoder());
  return new ProviderManager(provider);
 }
 @Bean
 public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtRequestFilter> jwtFilterRegistration(JwtRequestFilter filter) {
  var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
  registration.setEnabled(false);
  return registration;
 }
}
