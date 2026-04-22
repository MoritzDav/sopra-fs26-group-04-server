package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import java.util.List;


@Repository("courseRepository")
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    Course findByTitle(String title);

    Course findByCourseCode(String courseCode);

    Course findByTeacher(User teacher);

    List<Course> findByTeacherId(Long teacherID);

}
