package com.notesapp.services;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * A class that enables the retrieval of information
 * from a request. We use the to retrieve a verification
 * code parameter in the request.
 * 
 * @author stephen
 *
 */
public class MyWebAuthenticationDetails extends WebAuthenticationDetails{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * MFA verification code.
	 */
	private String verificationCode;
	
	/**
	 * Creates an instance of the class.
	 * @param request {@link HttpServletRequest}
	 */
	public MyWebAuthenticationDetails(HttpServletRequest request) {
		super(request);
		verificationCode = request.getParameter("code");
	}
	
	/**
	 * Get verification code.
	 * 
	 * @return code
	 */
	public String getVerificationCode() {
		return verificationCode;
	}
}
