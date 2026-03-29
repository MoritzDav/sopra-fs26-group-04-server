package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    
    List<CourseEnrollment> findByCourseId(Long courseId);
    
    List<CourseEnrollment> findByStudentId(Long studentId);

    Optional<CourseEnrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
}
