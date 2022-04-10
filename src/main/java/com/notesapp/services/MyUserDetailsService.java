package com.notesapp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.notesapp.model.User;
import com.notesapp.repository.UserRepository;

/**
 * A class that represents a custom user details service.
 * 
 * @author stephen
 */
@Service
public class MyUserDetailsService implements UserDetailsService {

	@Autowired
	UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(username);
		
		if (user == null) throw new BadCredentialsException("Invalid Username or Password");
			
		return new MyUserDetails(user);
	}

}
