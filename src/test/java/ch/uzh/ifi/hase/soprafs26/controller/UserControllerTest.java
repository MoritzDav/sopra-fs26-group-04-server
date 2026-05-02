package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;


    /**
     * POST /users
     */

    @Test
    public void createUser_validInput_returns201() throws Exception {
        // given
        UserPostDTO dto = new UserPostDTO();
        dto.setFirstName("Alice");
        dto.setLastName("Wonder");
        dto.setUsername("alice");
        dto.setPassword("secret");
        dto.setRole("STUDENT");

        User created = new User();
        created.setId(1L);
        created.setFirstName("Alice");
        created.setLastName("Wonder");
        created.setUsername("alice");
        created.setToken("some-token");
        created.setStatus(UserStatus.ONLINE);
        created.setRole(UserRole.STUDENT);

        given(userService.createUser(any(User.class))).willReturn(created);

        // when
        MockHttpServletRequestBuilder request = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.firstName", is("Alice")))
                .andExpect(jsonPath("$.status", is("ONLINE")))
                .andExpect(jsonPath("$.role", is("STUDENT")));
    }

    @Test
    public void createUser_duplicateUsername_returns409() throws Exception {
        // given
        UserPostDTO dto = new UserPostDTO();
        dto.setFirstName("Alice");
        dto.setLastName("Wonder");
        dto.setUsername("alice");
        dto.setPassword("secret");
        dto.setRole("STUDENT");

        given(userService.createUser(any(User.class)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists!"));

        // when
        MockHttpServletRequestBuilder request = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isConflict());
    }

    @Test
    public void createUser_missingFirstName_returns400() throws Exception {
        // given — @NotBlank on firstName triggers validation failure
        UserPostDTO dto = new UserPostDTO();
        dto.setFirstName("");
        dto.setLastName("Wonder");
        dto.setUsername("alice");
        dto.setPassword("secret");
        dto.setRole("STUDENT");

        // when
        MockHttpServletRequestBuilder request = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }


    /**
     * POST /users/login
     */

    @Test
    public void loginUser_validCredentials_returns200() throws Exception {
        // given
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("secret");

        User loggedIn = new User();
        loggedIn.setId(1L);
        loggedIn.setFirstName("Alice");
        loggedIn.setLastName("Wonder");
        loggedIn.setUsername("alice");
        loggedIn.setToken("some-token");
        loggedIn.setStatus(UserStatus.ONLINE);
        loggedIn.setRole(UserRole.STUDENT);

        given(userService.loginUser("alice", "secret")).willReturn(loggedIn);

        // when
        MockHttpServletRequestBuilder request = post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.token", is("some-token")))
                .andExpect(jsonPath("$.status", is("ONLINE")));
    }

    @Test
    public void loginUser_invalidCredentials_returns401() throws Exception {
        // given
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("wrongpassword");

        given(userService.loginUser("alice", "wrongpassword"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        // when
        MockHttpServletRequestBuilder request = post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }


    /**
     * GET /users/{id}
     */

    @Test
    public void getUser_validId_returns200() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setFirstName("Alice");
        user.setLastName("Wonder");
        user.setUsername("alice");
        user.setToken("some-token");
        user.setStatus(UserStatus.ONLINE);
        user.setRole(UserRole.STUDENT);

        given(userService.getUserById(1L)).willReturn(user);

        // when
        MockHttpServletRequestBuilder request = get("/users/1");

        // then
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.firstName", is("Alice")));
    }

    @Test
    public void getUser_invalidId_returns404() throws Exception {
        // given
        given(userService.getUserById(99L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // when
        MockHttpServletRequestBuilder request = get("/users/99");

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * PUT /users/{id}
     */

    //Update user with a valid request
    @Test
    public void updateUser_validRequest_returns200() throws Exception {
        // given
        UserPutDTO dto = new UserPutDTO();
        dto.setFirstName("NewName");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setFirstName("NewName");
        updatedUser.setLastName("Wonder");
        updatedUser.setUsername("alice");
        updatedUser.setToken("some-token");
        updatedUser.setStatus(UserStatus.ONLINE);
        updatedUser.setRole(UserRole.STUDENT);

        given(userService.updateUser(eq(1L), any(UserPutDTO.class))).willReturn(updatedUser);

        // when
        MockHttpServletRequestBuilder request = put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("NewName")));
    }


    //Update user with an invalid
    @Test
    public void updateUser_userNotFound_returns404() throws Exception {
        // given
        UserPutDTO dto = new UserPutDTO();
        dto.setFirstName("NewName");

        given(userService.updateUser(eq(99L), any(UserPutDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // when
        MockHttpServletRequestBuilder request = put("/users/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateUser_conflictingUsername_returns409() throws Exception {
        // given
        UserPutDTO dto = new UserPutDTO();
        dto.setUsername("existinguser");

        given(userService.updateUser(eq(1L), any(UserPutDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists!"));

        // when
        MockHttpServletRequestBuilder request = put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isConflict());
    }

    @Test
    public void updateUser_wrongPassword_returns401() throws Exception {
        // given
        UserPutDTO dto = new UserPutDTO();
        dto.setOldPassword("wrongpassword");
        dto.setNewPassword("newpassword");

        given(userService.updateUser(eq(1L), any(UserPutDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect"));

        // when
        MockHttpServletRequestBuilder request = put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto));

        // then
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    /**
     * POST /users/logout
     */

    @Test
    public void logoutUser_validRequest_returns200() throws Exception {
        // given
        User loggedOut = new User();
        loggedOut.setId(1L);
        loggedOut.setFirstName("Alice");
        loggedOut.setLastName("Wonder");
        loggedOut.setUsername("alice");
        loggedOut.setToken("some-token");
        loggedOut.setStatus(UserStatus.OFFLINE);
        loggedOut.setRole(UserRole.STUDENT);

        given(userService.logoutUser("some-token")).willReturn(loggedOut);

        // when
        MockHttpServletRequestBuilder request = post("/users/logout")
                .header("Authorization", "some-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OFFLINE")));
    }

    @Test
    public void logoutUser_invalidToken_returns404() throws Exception {
        // given
        given(userService.logoutUser("invalid-token"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You can only logout your own account"));

        // when
        MockHttpServletRequestBuilder request = post("/users/logout")
                .header("Authorization", "invalid-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    /**
     * GET /users/{id}/courses
     */


    @Test
    public void getUserCourses_validRequest_returns200() throws Exception {
        // given
        User teacher = new User();
        teacher.setId(1L);

        Course course = new Course();
        course.setId(10L);
        course.setTitle("Math 101");
        course.setCourseCode("XYZ999");
        course.setTeacher(teacher);

        given(userService.getCoursesByUser(eq(1L), eq("some-token")))
                .willReturn(java.util.List.of(course));

        // when
        MockHttpServletRequestBuilder request = get("/users/1/courses")
                .header("Authorization", "some-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].title", is("Math 101")));
    }

    @Test
    public void getUserCourses_invalidToken_returns401() throws Exception {
        // given
        given(userService.getCoursesByUser(eq(1L), eq("invalid-token")))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when
        MockHttpServletRequestBuilder request = get("/users/1/courses")
                .header("Authorization", "invalid-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getUserCourses_userNotFound_returns404() throws Exception {
        // given
        given(userService.getCoursesByUser(eq(99L), eq("some-token")))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // when
        MockHttpServletRequestBuilder request = get("/users/99/courses")
                .header("Authorization", "some-token");

        // then
        mockMvc.perform(request)
                .andExpect(status().isNotFound());
    }



    //Helper function
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}