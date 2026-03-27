package ch.uzh.ifi.hase.soprafs26.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.repository.CourseRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.User;




@Service
@Transactional
public class CourseService {
 
    private final Logger log = LoggerFactory.getLogger(CourseService.class);

	private final CourseRepository courseRepository;
    private final UserRepository userRepository;

	public CourseService(@Qualifier("courseRepository") CourseRepository courseRepository, @Qualifier("userRepository") UserRepository userRepository) {
		this.courseRepository = courseRepository;
        this.userRepository = userRepository;
	}

    public Course newCourse(Course newCourse, Long teacherId) {
        
        //Fetch full user from database
        User teacher = userRepository.findById(teacherId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User/teacher not found"));

        //Check whether user is a teacher
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers are allowed to create a new course");
        }
        
        newCourse.setTeacher(teacher);

        newCourse.setCourseCode("abcdef");

        newCourse = courseRepository.save(newCourse);
        userRepository.flush();

        log.debug("Created Information for Course: {}", newCourse);
        return newCourse;
    }


}
