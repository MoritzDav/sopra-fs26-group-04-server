package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import ch.uzh.ifi.hase.soprafs26.entity.*;

@Repository("sessionRepository")
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Override
    @EntityGraph(attributePaths = {"teacherWhiteboard", "teacherWhiteboard.currentPage", "course", "course.teacher"})
    Optional<Session> findById(Long sessionId);
    
    Session findByTitle(String title);
    List<Session> findByCourseId(Long courseId);
    void deleteByCourseId(Long courseId);

}
