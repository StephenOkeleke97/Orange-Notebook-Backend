package com.notesapp.repository;

import org.springframework.data.repository.CrudRepository;

import com.notesapp.model.Salt;

/**
 * Repository for querying salt from database.
 * 
 * @author stephen
 *
 */
public interface SaltRepository extends CrudRepository<Salt, Long>{
	Salt findBySaltName(String saltName);
}
