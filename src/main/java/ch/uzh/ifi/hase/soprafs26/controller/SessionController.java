package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionGetDTO;
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

    @PutMapping("/courses/{courseId}/sessions/{sessionId}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endSession(@PathVariable Long courseId, @PathVariable Long sessionId, @RequestHeader("Authorization") String token) {
        sessionService.endSession(sessionId, token);
    }
}
