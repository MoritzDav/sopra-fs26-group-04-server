package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the UserService with integration testing.
 * 
 * This integration test uses the actual H2 database and tests the full
 * flow without mocking.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

	@Qualifier("userRepository")
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@BeforeEach
	public void setup() {
		userRepository.deleteAll();
	}

	// ============ Create User Tests ============

	@Test
	public void createUser_validInputs_success() {
		// given
		assertNull(userRepository.findByUsername("testuser"));

		User testUser = new User();
		testUser.setFirstName("Test");
		testUser.setLastName("User");
		testUser.setUsername("testuser");
		testUser.setPassword("password123");
		testUser.setRole(UserRole.STUDENT);

		// when
		User createdUser = userService.createUser(testUser);

		// then
		assertNotNull(createdUser.getId());
		assertEquals("Test", createdUser.getFirstName());
		assertEquals("User", createdUser.getLastName());
		assertEquals("testuser", createdUser.getUsername());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
		assertEquals(UserRole.STUDENT, createdUser.getRole());
	}

	@Test
	public void createUser_duplicateUsername_throwsException() {
		// given
		assertNull(userRepository.findByUsername("duplicate"));

		User testUser = new User();
		testUser.setFirstName("Alice");
		testUser.setLastName("Smith");
		testUser.setUsername("duplicate");
		testUser.setPassword("password123");

		// Create first user - should succeed
		userService.createUser(testUser);

		// Verify user was created
		assertNotNull(userRepository.findByUsername("duplicate"));

		// when - attempt to create second user with same username
		User testUser2 = new User();
		testUser2.setFirstName("Bob");
		testUser2.setLastName("Jones");
		testUser2.setUsername("duplicate");
		testUser2.setPassword("password456");

		// then - check that an error is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
	}

	@Test
	public void createUser_withTeacherRole_success() {
		// given
		User teacher = new User();
		teacher.setFirstName("Professor");
		teacher.setLastName("Smith");
		teacher.setUsername("prof_smith");
		teacher.setPassword("password123");
		teacher.setRole(UserRole.TEACHER);

		// when
		User createdTeacher = userService.createUser(teacher);

		// then
		assertEquals(UserRole.TEACHER, createdTeacher.getRole());
		assertEquals(UserStatus.ONLINE, createdTeacher.getStatus());
		assertNotNull(createdTeacher.getToken());
	}

	@Test
	public void createUser_multipleUsers_allCreated() {
		// given
		User user1 = new User();
		user1.setFirstName("User");
		user1.setLastName("One");
		user1.setUsername("user1");
		user1.setPassword("pass1");

		User user2 = new User();
		user2.setFirstName("User");
		user2.setLastName("Two");
		user2.setUsername("user2");
		user2.setPassword("pass2");

		// when
		User created1 = userService.createUser(user1);
		User created2 = userService.createUser(user2);

		// then
		assertNotNull(created1.getId());
		assertNotNull(created2.getId());
		assertNotEquals(created1.getId(), created2.getId());
		assertEquals("user1", created1.getUsername());
		assertEquals("user2", created2.getUsername());
	}

	// ============ Get User Tests ============

	@Test
	public void getUserById_validId_success() {
		// given
		User testUser = new User();
		testUser.setFirstName("Test");
		testUser.setLastName("User");
		testUser.setUsername("gettest");
		testUser.setPassword("password123");

		User createdUser = userService.createUser(testUser);

		// when
		User retrievedUser = userService.getUserById(createdUser.getId());

		// then
		assertEquals(createdUser.getId(), retrievedUser.getId());
		assertEquals("Test", retrievedUser.getFirstName());
		assertEquals("gettest", retrievedUser.getUsername());
	}

	@Test
	public void getUserById_invalidId_throwsException() {
		// given
		Long invalidId = 9999L;

		// when & then
		assertThrows(ResponseStatusException.class, () -> userService.getUserById(invalidId));
	}

	// ============ Logout Tests ============

	@Test
	public void logoutUser_validUser_statusChangedToOffline() {
		// given
		User testUser = new User();
		testUser.setFirstName("Test");
		testUser.setLastName("User");
		testUser.setUsername("logouttest");
		testUser.setPassword("password123");

		User createdUser = userService.createUser(testUser);
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());

		// when
		User loggedOutUser = userService.logoutUser(createdUser.getId(), createdUser.getId());

		// then
		assertEquals(UserStatus.OFFLINE, loggedOutUser.getStatus());

		// Verify status persisted in database
		User retrievedUser = userService.getUserById(createdUser.getId());
		assertEquals(UserStatus.OFFLINE, retrievedUser.getStatus());
	}

	@Test
	public void logoutUser_ownershipCheck_throwsException() {
		// given
		User user1 = new User();
		user1.setFirstName("User");
		user1.setLastName("One");
		user1.setUsername("user_one");
		user1.setPassword("pass1");

		User user2 = new User();
		user2.setFirstName("User");
		user2.setLastName("Two");
		user2.setUsername("user_two");
		user2.setPassword("pass2");

		User created1 = userService.createUser(user1);
		User created2 = userService.createUser(user2);

		// when & then - User 1 trying to logout User 2
		assertThrows(ResponseStatusException.class,
				() -> userService.logoutUser(created2.getId(), created1.getId()));

		// Verify user 2 is still ONLINE
		User user2AfterAttempt = userService.getUserById(created2.getId());
		assertEquals(UserStatus.ONLINE, user2AfterAttempt.getStatus());
	}

	@Test
	public void logoutUser_nonexistentUser_throwsException() {
		// given
		Long nonexistentId = 9999L;

		// when & then
		assertThrows(ResponseStatusException.class,
				() -> userService.logoutUser(nonexistentId, nonexistentId));
	}

	// ============ Login User Tests ============

	@Test
	public void loginUser_validCredentials_success() {
		// given
		User testUser = new User();
		testUser.setFirstName("Login");
		testUser.setLastName("Test");
		testUser.setUsername("logintest");
		testUser.setPassword("password123");

		User createdUser = userService.createUser(testUser);
		Long userId = createdUser.getId();

		// Logout first to set OFFLINE
		userService.logoutUser(userId, userId);
		User offlineUser = userService.getUserById(userId);
		assertEquals(UserStatus.OFFLINE, offlineUser.getStatus());

		// when
		User loggedInUser = userService.loginUser("logintest", "password123");

		// then
		assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
		assertEquals("logintest", loggedInUser.getUsername());

		// Verify status persisted in database
		User retrievedUser = userService.getUserById(userId);
		assertEquals(UserStatus.ONLINE, retrievedUser.getStatus());
	}

	@Test
	public void loginUser_invalidUsername_throwsException() {
		// given
		User testUser = new User();
		testUser.setFirstName("Test");
		testUser.setLastName("User");
		testUser.setUsername("existinguser");
		testUser.setPassword("password123");

		userService.createUser(testUser);

		// when & then
		assertThrows(ResponseStatusException.class,
				() -> userService.loginUser("nonexistent", "password123"));
	}

	@Test
	public void loginUser_invalidPassword_throwsException() {
		// given
		User testUser = new User();
		testUser.setFirstName("Test");
		testUser.setLastName("User");
		testUser.setUsername("testuser2");
		testUser.setPassword("correctpassword");

		userService.createUser(testUser);

		// when & then
		assertThrows(ResponseStatusException.class,
				() -> userService.loginUser("testuser2", "wrongpassword"));
	}

	// ============ Session Management Tests ============

	@Test
	public void tokenGeneration_createsUniqueUUIDToken() {
		// given
		User user = new User();
		user.setFirstName("Token");
		user.setLastName("Test");
		user.setUsername("tokentest");
		user.setPassword("password123");

		// when
		User createdUser = userService.createUser(user);

		// then
		assertNotNull(createdUser.getToken());
		// UUID format: 8-4-4-4-12 hex digits
		assertTrue(createdUser.getToken().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
	}

	@Test
	public void multipleUsers_haveUniqueTokens() {
		// given
		User user1 = new User();
		user1.setFirstName("User");
		user1.setLastName("One");
		user1.setUsername("unique_user1");
		user1.setPassword("pass1");

		User user2 = new User();
		user2.setFirstName("User");
		user2.setLastName("Two");
		user2.setUsername("unique_user2");
		user2.setPassword("pass2");

		// when
		User created1 = userService.createUser(user1);
		User created2 = userService.createUser(user2);

		// then
		assertNotEquals(created1.getToken(), created2.getToken());
		assertNotNull(created1.getToken());
		assertNotNull(created2.getToken());
	}

	@Test
	public void loginUser_tokenRemainsSame() {
		// given
		User testUser = new User();
		testUser.setFirstName("Login");
		testUser.setLastName("Token");
		testUser.setUsername("logintokentest");
		testUser.setPassword("password123");

		User createdUser = userService.createUser(testUser);
		String originalToken = createdUser.getToken();

		// Logout to set OFFLINE
		userService.logoutUser(createdUser.getId(), createdUser.getId());

		// when
		User loggedInUser = userService.loginUser("logintokentest", "password123");

		// then
		assertEquals(originalToken, loggedInUser.getToken());
	}

	@Test
	public void logoutUser_tokenRemainsSame() {
		// given
		User testUser = new User();
		testUser.setFirstName("Logout");
		testUser.setLastName("Token");
		testUser.setUsername("logouttokentest");
		testUser.setPassword("password123");

		User createdUser = userService.createUser(testUser);
		String originalToken = createdUser.getToken();

		// when
		User loggedOutUser = userService.logoutUser(createdUser.getId(), createdUser.getId());

		// then
		assertEquals(originalToken, loggedOutUser.getToken());
	}

	@Test
	public void loginUser_statusChangesPersistsWithToken() {
		// given
		User testUser = new User();
		testUser.setFirstName("Status");
		testUser.setLastName("Change");
		testUser.setUsername("statuschangetest");
		testUser.setPassword("password123");

		User createdUser = userService.createUser(testUser);
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
		String token = createdUser.getToken();

		// Logout
		userService.logoutUser(createdUser.getId(), createdUser.getId());
		User offlineUser = userService.getUserById(createdUser.getId());
		assertEquals(UserStatus.OFFLINE, offlineUser.getStatus());
		assertEquals(token, offlineUser.getToken());

		// when - Login again
		User loggedInUser = userService.loginUser("statuschangetest", "password123");

		// then
		assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
		assertEquals(token, loggedInUser.getToken());

		// Verify in DB
		User verifiedUser = userService.getUserById(createdUser.getId());
		assertEquals(UserStatus.ONLINE, verifiedUser.getStatus());
		assertEquals(token, verifiedUser.getToken());
	}

	@Test
	public void concurrentSessions_samUserMultipleLogins() {
		// given
		User testUser = new User();
		testUser.setFirstName("Concurrent");
		testUser.setLastName("Session");
		testUser.setUsername("concurrenttest");
		testUser.setPassword("password123");

		User createdUser = userService.createUser(testUser);
		String token = createdUser.getToken();
		Long userId = createdUser.getId();

		// Logout first
		userService.logoutUser(userId, userId);

		// when - Login
		User login1 = userService.loginUser("concurrenttest", "password123");
		assertEquals(UserStatus.ONLINE, login1.getStatus());
		assertEquals(token, login1.getToken());

		// then - Token and user status should be consistent
		User retrievedUser = userService.getUserById(userId);
		assertEquals(UserStatus.ONLINE, retrievedUser.getStatus());
		assertEquals(token, retrievedUser.getToken());
	}

	@Test
	public void tokenRetrievalAfterLoginLogout() {
		// given
		User testUser = new User();
		testUser.setFirstName("Retrieval");
		testUser.setLastName("Test");
		testUser.setUsername("retrievaltest");
		testUser.setPassword("password123");

		User createdUser = userService.createUser(testUser);
		Long userId = createdUser.getId();
		String token = createdUser.getToken();

		// when
		User retrievedBeforeLogout = userService.getUserById(userId);
		userService.logoutUser(userId, userId);
		User retrievedAfterLogout = userService.getUserById(userId);
		userService.loginUser("retrievaltest", "password123");
		User retrievedAfterLogin = userService.getUserById(userId);

		// then
		assertEquals(token, retrievedBeforeLogout.getToken());
		assertEquals(token, retrievedAfterLogout.getToken());
		assertEquals(token, retrievedAfterLogin.getToken());
	}

}

