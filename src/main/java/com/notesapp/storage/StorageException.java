package com.notesapp.storage;

/**
 * A class to handle exceptions during storage
 * operations.
 * 
 * @author stephen
 *
 */
public class StorageException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1811648476759778512L;

	public StorageException(String message) {
		super(message);
	}

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
