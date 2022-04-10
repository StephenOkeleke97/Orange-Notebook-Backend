package com.notesapp.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.InputStream;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.notesapp.model.Salt;
import com.notesapp.model.User;
import com.notesapp.repository.SaltRepository;
import com.notesapp.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@Transactional
@ActiveProfiles("test")
class ApplicationTests {
	
	@Autowired
	MockMvc mockMvc;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private SaltRepository saltRepository;
	
	@Autowired
	PasswordEncoder passwordEncoder;
		
	@Test
	void contextLoads() {
	}
	
	@RegisterExtension
	private static GreenMailExtension greenMail = 
			new GreenMailExtension(ServerSetupTest.SMTP)
			.withConfiguration(GreenMailConfiguration.aConfig()
					.withUser("test", "test123"))
			.withPerMethodLifecycle(true);
	
	@Test
	public void testSaveUser() {
		User u = new User("test@yahoo.com", "Test123$");
		userRepository.save(u);
		Assertions.assertThat(u.getUserId()).isGreaterThan(0);
	}
	
	@Test
	public void testGetUserAttribute() {
		User u = new User("test@yahoo.com", "Test123$");
		userRepository.save(u);
		Assertions.assertThat(u.getPassword()).isEqualTo("Test123$");
	}
	
	@Test
	public void testSetUserAttribute() {
		User u = new User("test@yahoo.com", "Test123$");
		userRepository.save(u);
		u.setPassword("Test234$");
		userRepository.save(u);
		Assertions.assertThat(u.getPassword()).isEqualTo("Test234$");
	}
	
	@Test
	public void testDeleteUser() {
		User u = new User("test@yahoo.com", "Test123$");
		userRepository.save(u);
		Assertions.assertThat(u.getUserId()).isGreaterThan(0);
		userRepository.delete(u);
		Assertions.assertThat(userRepository.findByEmail("test@yahoo.com")).isEqualTo(null);
	}
	
	@Test
	public void testEmailUniqueConstraintEnforced() {
		assertThrows(DataIntegrityViolationException.class, () -> {
			User u = new User("test@yahoo.com", "Test123$");
			User u2 = new User("test@yahoo.com", "Test123$");
			userRepository.save(u);
			userRepository.save(u2);
		});
	}
	
	@Test
	public void testEmailCannotBeNull() {
		assertThrows(DataIntegrityViolationException.class, () -> {
			User u = new User();
			u.setPassword("Test");
			userRepository.save(u);
		});
	}
	
	@Test
	public void testPasswordCannotBeNull() {
		assertThrows(DataIntegrityViolationException.class, () -> {
			User u = new User();
			u.setEmail("test@yahoo.com");
			userRepository.save(u);
		});
	}
	
	@Test
	public void testProtectedEndPointReturnsUnauthorizedResponse() throws Exception {
		mockMvc.perform(get("/api"))
		.andDo(print())
		.andExpect(status().isUnauthorized());
	}
	
	@Test
	@WithMockUser(username = "test@yahoo.com")
	public void testUploadRestoreBackUp() throws Exception {
		createUser();
		
		String fileName = "backup.db";
		
		Resource resource = new ClassPathResource("backup.db");
		
		InputStream is = resource.getInputStream();
		MockMultipartFile backup = new MockMultipartFile("file", fileName, "db", is);
		
		MockMultipartHttpServletRequestBuilder multipartRequest = 
				MockMvcRequestBuilders.multipart("/api/backup");
		
		mockMvc.perform(multipartRequest.file(backup))
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"));
		
		User u = userRepository.findByEmail("test@yahoo.com");
		Assertions.assertThat(u.getBackupName()).isNotNull();
		Assertions.assertThat(u.getLastBackUpDate()).isNotNull();
		Assertions.assertThat(u.getLastBackUpSize()).isNotNull();
		
		//Test Restore
		mockMvc.perform(get("/api/restore"))
		.andDo(print()).andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.header().exists("Checksum"))
		.andExpect(MockMvcResultMatchers.header()
				.string("Content-Disposition", containsString("backup.db")));		
	}
	
	@Test
	@WithMockUser(username = "test@yahoo.com")
	public void testRestoreBackUpWhenNoBackupReturnsNotFound() throws Exception {
		createUser();
		
		mockMvc.perform(get("/api/restore"))
		.andDo(print()).andExpect(status().isNotFound());
	}
	
	
	@Test
	@WithMockUser(username = "test@yahoo.com")
	public void testDeleteBackUp() throws Exception {
		createUser();
		String fileName = "backup.db";
		
		Resource resource = new ClassPathResource("backup.db");
		
		InputStream is = resource.getInputStream();
		MockMultipartFile backup = new MockMultipartFile("file", fileName, "db", is);
		
		MockMultipartHttpServletRequestBuilder multipartRequest = 
				MockMvcRequestBuilders.multipart("/api/backup");
		
		mockMvc.perform(multipartRequest.file(backup))
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"));
		
		mockMvc.perform(delete("/api/deletebackup"))
		.andDo(print()).andExpect(status().isOk());
		
		User u = userRepository.findByEmail("test@yahoo.com");
		Assertions.assertThat(u.getBackupName()).isNull();
	}
	
	@Test 
	@WithMockUser(username = "test@yahoo.com")
	public void testDeleteBackUpWhenNoBackUpReturnsNotFound() throws Exception {
		createUser();
		mockMvc.perform(delete("/api/deletebackup"))
		.andDo(print()).andExpect(status().isNotFound());
	}
	
	@Test
	public void testRegisterUserSuccessfulAndEmailSent() throws Exception {
		mockMvc.perform(post("/api/register")
				.param("email", "test@yahoo.com")
				.param("password", "Test123$"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"));
		
		User u = userRepository.findByEmail(("test@yahoo.com"));
		Assertions.assertThat(u).isNotNull();
		Assertions.assertThat(u.isEnabled()).isFalse();
		
		MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
		String recepient = receivedMessage.getAllRecipients()[0].toString();
		String body = GreenMailUtil.getBody(receivedMessage);
		
		Assertions.assertThat(recepient).isEqualTo("test@yahoo.com");
		Assertions.assertThat(body)
		.contains("Verification code created to sign into your account");
	}
	
	@Test
	public void testRegisterDuplicateUserReturnsBadRequest() throws Exception {
		createUser();
		
		mockMvc.perform(post("/api/register")
				.param("email", "test@yahoo.com")
				.param("password", "Test123$"))
		.andDo(print())
		.andExpect(status().isBadRequest())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message")
				.value("There is an account associated with this email."));
	}
	
	@Test
	public void testDuplicateInactiveUserDeletesUserAndIsSuccessful() throws Exception {
		User u = createInActiveUser();
		u.setBackupName("this will be null");
		userRepository.save(u);
		
		mockMvc.perform(post("/api/register")
				.param("email", "test@yahoo.com")
				.param("password", "Old User Overwritten"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"));
		
		u = userRepository.findByEmail("test@yahoo.com");
		Assertions.assertThat(u.getBackupName()).isNull();
	}
	
	@Test
	public void testEnableAccountSuccessful() throws Exception {
		User u = createInActiveUser();
		u.setOneTimePassword();
		userRepository.save(u);
		
		mockMvc.perform(post("/api/enableaccount")
				.param("email", "test@yahoo.com")
				.param("code", String.valueOf(u.getOneTimePassword())))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"));
		
		u = userRepository.findByEmail("test@yahoo.com");
		Assertions.assertThat(u.isEnabled()).isTrue();
	}
	
	@Test
	public void testEnableAccountUserNotExistsReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/enableaccount")
				.param("email", "test@yahoo.com")
				.param("code", "1234"))
		.andDo(print())
		.andExpect(status().isBadRequest())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid Email"));
	}
	
	@Test
	public void testEnableAccountInvalidCodeReturnsUnauthorized() throws Exception {
		User u = createInActiveUser();
		u.setOneTimePassword();
		userRepository.save(u);
		
		mockMvc.perform(post("/api/enableaccount")
				.param("email", "test@yahoo.com")
				.param("code", String.valueOf(u.getOneTimePassword() + 1)))
		.andDo(print())
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid Verification Code"));
		
		u = userRepository.findByEmail("test@yahoo.com");
		Assertions.assertThat(u.isEnabled()).isFalse();
	}
	
	@Test
	public void testEnableAccountExpiredCodeReturnsUnauthorized() throws Exception {
		User u = createInActiveUser();
		u.setOneTimePassword();
		u.setOneTimePasswordDateAdded(new DateTime().minusDays(1).toDate());
		userRepository.save(u);
		
		mockMvc.perform(post("/api/enableaccount")
				.param("email", "test@yahoo.com")
				.param("code", String.valueOf(u.getOneTimePassword())))
		.andDo(print())
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message")
				.value("Verification Code is Expired"));
		
		u = userRepository.findByEmail("test@yahoo.com");
		Assertions.assertThat(u.isEnabled()).isFalse();
	}

	@Test
	public void testRequestTokenUserNotExistsIsBadRequest() throws Exception {
		mockMvc.perform(post("/api/requesttoken")
				.param("email", "test@yahoo.com"))
		.andDo(print())
		.andExpect(status().isBadRequest());
	}
	
	@Test
	public void testRequestTokenUserSuccessful() throws Exception {
		createUser();
		
		mockMvc.perform(post("/api/requesttoken")
				.param("email", "test@yahoo.com"))
		.andDo(print())
		.andExpect(status().isOk());
		

		MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
		String recepient = receivedMessage.getAllRecipients()[0].toString();
		String body = GreenMailUtil.getBody(receivedMessage);
		
		Assertions.assertThat(recepient).isEqualTo("test@yahoo.com");
		Assertions.assertThat(body)
		.contains("Verification code created to sign into your account");
	}
	
	@Test
	@WithMockUser(username = "test@yahoo.com")
	public void testDeleteAccount() throws Exception {
		User u = createUser();
		userRepository.save(u);
		
		mockMvc.perform(delete("/api/deleteaccount"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Account Deleted"));
		
		u = userRepository.findByEmail("test@yahoo.com");
		Assertions.assertThat(u).isNull();
	}
	
	@Test
	public void testIfMfaEnabledWhenDisabledNoEmailSent() throws Exception {
		User u = createUser();
		userRepository.save(u);
		
		mockMvc.perform(post("/api/mfaenabled")
				.param("email", "test@yahoo.com"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string("false"));
		
		MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		Assertions.assertThat(receivedMessages.length).isEqualTo(0);
	}
	
	@Test
	public void testIfMfaEnabledWhenEnabledEmailSent() throws Exception {
		User u = createUser();
		u.setTwoFactorAuthentication(true);
		userRepository.save(u);
		
		mockMvc.perform(post("/api/mfaenabled")
				.param("email", "test@yahoo.com"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string("true"));
		
		MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
		String recepient = receivedMessage.getAllRecipients()[0].toString();
		String body = GreenMailUtil.getBody(receivedMessage);
		
		Assertions.assertThat(recepient).isEqualTo("test@yahoo.com");
		Assertions.assertThat(body)
		.contains("Verification code created to sign into your account");
	}
	
	@Test
	public void testIfMfaEnabledBadRequestWhenUserIsNull() throws Exception {
		
		mockMvc.perform(post("/api/mfaenabled")
				.param("email", "test@yahoo.com"))
		.andDo(print())
		.andExpect(status().isBadRequest());
	}
	
	@Test
	public void testIfMfaEnabledBadRequestWhenUserIsInactive() throws Exception {
		createInActiveUser();
		mockMvc.perform(post("/api/mfaenabled")
				.param("email", "test@yahoo.com"))
		.andDo(print())
		.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser(username = "test@yahoo.com")
	public void testEnableMFA() throws Exception {
		User u = createUser();
		Assertions.assertThat(u.getTwoFactorAuthentication()).isFalse();
		
		mockMvc.perform(post("/api/enablemfa")
				.param("enabled", "true"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"));
		
		u = userRepository.findByEmail("test@yahoo.com");
		Assertions.assertThat(u.getTwoFactorAuthentication()).isTrue();
	}
	
	@Test
	public void testResetPasswordUserNotExistsRequestIsUnauthorized() throws Exception {
		mockMvc.perform(put("/api/resetpassword")
				.param("email", "test@yahoo.com")
				.param("password", "NewPassword")
				.param("code", "1234"))
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message")
				.value("Unauthorized"));
	}
	
	@Test
	public void testResetPasswordUserNotEnabledRequestIsUnauthorized() throws Exception {
		createInActiveUser();
		
		mockMvc.perform(put("/api/resetpassword")
				.param("email", "test@yahoo.com")
				.param("password", "NewPassword")
				.param("code", "1234"))
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message")
				.value("Unauthorized"));
	}
	
	@Test
	public void testResetPasswordVerificationCodeInvalidRequestUnauthorized() throws Exception {
		User u = createUser();
		u.setOneTimePassword();
		userRepository.save(u);
		
		mockMvc.perform(put("/api/resetpassword")
				.param("email", "test@yahoo.com")
				.param("password", "NewPassword")
				.param("code", String.valueOf(u.getOneTimePassword() + 1)))
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message")
				.value("Invalid Verification Code"));
	}
	
	@Test
	public void testResetPasswordVerificationCodeExpiredRequestUnauthorized() throws Exception {
		User u = createUser();
		u.setOneTimePassword();
		u.setOneTimePasswordDateAdded(new DateTime().minusDays(1).toDate());
		userRepository.save(u);
		
		mockMvc.perform(put("/api/resetpassword")
				.param("email", "test@yahoo.com")
				.param("password", "NewPassword")
				.param("code", String.valueOf(u.getOneTimePassword())))
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message")
				.value("The Verification Code is Expired"));
	}
	
	@Test
	public void testResetPasswordSuccessful() throws Exception {
		User u = createUser();
		u.setOneTimePassword();
		u = userRepository.save(u);
		
		
		mockMvc.perform(put("/api/resetpassword")
				.param("email", "test@yahoo.com")
				.param("password", "NewPassword")
				.param("code", String.valueOf(u.getOneTimePassword())))
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"));
		
		u = userRepository.findByEmail("test@yahoo.com");
		
		Assertions.assertThat(u.getPassword()).isEqualTo(passwordEncoder.encode("NewPassword"));
	}
	
	@Test
	@WithMockUser(username = "test@yahoo.com")
	public void testGetBackupInfoBackupNull() throws Exception {
		createUser();
		
		mockMvc.perform(get("/api/getbackupinfo"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.date").doesNotExist())
		.andExpect(MockMvcResultMatchers.jsonPath("$.size").doesNotExist());
	}
	
	@Test
	@WithMockUser(username = "test@yahoo.com")
	public void testGetBackupInfo() throws Exception {
		createUser();
		
		String fileName = "backup.db";
		
		Resource resource = new ClassPathResource("backup.db");
		
		InputStream is = resource.getInputStream();
		MockMultipartFile backup = new MockMultipartFile("file", fileName, "db", is);
		
		MockMultipartHttpServletRequestBuilder multipartRequest = 
				MockMvcRequestBuilders.multipart("/api/backup");
		
		mockMvc.perform(multipartRequest.file(backup))
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"));
		
		User u = userRepository.findByEmail("test@yahoo.com");
		u.setLastBackUpDate(new DateTime(2022, 4, 8, 22, 30).toDate());
		userRepository.save(u);
		
		mockMvc.perform(get("/api/getbackupinfo"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.date")
				.value(containsString("04")));
	}
	
	@Test
	public void testCreateSaltAndFindBySaltName() {
		Salt salt = new Salt("Test");
		salt.setSalt();
		saltRepository.save(salt);
		
		salt = saltRepository.findBySaltName("Test");
		Assertions.assertThat(salt).isNotNull();
	}
	
	@Test
	public void testSaltCannotBeNull() {
		Salt salt = new Salt("Test");
		
		assertThrows(DataIntegrityViolationException.class, () -> {
			saltRepository.save(salt);
		});
	}
	
	@Test
	public void testDefaultSaltIsCreatedWhenApplicationStarts() {
		Salt salt = saltRepository.findBySaltName("Default");
		Assertions.assertThat(salt).isNotNull();
	}
	
	@Test
	public void testLoginSuccessful2FADisabled() throws Exception {
		User user = new User("test@yahoo.com", passwordEncoder.encode("Test123$"));
		user.setEnabled(true);
		userRepository.save(user);
		
		mockMvc.perform(post("/login")
				.param("email", "test@yahoo.com")
				.param("password", "Test123$"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"))
		.andExpect(MockMvcResultMatchers.jsonPath("$.token").exists());
	}
	
	@Test
	public void testLoginSuccessful2FAEnabled() throws Exception {
		User user = new User("test@yahoo.com", passwordEncoder.encode("Test123$"));
		user.setEnabled(true);
		user.setTwoFactorAuthentication(true);
		user.setOneTimePassword();
		userRepository.save(user);
		
		mockMvc.perform(post("/login")
				.param("email", "test@yahoo.com")
				.param("password", "Test123$")
				.param("code", String.valueOf(user.getOneTimePassword())))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Success"))
		.andExpect(MockMvcResultMatchers.jsonPath("$.token").exists());
	}
	
	@Test
	public void testLoginUnauthorizedWhenUserDoesNotExist() throws Exception {
		mockMvc.perform(post("/login")
				.param("email", "test@yahoo.com")
				.param("password", "Test123$"))
		.andDo(print())
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid Username or Password"))
		.andExpect(MockMvcResultMatchers.jsonPath("$.token").doesNotExist());
	}
	
	@Test
	public void testLoginUnauthorizedWhenUserNotEnabled() throws Exception {
		createInActiveUser();
		mockMvc.perform(post("/login")
				.param("email", "test@yahoo.com")
				.param("password", "Test123$"))
		.andDo(print())
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("User is disabled"))
		.andExpect(MockMvcResultMatchers.jsonPath("$.token").doesNotExist());
	}
	
	@Test
	public void testLoginUnauthorizedInvalidPassword() throws Exception {
		User user = new User("test@yahoo.com", passwordEncoder.encode("Test123$"));
		user.setEnabled(true);
		userRepository.save(user);
		
		mockMvc.perform(post("/login")
				.param("email", "test@yahoo.com")
				.param("password", "Test123")) //Wrong password
		.andDo(print())
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid Username or Password"))
		.andExpect(MockMvcResultMatchers.jsonPath("$.token").doesNotExist());
	}
	
	@Test
	public void testLoginUnauthorizedInvalidCodeWhen2faEnabled() throws Exception {
		User user = new User("test@yahoo.com", passwordEncoder.encode("Test123$"));
		user.setTwoFactorAuthentication(true);
		user.setOneTimePassword();
		user.setEnabled(true);
		userRepository.save(user);
		
		mockMvc.perform(post("/login")
				.param("email", "test@yahoo.com")
				.param("password", "Test123$")
				.param("code", String.valueOf(user.getOneTimePassword() - 1)))
		.andDo(print())
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid Verification Code"))
		.andExpect(MockMvcResultMatchers.jsonPath("$.token").doesNotExist());
	}
	
	@Test
	public void testUnauthorizedExpiredVerificationCodeWhen2faEnabled() throws Exception {
		User user = new User("test@yahoo.com", passwordEncoder.encode("Test123$"));
		user.setTwoFactorAuthentication(true);
		user.setOneTimePassword();
		user.setOneTimePasswordDateAdded(new DateTime().minusDays(1).toDate());
		user.setEnabled(true);
		userRepository.save(user);
		
		mockMvc.perform(post("/login")
				.param("email", "test@yahoo.com")
				.param("password", "Test123$")
				.param("code", String.valueOf(user.getOneTimePassword())))
		.andDo(print())
		.andExpect(status().isUnauthorized())
		.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Verification Code is Expired"))
		.andExpect(MockMvcResultMatchers.jsonPath("$.token").doesNotExist());
	}
	
	
	private User createUser() {
		User u =  new User("test@yahoo.com", "Test123$");
		u.setEnabled(true);
		userRepository.save(u);
		return u;
	}
	
	private User createInActiveUser() {
		User u =  new User("test@yahoo.com", "Test123$");
		userRepository.save(u);
		return u;
	}
	
}