package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

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
}
