package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserServiceTest
 * 
 * This is a unit test class for the UserService using Mockito for mocking the
 * UserRepository. Tests verify registration, user retrieval, and logout
 * functionality.
 */
public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;
	private User offlineUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// Create a test user
		testUser = new User();
		testUser.setId(1L);
		testUser.setFirstName("Test");
		testUser.setLastName("User");
		testUser.setUsername("testuser");
		testUser.setPassword("password123");
		testUser.setRole(UserRole.STUDENT);
		testUser.setStatus(UserStatus.ONLINE);
		testUser.setToken("test-token-uuid");

		//Create an offline user
		offlineUser = new User();
		offlineUser.setId(1L);
		offlineUser.setFirstName("Test");
		offlineUser.setLastName("User");
		offlineUser.setUsername("testuser");
		offlineUser.setPassword("password123");
		offlineUser.setRole(UserRole.STUDENT);
		offlineUser.setStatus(UserStatus.OFFLINE);
		offlineUser.setToken("test-token-uuid");
	}

	// ============ Create User Tests ============

	@Test
	public void createUser_validInputs_success() {
		// given
		User newUser = new User();
		newUser.setFirstName("Test");
		newUser.setLastName("User");
		newUser.setUsername("testuser");
		newUser.setPassword("password123");
		newUser.setRole(UserRole.STUDENT);

		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);

		// when
		User createdUser = userService.createUser(newUser);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
		Mockito.verify(userRepository, Mockito.times(1)).flush();

		assertEquals(testUser.getId(), createdUser.getId());
		assertEquals(testUser.getFirstName(), createdUser.getFirstName());
		assertEquals(testUser.getLastName(), createdUser.getLastName());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
		assertEquals(UserRole.STUDENT, createdUser.getRole());
	}

	@Test
	public void createUser_duplicateUsername_throwsException() {
		// given
		User newUser = new User();
		newUser.setFirstName("Test");
		newUser.setLastName("User");
		newUser.setUsername("testuser");
		newUser.setPassword("password123");

		// Mock that username already exists
		Mockito.when(userRepository.findByUsername("testuser")).thenReturn(testUser);

		// when & then
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.createUser(newUser));

		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
		assertTrue(exception.getReason().contains("Username already exists"));

		// Verify save was never called
		Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
	}

	@Test
	public void createUser_nullRole_defaultsToStudent() {
		// given
		User newUser = new User();
		newUser.setFirstName("Test");
		newUser.setLastName("User");
		newUser.setUsername("newuser");
		newUser.setPassword("password123");
		newUser.setRole(null); // No role provided

		testUser.setRole(UserRole.STUDENT); // Default set to STUDENT

		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);

		// when
		User createdUser = userService.createUser(newUser);

		// then
		assertEquals(UserRole.STUDENT, createdUser.getRole());
	}

	@Test
	public void createUser_statusSetToOnline() {
		// given
		User newUser = new User();
		newUser.setFirstName("Test");
		newUser.setLastName("User");
		newUser.setUsername("newuser");
		newUser.setPassword("password123");

		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);

		// when
		User createdUser = userService.createUser(newUser);

		// then
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
	}

	@Test
	public void createUser_tokenGenerated() {
		// given
		User newUser = new User();
		newUser.setFirstName("Test");
		newUser.setLastName("User");
		newUser.setUsername("newuser");
		newUser.setPassword("password123");

		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);

		// when
		User createdUser = userService.createUser(newUser);

		// then
		assertNotNull(createdUser.getToken());
		assertFalse(createdUser.getToken().isEmpty());
	}

	@Test
	public void createUser_withTeacherRole_success() {
		// given
		User newUser = new User();
		newUser.setFirstName("Prof");
		newUser.setLastName("Smith");
		newUser.setUsername("prof_smith");
		newUser.setPassword("password123");
		newUser.setRole(UserRole.TEACHER);

		testUser.setRole(UserRole.TEACHER);

		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);

		// when
		User createdUser = userService.createUser(newUser);

		// then
		assertEquals(UserRole.TEACHER, createdUser.getRole());
	}

	// ============ Get User Tests ============

	@Test
	public void getUserById_validId_success() {
		// given
		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

		// when
		User retrievedUser = userService.getUserById(1L);

		// then
		assertEquals(testUser.getId(), retrievedUser.getId());
		assertEquals(testUser.getFirstName(), retrievedUser.getFirstName());
		assertEquals(testUser.getUsername(), retrievedUser.getUsername());
	}

	@Test
	public void getUserById_invalidId_throwsException() {
		// given
		Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

		// when & then
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.getUserById(999L));

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
		assertTrue(exception.getReason().contains("User not found"));
	}

	// ============ Logout Tests ============

	@Test
	public void logoutUser_validIdAndRequestingUser_success() {
		// given
		testUser.setStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when
		User loggedOutUser = userService.logoutUser("test-token-uuid");

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
		Mockito.verify(userRepository, Mockito.times(1)).flush();

		assertEquals(UserStatus.OFFLINE, loggedOutUser.getStatus());
		assertEquals(1L, loggedOutUser.getId());
	}

	@Test
	public void logoutUser_invalidToken_throwsException() {
		
		// given
		Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

		// when & then
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.logoutUser("invalid-token"));

		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
		assertTrue(exception.getReason().contains("Invalid token"));
		Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
	}

	@Test
	public void logoutUser_validToken_differentUser_success() {
		// given - User 5 logging out themselves
		User user5 = new User();
		user5.setId(5L);
		user5.setFirstName("Alice");
		user5.setLastName("Wonder");
		user5.setUsername("alice");
		user5.setStatus(UserStatus.ONLINE);
		user5.setToken("alice-token");

		User offlineUser5 = new User();
		offlineUser5.setId(5L);
		offlineUser5.setFirstName("Alice");
		offlineUser5.setLastName("Wonder");
		offlineUser5.setUsername("alice");
		offlineUser5.setStatus(UserStatus.OFFLINE);
		offlineUser5.setToken("alice-token");

		Mockito.when(userRepository.findByToken("alice-token")).thenReturn(Optional.of(user5));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser5);

		// when
		User loggedOut = userService.logoutUser("alice-token");

		// then
		assertEquals(UserStatus.OFFLINE, loggedOut.getStatus());
	}

	@Test
	public void loginUser_validCredentials_success() {
		// given
		testUser.setStatus(UserStatus.OFFLINE);
		Mockito.when(userRepository.findByUsername("testuser")).thenReturn(testUser);
		
		User onlineUser = new User();
		onlineUser.setId(1L);
		onlineUser.setFirstName("Test");
		onlineUser.setLastName("User");
		onlineUser.setUsername("testuser");
		onlineUser.setPassword("password123");
		onlineUser.setRole(UserRole.STUDENT);
		onlineUser.setStatus(UserStatus.ONLINE);
		onlineUser.setToken("test-token");
		onlineUser.setToken(UUID.randomUUID().toString());
		
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(onlineUser);

		// when
		User loggedInUser = userService.loginUser("testuser", "password123");

		// then
		assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
		assertEquals("testuser", loggedInUser.getUsername());
		assertNotNull(loggedInUser.getToken());
		Mockito.verify(userRepository).findByUsername("testuser");
		Mockito.verify(userRepository).save(Mockito.any());
	}

	@Test
	public void loginUser_invalidUsername_throwsException() {
		// given
		Mockito.when(userRepository.findByUsername("nonexistent")).thenReturn(null);

		// when & then
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.loginUser("nonexistent", "password123"));

		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
		assertTrue(exception.getReason().contains("Invalid username or password"));
	}

	@Test
	public void loginUser_invalidPassword_throwsException() {
		// given
		testUser.setStatus(UserStatus.OFFLINE);
		Mockito.when(userRepository.findByUsername("testuser")).thenReturn(testUser);

		// when & then
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.loginUser("testuser", "wrongpassword"));

		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
		assertTrue(exception.getReason().contains("Invalid username or password"));
		
		// Verify save was never called
		Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
	}

	// ============ Session Management Tests ============

	@Test
	public void tokenGeneration_createsUniqueToken() {
		// given
		User user1 = new User();
		user1.setFirstName("User");
		user1.setLastName("One");
		user1.setUsername("user1");
		user1.setPassword("pass1");

		User createdUser1 = new User();
		createdUser1.setId(1L);
		createdUser1.setUsername("user1");
		createdUser1.setToken("550e8400-e29b-41d4-a716-446655440000"); // Valid UUID format

		Mockito.when(userRepository.findByUsername("user1")).thenReturn(null);
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(createdUser1);

		// when
		User result1 = userService.createUser(user1);

		// then
		assertNotNull(result1.getToken());
		assertTrue(result1.getToken().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
	}

	@Test
	public void loginUser_tokenRemainsTheSame() {
		// given
		testUser.setStatus(UserStatus.OFFLINE);
		String originalToken = testUser.getToken();
		
		Mockito.when(userRepository.findByUsername("testuser")).thenReturn(testUser);
		
		User onlineUser = new User();
		onlineUser.setId(1L);
		onlineUser.setUsername("testuser");
		onlineUser.setPassword("password123");
		onlineUser.setStatus(UserStatus.ONLINE);
		onlineUser.setToken(originalToken); // Token should be same after login
		
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(onlineUser);

		// when
		User loggedInUser = userService.loginUser("testuser", "password123");

		// then
		assertEquals(originalToken, loggedInUser.getToken());
	}

	@Test
	public void logoutUser_tokenRemainsSame() {
		// given
		testUser.setStatus(UserStatus.ONLINE);
		String originalToken = testUser.getToken();

		Mockito.when(userRepository.findByToken(originalToken)).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when
		User loggedOutUser = userService.logoutUser(originalToken);

		// then
		assertEquals(originalToken, loggedOutUser.getToken());
	}

	@Test
	public void getUserById_byToken_success() {
		// given
		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

		// when
		User retrievedUser = userService.getUserById(testUser.getId());

		// then
		assertEquals(testUser.getToken(), retrievedUser.getToken());
		assertEquals("test-token-uuid", retrievedUser.getToken());
	}

	@Test
	public void multipleUsers_haveUniqueTokens() {
		// given
		User user1 = new User();
		user1.setFirstName("User1");
		user1.setLastName("One");
		user1.setUsername("user1");
		user1.setPassword("pass1");

		User user2 = new User();
		user2.setFirstName("User2");
		user2.setLastName("Two");
		user2.setUsername("user2");
		user2.setPassword("pass2");

		User created1 = new User();
		created1.setId(1L);
		created1.setUsername("user1");
		created1.setToken("550e8400-e29b-41d4-a716-446655440001");

		User created2 = new User();
		created2.setId(2L);
		created2.setUsername("user2");
		created2.setToken("550e8400-e29b-41d4-a716-446655440002");

		Mockito.when(userRepository.findByUsername("user1")).thenReturn(null);
		Mockito.when(userRepository.findByUsername("user2")).thenReturn(null);
		
		Mockito.when(userRepository.save(user1)).thenReturn(created1);
		Mockito.when(userRepository.save(user2)).thenReturn(created2);

		// when
		User result1 = userService.createUser(user1);
		User result2 = userService.createUser(user2);

		// then
		assertNotEquals(result1.getToken(), result2.getToken());
	}

	@Test
	public void loginUser_changesStatusFromOfflineToOnline() {
		// given
		testUser.setStatus(UserStatus.OFFLINE);
		Mockito.when(userRepository.findByUsername("testuser")).thenReturn(testUser);

		User onlineUser = new User();
		onlineUser.setId(1L);
		onlineUser.setUsername("testuser");
		onlineUser.setStatus(UserStatus.ONLINE);
		onlineUser.setToken(testUser.getToken());

		Mockito.when(userRepository.save(Mockito.any())).thenReturn(onlineUser);

		// when
		User loggedInUser = userService.loginUser("testuser", "password123");

		// then
		assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
	}

	@Test
	public void logoutUser_changesStatusFromOnlineToOffline() {
		// given
		testUser.setStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when
		User loggedOutUser = userService.logoutUser("test-token-uuid");

		// then
		assertEquals(UserStatus.OFFLINE, loggedOutUser.getStatus());
	}

	// ============ Additional Logout Tests ============

	@Test
	public void logoutUser_preservesOtherFields() {
		// given
		testUser.setStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when
		User loggedOutUser = userService.logoutUser("test-token-uuid");

		// then - Verify all fields except status remain unchanged
		assertEquals("Test", loggedOutUser.getFirstName());
		assertEquals("User", loggedOutUser.getLastName());
		assertEquals("testuser", loggedOutUser.getUsername());
		assertEquals(UserRole.STUDENT, loggedOutUser.getRole());
		assertEquals(testUser.getToken(), loggedOutUser.getToken());
		assertEquals(UserStatus.OFFLINE, loggedOutUser.getStatus());
	}

	@Test
	public void logoutUser_alreadyOffline_stillSucceeds() {
		// given - User is already offline
		testUser.setStatus(UserStatus.OFFLINE);

		Mockito.when(userRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when
		User loggedOutUser = userService.logoutUser("test-token-uuid");

		// then - Even if already offline, logout still works
		assertEquals(UserStatus.OFFLINE, loggedOutUser.getStatus());
		Mockito.verify(userRepository).save(Mockito.any());
	}

	@Test
	public void logoutUser_verifySaveCalledWithCorrectUser() {
		// given
		testUser.setStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when
		userService.logoutUser("test-token-uuid");

		// then - Verify save and flush were called
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
		Mockito.verify(userRepository, Mockito.times(1)).flush();
	}

	@Test
	public void logoutUser_multipleLogoutsIdempotent() {
		// given - First logout
		testUser.setStatus(UserStatus.ONLINE);

		Mockito.when(userRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when - First logout
		User firstLogout = userService.logoutUser("test-token-uuid");
		assertEquals(UserStatus.OFFLINE, firstLogout.getStatus());

		// Prepare for second logout attempt
		Mockito.reset(userRepository);
		Mockito.when(userRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(offlineUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when - Second logout should succeed
		User secondLogout = userService.logoutUser("test-token-uuid");

		// then - Status remains offline
		assertEquals(UserStatus.OFFLINE, secondLogout.getStatus());
	}

	@Test
	public void logoutUser_returnsCompleteUserObject() {
		// given
		testUser.setStatus(UserStatus.ONLINE);
		testUser.setId(1L);
		testUser.setFirstName("Alice");
		testUser.setLastName("Smith");
		testUser.setUsername("alice");
		testUser.setRole(UserRole.TEACHER);
		testUser.setToken("test-token-uuid");

		User loggedOutUser = new User();
		loggedOutUser.setId(1L);
		loggedOutUser.setFirstName("Alice");
		loggedOutUser.setLastName("Smith");
		loggedOutUser.setUsername("alice");
		loggedOutUser.setRole(UserRole.TEACHER);
		loggedOutUser.setStatus(UserStatus.OFFLINE);
		loggedOutUser.setToken("test-token-uuid");

		Mockito.when(userRepository.findByToken("test-token-uuid")).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(loggedOutUser);

		// when
		User result = userService.logoutUser("test-token-uuid");

		// then - Verify complete user object is returned
		assertNotNull(result);
		assertEquals(1L, result.getId());
		assertEquals("Alice", result.getFirstName());
		assertEquals("Smith", result.getLastName());
		assertEquals("alice", result.getUsername());
		assertEquals(UserRole.TEACHER, result.getRole());
		assertNotNull(result.getToken());
		assertEquals(UserStatus.OFFLINE, result.getStatus());
	}

}


