package com.notesapp.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.notesapp.model.Salt;
import com.notesapp.repository.SaltRepository;

/**
 * A class that represents a custom password
 * encoder. It uses a salt retrieved from
 * the database to hash a password.
 * 
 * @author stephen
 */
@Service
public class MyPasswordEncoder implements PasswordEncoder{
	
	@Autowired
	SaltRepository saltRepository;
	
	@Override
	public String encode(CharSequence rawPassword) {
		byte[] salt = getSalt();
		StringBuilder builder = new StringBuilder();
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			byte[] bytes = md.digest(rawPassword.toString().getBytes());
			
			for (int i = 0; i < bytes.length; i++) {
				builder.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return builder.toString();
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		String hashed = encode(rawPassword);
		return hashed.equals(encodedPassword);
	}
	
	/**
	 * Get default salt from the database.
	 * 
	 * @return salt
	 */
	private byte[] getSalt() {
		Salt salt = saltRepository.findBySaltName("Default");
		return salt.getSalt();
	}
}
