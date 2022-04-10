package com.notesapp.dto;

/**
 * A class to map a json object response to
 * a client. 
 * 
 * @author stephen
 *
 */
public class ResponseDTO {
	/**
	 * Response message.
	 */
	private String message;
	
	/**
	 * Return true if response
	 * resulted in an error.
	 */
	private boolean error;
	
	public ResponseDTO() {
		this.error = true;
	}
	
	public ResponseDTO(String message, boolean error) {
		this.message = message;
		this.error = error;
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
	
	
}
