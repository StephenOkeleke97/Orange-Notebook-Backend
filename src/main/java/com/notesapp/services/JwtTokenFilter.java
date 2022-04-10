package com.notesapp.services;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.notesapp.model.User;
import com.notesapp.repository.UserRepository;

/**
 * Class that represents a custom Jwt authentication
 * filter.
 * 
 * @author stephen
 */
@Service
public class JwtTokenFilter extends OncePerRequestFilter{

	@Autowired
	private UserRepository userRepository;
	
	@Value("${secret}")
	private String secret;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, 
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException, JWTDecodeException {
		
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		String token = header.split(" ")[1].trim();
		
		String email = "";
		
		try {
			email= JWT.require(Algorithm.HMAC512(secret))
					.build()
					.verify(token)
					.getSubject();
		} catch (Exception e) {
			filterChain.doFilter(request, response);
			return;	
		}
		
		User user = userRepository.findByEmail(email);
		
		if (user == null) {
			filterChain.doFilter(request, response);
			return;	
		}
		
		UserDetails userDetails = new MyUserDetails(user);
		
		UsernamePasswordAuthenticationToken authentication = 
				new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null, 
						userDetails.getAuthorities());
		
		authentication.setDetails(new MyWebAuthenticationDetailsSource().buildDetails(request));
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		filterChain.doFilter(request, response);
		
	}

}
