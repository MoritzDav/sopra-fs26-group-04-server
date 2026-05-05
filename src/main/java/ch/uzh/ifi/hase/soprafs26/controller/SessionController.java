package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.PersonalWhiteboard;
import jakarta.persistence.PreUpdate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.SessionService;

@RestController
public class SessionController{
    
    private final SessionService sessionService;

    SessionController(SessionService sessionService){
        this.sessionService = sessionService;
    }

    @PostMapping("courses/{courseId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public SessionGetDTO startSession(@PathVariable Long courseId, @RequestHeader("Authorization") String token, @RequestBody SessionPostDTO sessionPostDTO){
        

        //Convert API-input to internal representation
        Session sessionInput = DTOMapper.INSTANCE.convertSessionPostDTOtoEntity(sessionPostDTO);

        //start a session via sessionService with PostDTO as input
        Session session = sessionService.startSession(courseId, token, sessionInput);

        //return getDTO
        return DTOMapper.INSTANCE.convertSessionEntityToSessionGetDTO(session);
    }

    @GetMapping("courses/{courseId}/sessions")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<SessionGetDTO> getSessionsByCourse(@PathVariable Long courseId, @RequestHeader("Authorization") String token) {
        List<Session> sessions = sessionService.getSessionsByCourse(courseId, token);
        List<SessionGetDTO> sessionGetDTOs = new ArrayList<>();
        for (Session session : sessions) {
            sessionGetDTOs.add(DTOMapper.INSTANCE.convertSessionEntityToSessionGetDTO(session));
        }
        return sessionGetDTOs;
    }

    @PutMapping("/courses/{courseId}/sessions/{sessionId}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endSession(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        sessionService.endSession(sessionId, token);
    }

    @PostMapping("/courses/{courseId}/sessions/{sessionId}/join")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public PersonalWhiteboardGetDTO joinSession(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token){
        PersonalWhiteboard whiteboard = sessionService.joinSession(courseId, sessionId, token);
        return DTOMapper.INSTANCE.convertPersonalWhiteboardToGetDTO(whiteboard);
    }

    @GetMapping("/courses/{courseId}/sessions/{sessionId}/whiteboard")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public WhiteboardStateDTO getWhiteboardState(@PathVariable Long courseId, @PathVariable Long sessionId) {
        return sessionService.getWhiteboardState(sessionId);
    }

    @PutMapping("/courses/{courseId}/sessions/{sessionId}/whiteboard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveWhiteboardState(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token,
            @RequestBody WhiteboardStateDTO dto) {
        sessionService.saveWhiteboardState(sessionId, token, dto.getCanvasSnapshot());
    }

    //Student saves his snapshot
    @PutMapping("/courses/{courseId}/sessions/{sessionId}/personal-whiteboard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void savePersonalWhiteboardState(@PathVariable Long courseId,
                                            @PathVariable Long sessionId,
                                            @RequestHeader("Authorization") String token,
                                            @RequestBody WhiteboardStateDTO dto) {
        sessionService.savePersonalWhiteboardState(courseId, sessionId, token, dto.getCanvasSnapshot());
    }

    //Teacher fetches student snapshot
    @GetMapping("/courses/{courseId}/sessions/{sessionId}/students/{studentId}/whiteboard")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public WhiteboardStateDTO getStudentWhiteboardState(@PathVariable Long courseId,
                                                        @PathVariable Long sessionId,
                                                        @PathVariable Long studentId) {
        return sessionService.getStudentWhiteboardState(sessionId, studentId);
    }


    //Teacher selects student whiteboard
    @PutMapping("/courses/{courseId}/sessions/{sessionId}/selected-board")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectStudentBoard(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token,
            @RequestBody SessionBoardSelectDTO dto) {
        sessionService.selectStudentBoard(courseId, sessionId, token, dto.getStudentId());
    }

    //Teacher deselects student whiteboard
    @PutMapping("/courses/{courseId}/sessions/{sessionId}/deselect-board")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deselectStudentBoard(
            @PathVariable Long courseId,
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token,
            @RequestBody SessionBoardDeselectDTO dto){
        sessionService.deselectStudentBoard(courseId, sessionId, token, dto.getStudentId(), dto.getCanvasSnapshot());
    }

}
