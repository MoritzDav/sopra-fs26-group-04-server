package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CourseGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CoursePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessagePostDTO;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(source = "firstName", target = "firstName")
	@Mapping(source = "lastName", target = "lastName")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "password")
	@Mapping(target = "role", expression = "java(mapRole(userPostDTO.getRole()))")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	//Helper function to deal with the mapping of Role input as a String and output as enumeration type
	default UserRole mapRole(String role){
		if (role == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Role cannot be null");
		role = role.trim().toUpperCase();
		return switch(role){
			case "TEACHER" -> UserRole.TEACHER;
			case "STUDENT" -> UserRole.STUDENT;
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role:" + role + " must be Teacher/Student.");
		};
	}


	@Mapping(source = "id", target = "id")
	@Mapping(source = "firstName", target = "firstName")
	@Mapping(source = "lastName", target = "lastName")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "role", target = "role")	
	UserAuthDTO convertEntitytoUserAuthDTO(User user);


	@Mapping(source = "title", target = "title")
	@Mapping(source = "description", target = "description")
	@Mapping(source = "pictureURL", target = "pictureURL")
	@Mapping(target = "teacher", ignore = true)
	@Mapping(target = "courseCode", ignore = true) 
	Course convertCoursePostDTOtoEntity(CoursePostDTO coursePostDTO);



	@Mapping(source = "id", target = "id")
	@Mapping(source = "title", target = "title")
	@Mapping(source = "description", target = "description")
	@Mapping(source = "pictureURL", target = "pictureURL")
	@Mapping(source = "courseCode", target = "courseCode")
	@Mapping(source = "teacher.id", target = "teacherId")
	CourseGetDTO convertEntitiytoCourseGetDTO(Course course);



	//Session mappings

	@Mapping(source = "title", target = "title")
	@Mapping(target = "mode", ignore = true)
	@Mapping(target = "course", ignore = true)
	Session convertSessionPostDTOtoEntity(SessionPostDTO sessionPostDTO);

	@Mapping(source = "sessionId", target = "sessionId")
	@Mapping(source = "title", target = "title")
	@Mapping(source = "active", target = "active")
	@Mapping(source = "createdAt", target = "createdAt")
	@Mapping(source = "start", target = "start")
	@Mapping(source = "mode", target = "mode")
	@Mapping(source = "course.id", target = "courseId")
	@Mapping(source = "teacherWhiteboard.whiteboardId", target = "teacherWhiteboardId")
	@Mapping(source = "selectedWhiteboard.whiteboardId", target = "selectedWhiteboardId")
	SessionGetDTO convertSessionEntityToSessionGetDTO(Session session);
	
	//ChatMessage mappings

	@Mapping(source = "messageId", target = "messageId")
	@Mapping(source = "session.sessionId", target = "sessionId")
	@Mapping(source = "user.id", target = "userId")
	@Mapping(source = "user.username", target = "username")
	@Mapping(source = "content", target = "content")
	@Mapping(source = "timestamp", target = "timestamp")
	ChatMessageGetDTO convertChatMessageEntityToGetDTO(ChatMessage chatMessage);
	
}

