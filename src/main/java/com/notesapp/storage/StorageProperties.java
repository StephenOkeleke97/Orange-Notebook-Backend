package com.notesapp.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A class for configuring storage properties.
 * 
 * @author stephen
 *
 */
@ConfigurationProperties("storage")
public class StorageProperties {

	/**
	 * Folder location for storing files
	 */
	private String location = "upload-dir";

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
