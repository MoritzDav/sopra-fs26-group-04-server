package ch.uzh.ifi.hase.soprafs26.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CourseGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;
/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/users/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserAuthDTO getUser(@PathVariable Long id) {
		// get user by id
		User user = userService.getUserById(id);

		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntitytoUserAuthDTO(user);
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserAuthDTO createUser(@Valid @RequestBody UserPostDTO userPostDTO) {
		
		// convert API user to internal representation
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// create user
		User createdUser = userService.createUser(userInput);

		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntitytoUserAuthDTO(createdUser);
	}

	@PostMapping("/users/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserAuthDTO loginUser(@RequestBody UserLoginDTO userLoginDTO) {
		// login user with username and password
		User loggedInUser = userService.loginUser(userLoginDTO.getUsername(), userLoginDTO.getPassword());

		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntitytoUserAuthDTO(loggedInUser);
	}

	@PutMapping("/users/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserAuthDTO updateUser(@PathVariable Long id, @RequestBody UserPutDTO userPutDTO) {
		User updatedUser = userService.updateUser(id, userPutDTO);
		return DTOMapper.INSTANCE.convertEntitytoUserAuthDTO(updatedUser);
	}

	@PostMapping("/users/{id}/logout")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserAuthDTO logoutUser(@PathVariable Long id, @RequestParam Long requestingUserId) {
		// logout user with ownership check
		User loggedOutUser = userService.logoutUser(id, requestingUserId);

		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntitytoUserAuthDTO(loggedOutUser);
	}

	//Endpoint to get all courses of a respective student/teacher
	@GetMapping("/users/{id}/courses")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CourseGetDTO> getUserCourses(@PathVariable Long id, @RequestHeader("Authorization") String token){
		
		//Get a list with all courses
		List<Course> courses = userService.getCoursesByUser(id, token);

		//Transform to DTOs
		List<CourseGetDTO> courseGetDTOs = new ArrayList<>();
		for (Course course : courses){
			courseGetDTOs.add(DTOMapper.INSTANCE.convertEntitiytoCourseGetDTO(course));
		}
		return courseGetDTOs;
	}

}