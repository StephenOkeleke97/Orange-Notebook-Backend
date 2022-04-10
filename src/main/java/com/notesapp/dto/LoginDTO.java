package com.notesapp.dto;

/**
 * A class to map a json object response to
 * a client after a login attempt.
 * 
 * @author stephen
 *
 */
public class LoginDTO {
	/**
	 * Response message
	 */
	private String message;
	/**
	 * Return true if error exists.
	 */
	private boolean error;
	/**
	 * Authentication token.
	 */
	private String token;
	
	public LoginDTO(String message, boolean error, String token) {
		this.message = message;
		this.error = error;
		this.token = token;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public boolean isError() {
		return error;
	}
	
	public void setError(boolean error) {
		this.error = error;
	}
	
	public String getToken() {
		return token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
}
