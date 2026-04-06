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
		User offlineUser = new User();
		offlineUser.setId(1L);
		offlineUser.setFirstName("Test");
		offlineUser.setLastName("User");
		offlineUser.setUsername("testuser");
		offlineUser.setStatus(UserStatus.OFFLINE);
		offlineUser.setToken("test-token-uuid");

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser);

		// when
		User loggedOutUser = userService.logoutUser(1L, 1L);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
		Mockito.verify(userRepository, Mockito.times(1)).flush();

		assertEquals(UserStatus.OFFLINE, loggedOutUser.getStatus());
		assertEquals(1L, loggedOutUser.getId());
	}

	@Test
	public void logoutUser_ownershipCheckFails_throwsException() {
		// given
		Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

		// when & then - User 1 trying to logout User 2
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.logoutUser(2L, 1L));

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
		assertTrue(exception.getReason().contains("only logout your own account"));

		// Verify save was never called
		Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
	}

	@Test
	public void logoutUser_userNotFound_throwsException() {
		// given
		Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

		// when & then
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> userService.logoutUser(999L, 999L));

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
		assertTrue(exception.getReason().contains("User not found"));
	}

	@Test
	public void logoutUser_correctOwnership_success() {
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

		Mockito.when(userRepository.findById(5L)).thenReturn(Optional.of(user5));
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(offlineUser5);

		// when
		User loggedOut = userService.logoutUser(5L, 5L);

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
		
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(onlineUser);

		// when
		User loggedInUser = userService.loginUser("testuser", "password123");

		// then
		assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
		assertEquals("testuser", loggedInUser.getUsername());
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

}
