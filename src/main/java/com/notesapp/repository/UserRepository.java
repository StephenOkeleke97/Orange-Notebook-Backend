package com.notesapp.repository;

import org.springframework.data.repository.CrudRepository;

import com.notesapp.model.User;

/**
 * Repository for querying the user table.
 * 
 * @author stephen
 *
 */
public interface UserRepository extends CrudRepository<User, Long>{
	User findByEmail(String email);
}
