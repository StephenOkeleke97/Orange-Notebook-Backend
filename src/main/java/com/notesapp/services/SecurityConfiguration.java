package com.notesapp.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notesapp.dto.LoginDTO;
import com.notesapp.dto.ResponseDTO;

/**
 * A class that represents the security configuration
 * of the application.
 * 
 * @author stephen
 *
 */
@Configuration
@EnableWebSecurity     
public class SecurityConfiguration extends WebSecurityConfigurerAdapter{
		
	@Autowired
	UserDetailsService userDetailsService;
	
	@Autowired
	BasicAuthenticationEntryPoint authenticationEntryPoint;
	
	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	@Autowired
	MyWebAuthenticationDetailsSource authenticationDetailsSource;
	
	@Autowired
	JwtTokenFilter jwtTokenFilter;
	
	@Value("${secret}")
	String secret;

	@Override
    protected void configure(HttpSecurity http) throws Exception {
        http
        .cors().and().csrf().disable();
        
        http.sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        http.authorizeRequests()
        .mvcMatchers("/api/register").permitAll()
        .mvcMatchers("/api/mfaenabled").permitAll()
        .mvcMatchers("/api/sendtoken").permitAll()
        .mvcMatchers("/api/enableaccount").permitAll()
        .mvcMatchers("/api/resetpassword").permitAll()
        .mvcMatchers("/api/requesttoken").permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .formLogin()
        .authenticationDetailsSource(authenticationDetailsSource)
        .loginProcessingUrl("/login")
        .usernameParameter("email")
        .passwordParameter("password")
        .successHandler(new loginSuccessHandler())
        .failureHandler(new loginFailureHandler())
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(authenticationEntryPoint);
        
        http.addFilterBefore(jwtTokenFilter, 
        		UsernamePasswordAuthenticationFilter.class);
    }
	
	/**
	 * Get custom authentication provider.
	 * 
	 * @return authentication provider
	 */
	@Bean
	public DaoAuthenticationProvider authProvider() {
		MyAuthenticationProvider authProvider = new MyAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder);
		return authProvider;
	}
	
	/**
	 * Cors configuration.
	 * 
	 * @return a cors filter
	 */
	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = 
				new UrlBasedCorsConfigurationSource();
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowCredentials(true);
		cors.setAllowedOrigins(Arrays.asList("*"));
		cors.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", 
				"DELETE", "OPTIONS", "PATCH", "HEAD", "CONNECT"));
		source.registerCorsConfiguration("/**", cors);
		return new CorsFilter(source);
	}

	/**
	 * A class that represents a handler for a 
	 * successful authentication.
	 * 
	 * @author stephen
	 *
	 */
	private class loginSuccessHandler implements AuthenticationSuccessHandler {

		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
				Authentication authentication) throws IOException, ServletException {
			response.setStatus(200);
			String jwt = JWT.create().withSubject(authentication.getName())
					.withIssuedAt(new Date())
					.sign(Algorithm.HMAC512(secret));
			LoginDTO result = new LoginDTO("Success", false, jwt);
			objectMapper.writeValue(response.getWriter(), result);
		}
		
	}
	
	/**
	 * A class that represents a handler for a 
	 * failed authentication.
	 * 
	 * @author stephen
	 *
	 */
	private class loginFailureHandler implements AuthenticationFailureHandler {

		@Override
		public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
				AuthenticationException exception) throws IOException, ServletException {
			String message = exception.getMessage();
			if (message.toLowerCase().equals("bad credentials"))
				message = "Invalid Username or Password";
			ResponseDTO responseDTO = new ResponseDTO(message, true);
			response.setStatus(401);
			objectMapper.writeValue(response.getWriter(), responseDTO);
		}
		
	}

}
