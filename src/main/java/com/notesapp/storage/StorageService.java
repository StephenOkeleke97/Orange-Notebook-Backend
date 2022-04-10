package com.notesapp.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Interface that provides a contract
 * for managing the storage service.
 * 
 * @author stephen
 *
 */
public interface StorageService {
	/**
	 * Initialize root directory.
	 */
	void init();
	
	/**
	 * Store file in file system.
	 * 
	 * @param file to be stored
	 */
	void store(MultipartFile file);
	
	/**
	 * Get all files.
	 * 
	 * @return stream of path to files
	 */
	Stream<Path> loadAll();
	
	/**
	 * Load path to file.
	 * 
	 * @param filename name of file
	 * @return path to file
	 */
	Path load(String filename);
	
	/**
	 * Load file as resource.
	 * 
	 * @param filename file name to be loaded
	 * @return file as resource
	 */
	Resource loadAsResource(String filename);
	
	/**
	 * Delete all files.
	 */
	void deleteAll();
	
	/**
	 * Delete specific file.
	 * 
	 * @param filename
	 */
	void delete(String filename);

}
