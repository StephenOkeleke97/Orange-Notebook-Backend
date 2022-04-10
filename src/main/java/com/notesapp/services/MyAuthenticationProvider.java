package com.notesapp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.notesapp.model.User;
import com.notesapp.repository.UserRepository;

/**
 * A class that represents a custom authentication
 * provider.
 * 
 * @author stephen
 */
public class MyAuthenticationProvider extends DaoAuthenticationProvider {
	@Autowired
	private UserRepository userRepository;
	
	@Override
	public Authentication authenticate(Authentication auth) throws AuthenticationException{
		User user = userRepository.findByEmail(auth.getName());
		
		if (user == null) throw new BadCredentialsException("Invalid Username or Password");
		
		if (user.getTwoFactorAuthentication()) {
			String code = ((MyWebAuthenticationDetails) auth
					.getDetails()).getVerificationCode();
			if (!validateCode(code)) throw new BadCredentialsException("Invalid Verification Code");
			
			final int verificationCode = Integer.parseInt(((MyWebAuthenticationDetails) auth
					.getDetails()).getVerificationCode());
			int validCode = user.getOneTimePassword();
			
			if (verificationCode != validCode) 
				throw new BadCredentialsException("Invalid Verification Code");
			
			if (!user.checkValidVerificationCode())
				throw new BadCredentialsException("Verification Code is Expired");
			
		}
		
		final Authentication result = super.authenticate(auth);
		return new UsernamePasswordAuthenticationToken(auth.getName(), 
				result.getCredentials(), result.getAuthorities());
	}
	
	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
	
	/**
	 * Check if code is a valid integer.
	 * 
	 * @param code code to be checked
	 * @return true if code is valid or false otherwise
	 */
	private boolean validateCode(String code) {
		try {
			Integer.parseInt(code);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
