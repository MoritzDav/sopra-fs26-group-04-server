package ch.uzh.ifi.hase.soprafs26.repository;


import ch.uzh.ifi.hase.soprafs26.entity.BrowniePointEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("browniePointEntryRepository")
public interface BrowniePointEntryRepository extends JpaRepository<BrowniePointEntry, Long> {

    //Entries of one course
    List<BrowniePointEntry> findByCourseId(Long courseId);

    //Entries of a student in a course
    List<BrowniePointEntry> findByStudentIdAndCourseId(Long studentId, Long courseId);

    //Leaderboard query
    @Query("SELECT b.student, SUM(b.points) as total " +
            "FROM BrowniePointEntry b " +
            "WHERE b.course.id = :courseId " +
            "GROUP BY b.student " +
            "ORDER BY total DESC")
    List<Object[]> findLeaderboardByCourseId(@Param("courseId") Long courseId);

}

