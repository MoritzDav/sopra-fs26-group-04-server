package ch.uzh.ifi.hase.soprafs26.controller;


import ch.uzh.ifi.hase.soprafs26.rest.dto.BrowniePointsPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderBoardEntryGetDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.service.BrowniePointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BrowniePointController {

    private final BrowniePointService browniePointService;

    BrowniePointController (BrowniePointService browniePointService){
        this.browniePointService = browniePointService;
    }


    //Get the leaderboard for a specific course
    @GetMapping("/courses/{courseId}/leaderboard")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<LeaderBoardEntryGetDTO> getLeaderboard(@PathVariable Long courseId) {
        return browniePointService.getLeaderboard(courseId);
    }

    //As a teacher distribute brownie points
    @PostMapping("/courses/{courseId}/sessions/{sessionId}/browniepoints")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void awardBrowniePoints(@PathVariable Long courseId,
                                   @PathVariable Long sessionId,
                                   @RequestBody BrowniePointsPostDTO browniePointsPostDTO,
                                   @RequestHeader("Authorization") String token) {
        Long studentId = browniePointsPostDTO.getStudentId();
        int points = browniePointsPostDTO.getPoints();
        browniePointService.awardBrowniePoints(courseId, studentId, sessionId, points, token);
    }

}
