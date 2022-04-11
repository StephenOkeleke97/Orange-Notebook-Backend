package com.notesapp.controller;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Date;
import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.xml.bind.DatatypeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notesapp.dto.BackUpInfoDTO;
import com.notesapp.dto.ResponseDTO;
import com.notesapp.model.User;
import com.notesapp.repository.UserRepository;
import com.notesapp.services.SMTPMailSender;
import com.notesapp.storage.StorageFileNotFoundException;
import com.notesapp.storage.StorageService;

/**
 * Controller for handling ajax
 * requests to this application.
 * 
 * @author stephen
 *
 */
@RestController
@RequestMapping("api/")
public class Controller {
	
	private final StorageService storageService;
		
	@Autowired
	private SMTPMailSender mailSender;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	public Controller(StorageService storageService) {
		this.storageService = storageService;
	}
	
	@PersistenceContext
	EntityManager entityManager;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	/**
	 * Get file from file system.
	 * 
	 * @param user authenticated principal
	 * @return response entity with file in body
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	@GetMapping("restore")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(Principal user) 
			throws IOException, NoSuchAlgorithmException {
		
		User u = userRepository.findByEmail(user.getName());
		String filename = u.getBackupName();
		
		if (filename != null) {
			Resource file = storageService.loadAsResource(filename);
			
			byte[] bytes = file.getInputStream().readAllBytes();
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			byte[] digest = messageDigest.digest(bytes);
			String hash = DatatypeConverter.printHexBinary(digest);
			
			String backupInfo = objectMapper
					.writeValueAsString(new BackUpInfoDTO(u.getLastBackUpDate(), u.getLastBackUpSize()));
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
			file.getFilename() + "\"")
					.header("Checksum", hash)
					.header("Info", backupInfo)
					.body(file);
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	/**
	 * Upload file to application file system.
	 * 
	 * @param file file to be uploaded
	 * @param user authenticated principal
	 * @param response {@link HttpServletRequest}
	 * @return a response dto
	 */
	@PostMapping("backup")
	public @ResponseBody ResponseDTO handleFileUpload(@RequestParam("file") MultipartFile file, 
			Principal user, HttpServletResponse response) {
		if (file != null) {
			storageService.store(file);
			
			User u = userRepository.findByEmail(user.getName());
			u.setBackupName(file.getOriginalFilename().toString());
			u.setLastBackUpDate(new Date());
			u.setLastBackUpSize(file.getSize());
			userRepository.save(u);
			
			String message = "Success";
			return new ResponseDTO(message, false);
		} else {
			String message = "File cannot be empty";
			response.setStatus(400);
			return new ResponseDTO(message, true);
		}
	}
	
	/**
	 * Delete file from application file system.
	 * 
	 * @param user authenticated principal
	 * @param response {@link HttpServletResponse}
	 * @return a response data transfer object
	 */
	@DeleteMapping("deletebackup")
	public @ResponseBody ResponseDTO deleteBackup(Principal user,
			HttpServletResponse response) {
		User u = userRepository.findByEmail(user.getName());
		String filename = u.getBackupName();
		
		if (filename != null) {
			storageService.delete(filename);
			
			u.setLastBackUpDate(null);
			u.setLastBackUpSize(null);
			u.setBackupName(null);
			userRepository.save(u);
			String message = "Success";
			return new ResponseDTO(message, false);
		} else {
			response.setStatus(404);
			return null;
		}
	}
	
	/**
	 * Handle a file not found exception.
	 * 
	 * @param exc exception
	 * @return response entity
	 */
	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}
	
	/**
	 * Register user. Checks if email is valid 
	 * before registration. An email is valid if there is no
	 * active user with the email. If an inactive user exists,
	 * the user is deleted from the database, and a new user
	 * is created with the email.
	 * 
	 * @param email user email
	 * @param password user password
	 * @param response {@link HttpServletResponse}
	 * @return response dto
	 */
	@PostMapping("register")
	public @ResponseBody ResponseDTO addNewUser(@RequestParam String email, 
			@RequestParam String password,
			HttpServletResponse response) {
		String message;
		if (!verifyThatEmailIsAvailable(email)) {
			response.setStatus(400);
			message = "There is an account associated with this email.";
			return new ResponseDTO(message, true);
		}
		
		User user = new User();
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(password));
		user.setOneTimePassword();
		userRepository.save(user);			
		sendEmail(email, user.getOneTimePassword());
		
		message = "Success";
		return new ResponseDTO(message, false);
	}
	
	/**
	 * Generated new code and send email to user.
	 * 
	 * @param email user email
	 * @param response {@link HttpServletResponse}
	 */
	@PostMapping("requesttoken")
	public void requestOTP(@RequestParam String email,
			HttpServletResponse response) {
		User user = userRepository.findByEmail(email);
		
		if (user == null) {
			response.setStatus(400);
			return;
		}
		
		user.setOneTimePassword();
		userRepository.save(user);
		sendEmail(email, user.getOneTimePassword());
	}

	/**
	 * Enable user account. When a user is registered,
	 * the account is disabled by default.
	 * A valid verification code is required to enable the
	 * user account.
	 * 
	 * @param email user email
	 * @param code verification code
	 * @param response {@link HttpServletResponse}
	 * @return response dto
	 */
	@PostMapping("enableaccount")
	public @ResponseBody ResponseDTO verify(@RequestParam String email, 
			@RequestParam int code,
			HttpServletResponse response) {
		String message;
		ResponseDTO result = new ResponseDTO();
		
		User user = userRepository.findByEmail(email);
		
		if (user == null) {
			message = "Invalid Email";
			result.setMessage(message);
			response.setStatus(400);
			return result;
		}
		
		if (user.getOneTimePassword() != code) {
			message = "Invalid Verification Code";
			result.setMessage(message);
			response.setStatus(401);
			return result;
		}
		
		if (!user.checkValidVerificationCode()) {
			message = "Verification Code is Expired";
			response.setStatus(401);
			result.setMessage(message);
			return result;
		}
		
		user.setEnabled(true);
		userRepository.save(user);
		message = "Success";
		result.setMessage(message);
		result.setError(false);
		return result;		
	}
	
	/**
	 * Delete user account and all backups
	 * associated with the user.
	 * 
	 * @param user user email
	 * @return response dto
	 */
	@DeleteMapping("deleteaccount")
	public @ResponseBody ResponseDTO deleteUser(Principal user) {
		User u = userRepository.findByEmail(user.getName());
		String filename = u.getBackupName();
		if (filename != null) storageService.delete(filename);
		userRepository.delete(u);
		return new ResponseDTO("Account Deleted", false);
	}
	
	/**
	 * Check if MultiFactor Authentication is
	 * enabled. If enabled, send a code to the
	 * email provided.
	 * 
	 * @param email account to be checked
	 * @param response {@link HttpServletResponse} response
	 * @return true if mfa enabled or false otherwise.
	 */
	@PostMapping("mfaenabled")
	public Boolean twoFactorEnabled(@RequestParam String email,
			HttpServletResponse response) {
		User u = userRepository.findByEmail(email);
		
		if (u == null || !u.isEnabled()) {
			response.setStatus(400);
			return null;
		}
		
		if (u.getTwoFactorAuthentication()) {
			u.setOneTimePassword();
			userRepository.save(u);
			sendEmail(email, u.getOneTimePassword());
		}
		
		return u.getTwoFactorAuthentication();
	}
	
	/**
	 * Enable or disable two factor (or multi factor) 
	 * authentication.
	 * 
	 * @param enabled true to enable or false to disable
	 * @param user authenticated principal
	 * @param response {@link HttpServletResponse}
	 * @return response dto
	 */
	@PostMapping("enablemfa")
	public @ResponseBody ResponseDTO enableTwoFactor(@RequestParam boolean enabled,
			Principal user, 
			HttpServletResponse response) {
		
		User u = userRepository.findByEmail(user.getName());
		u.setTwoFactorAuthentication(enabled);
		userRepository.save(u);
		return new ResponseDTO("Success", false);
	}
	
	/**
	 * Reset user password. A valid verification
	 * code is required to complete this
	 * action.
	 * 
	 * @param email user email
	 * @param password new password
	 * @param code verification code
	 * @param response {@link HttpServletResponse}
	 * @return response dto
	 */
	@PutMapping("resetpassword")
	public @ResponseBody ResponseDTO resetPassword(@RequestParam String email,
			@RequestParam String password,
			@RequestParam int code,
			HttpServletResponse response) {
		User user = userRepository.findByEmail(email);
		
		
		if (user == null || !user.isEnabled()) {
			response.setStatus(401);
			return new ResponseDTO("Unauthorized", true);
		}
		
		if (user.getOneTimePassword() != code) {
			response.setStatus(401);
			return new ResponseDTO("Invalid Verification Code", true);
		}
		
		if (!user.checkValidVerificationCode()) {
			response.setStatus(401);
			return new ResponseDTO("The Verification Code is Expired", true);
		}
		
		user.setPassword(passwordEncoder.encode(password));
		userRepository.save(user);
		return new ResponseDTO("Success", false);
	}
	
	/**
	 * Get information about a user's backup.
	 * 
	 * @param user authenticated principal
	 * @return BackUpInfoDTO instance
	 */
	@GetMapping("getbackupinfo")
	public BackUpInfoDTO getLastBackUpDateAndSize(Principal user) {
		User u = userRepository.findByEmail(user.getName());
		return new BackUpInfoDTO(u.getLastBackUpDate(), u.getLastBackUpSize());
	}
	
	/**
	 * Verify that an email is available.
	 * An email is available if it has no active user 
	 * associated with it. If a user with the email
	 * is inactive, that user is deleted.
	 * 
	 * @param email email to be verified
	 * @return true if valid or false otherwise
	 */
	@Transactional
	private boolean verifyThatEmailIsAvailable(String email) {
		User user = userRepository.findByEmail(email);
		if (user == null) return true;
		if (!user.isEnabled()) {
			userRepository.delete(user);
			
			entityManager.flush();
			return true;
		}
		return false;
	}
	
	/**
	 * Send email and verification code
	 * to provided email address.
	 * 
	 * @param email recipient
	 * @param code code
	 */
	private void sendEmail(String email, int code) {
		try {
			mailSender.send(email, "Verification Code", code);
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
}
