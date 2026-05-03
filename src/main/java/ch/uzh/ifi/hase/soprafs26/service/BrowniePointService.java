package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.BrowniePointEntryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderBoardEntryGetDTO;
import jakarta.transaction.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;

@Service
@Transactional
public class BrowniePointService {

    private final Logger log = LoggerFactory.getLogger(BrowniePointService.class);

    private final BrowniePointEntryRepository browniePointEntryRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;

    public BrowniePointService(@Qualifier("browniePointEntryRepository") BrowniePointEntryRepository browniePointEntryRepository,
                               @Qualifier("userRepository") UserRepository userRepository,
                               @Qualifier("courseRepository") CourseRepository courseRepository,
                               @Qualifier("sessionRepository") SessionRepository sessionRepository) {
        this.browniePointEntryRepository = browniePointEntryRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
    }


    public void awardBrowniePoints(Long courseId, Long studentId, Long sessionId, int points, String token) {

        //Validate teacher
        User teacher = getUserByToken(token);
        validateTeacher(teacher);

        //Fetch the corresponding student
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        //Fetch the corresponding course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        //Fetch the corresponding session
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        //Build the new entry
        BrowniePointEntry entry = new BrowniePointEntry();
        entry.setStudent(student);
        entry.setCourse(course);
        entry.setSession(session);
        entry.setPoints(points);
        entry.setTimestamp(LocalDateTime.now());

        browniePointEntryRepository.save(entry);

        log.debug("Awarded {} points to student {} in course {}", points, studentId, courseId);
    }


    public List<LeaderBoardEntryGetDTO> getLeaderboard(Long courseId) {
        List<Object[]> results = browniePointEntryRepository.findLeaderboardByCourseId(courseId);

        return results.stream().map(row -> {
            User student = (User) row[0];
            Long totalPoints = (Long) row[1];

            LeaderBoardEntryGetDTO dto = new LeaderBoardEntryGetDTO();
            dto.setUserId(student.getId());
            dto.setUsername(student.getUsername());
            dto.setFirstName(student.getFirstName());
            dto.setLastName(student.getLastName());
            dto.setTotalPoints(totalPoints);

            return dto;
        }).toList();
    }


    private User getUserByToken(String token) {
        return userRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
    }

    private void validateTeacher(User user) {
        if (user.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can award points");
        }

    }
}
