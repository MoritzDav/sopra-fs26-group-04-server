package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.CourseEnrollmentRepository;
import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final CourseRepository courseRepository;
	private final CourseEnrollmentRepository courseEnrollmentRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository,
					@Qualifier("courseRepository") CourseRepository courseRepository,
					@Qualifier("courseEnrollmentRepository") CourseEnrollmentRepository courseEnrollmentRepository) {
		this.userRepository = userRepository;
		this.courseRepository = courseRepository;
		this.courseEnrollmentRepository = courseEnrollmentRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User getUserById(Long id) {
		return userRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	public User loginUser(String username, String password) {
		User user = userRepository.findByUsername(username);
		
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
		
		// Simple password validation (not hashed - for testing only)
		if (!user.getPassword().equals(password)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
		
		// Set status to ONLINE
		user.setStatus(UserStatus.ONLINE);
		user = userRepository.save(user);
		userRepository.flush();
		
		log.debug("User {} logged in", username);
		return user;
	}

	public User logoutUser(Long id, Long requestingUserId) {
		// Check ownership: only the user can logout themselves
		if (!id.equals(requestingUserId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only logout your own account");
		}
		
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		
		user.setStatus(UserStatus.OFFLINE);
		user = userRepository.save(user);
		userRepository.flush();
		
		log.debug("User {} logged out", id);
		return user;
	}
	
	public User createUser(User newUser) {
		newUser.setToken(UUID.randomUUID().toString());
		
		//We need to fix whether we need UserStatus and if we need to be automatically online
		newUser.setStatus(UserStatus.ONLINE);

		// Set default role to STUDENT if not provided
		if (newUser.getRole() == null) {
			newUser.setRole(UserRole.STUDENT);
		}

		checkIfUserExists(newUser);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	public User updateUser(Long id, UserPutDTO updates) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (updates.getFirstName() != null && !updates.getFirstName().isBlank()) {
			user.setFirstName(updates.getFirstName());
		}
		if (updates.getLastName() != null && !updates.getLastName().isBlank()) {
			user.setLastName(updates.getLastName());
		}
		if (updates.getUsername() != null && !updates.getUsername().isBlank()) {
			if (userRepository.findByUsername(updates.getUsername()) != null) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists!");
			}
			user.setUsername(updates.getUsername());
		}
		if (updates.getNewPassword() != null && !updates.getNewPassword().isBlank()) {
			if (updates.getOldPassword() == null || !user.getPassword().equals(updates.getOldPassword())) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
			}
			if (user.getPassword().equals(updates.getNewPassword())) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "New password must be different from the current password");
			}
			user.setPassword(updates.getNewPassword());
		}

		user = userRepository.save(user);
		userRepository.flush();

		log.debug("Updated user {}", id);
		return user;
	}

	public List<Course> getCoursesByUser(Long id, String token) {

		//Validate the token, no matching due to possible functionality
		userRepository.findByToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
		
		//Fetch user
		User user = userRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		//Different methodes for fetching the courses depending on user role
		if (user.getRole() == UserRole.TEACHER){
			return courseRepository.findByTeacherId(user.getId());
		} else {
			List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentId(user.getId());
			List<Course> courses = new ArrayList<>();
			for (CourseEnrollment enrollment : enrollments){
				Course course = courseRepository.findById(enrollment.getCourseId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
				courses.add(course);
			}
			return courses;
		}
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
        if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists!");
        }
    }

}
