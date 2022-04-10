package com.notesapp.storage;

/**
 * A class to handle not found exceptions.
 * 
 * @author stephen
 *
 */
public class StorageFileNotFoundException extends StorageException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4139581816717667750L;

	public StorageFileNotFoundException(String message) {
		super(message);
	}

	public StorageFileNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
