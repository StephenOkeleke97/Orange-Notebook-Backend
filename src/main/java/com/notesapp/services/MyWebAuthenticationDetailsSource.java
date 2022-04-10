package com.notesapp.services;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * A class that represents a custom WebAuthenticationDetailsSource.
 * 
 * @author stephen
 *
 */
@Component
public class MyWebAuthenticationDetailsSource implements 
AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails>{

	@Override
	public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
		return new MyWebAuthenticationDetails(context);
	}

}
