package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.CourseEnrollment;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CoursePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CourseGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CoursePutDTO;
import ch.uzh.ifi.hase.soprafs26.service.CourseService;
import ch.uzh.ifi.hase.soprafs26.service.CourseEnrollmentService;
import ch.uzh.ifi.hase.soprafs26.service.OutlookService;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;

import java.util.List;


@RestController
public class CourseController {
    
    private final CourseService courseService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final OutlookService outlookService;

	CourseController(CourseService courseService, CourseEnrollmentService courseEnrollmentService, OutlookService outlookService) {
		this.courseService = courseService;
		this.courseEnrollmentService = courseEnrollmentService;
		this.outlookService = outlookService;
	}


    //Creates a new course
	@PostMapping("/courses")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public CourseGetDTO createCourse(@RequestBody CoursePostDTO coursePostDTO) {
        Course courseInput = DTOMapper.INSTANCE.convertCoursePostDTOtoEntity(coursePostDTO);
        Course createdCourse = courseService.newCourse(courseInput, coursePostDTO.getTeacherId());
        return DTOMapper.INSTANCE.convertEntitiytoCourseGetDTO(createdCourse);
    }

    //Gets information of a course
    @GetMapping("/courses/{courseId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CourseGetDTO getCourse(@PathVariable Long courseId) {
		Course course = courseService.getCourseById(courseId);
		return DTOMapper.INSTANCE.convertEntitiytoCourseGetDTO(course);
	}

    //Changes credentials of a course
    @PutMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void updateCourse(@PathVariable Long courseId, 
        @RequestHeader ("Authorization") String token,
        @RequestBody CoursePutDTO coursePutDTO) {
        courseService.updateCourse(courseId, token, coursePutDTO);
    }

    //Deletes a course
    @DeleteMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteCourse(@PathVariable Long courseId,
        @RequestHeader ("Authorization") String token) {
        courseService.deleteCourse(courseId, token);
    }

    // Generates and returns a QR code image for a course.
    @GetMapping("/courses/{courseId}/qr")
    @ResponseStatus(HttpStatus.OK)
    public byte[] getQRCode(@PathVariable Long courseId) {
        Course course = courseService.getCourseById(courseId);
        return courseService.generateQRCode(course);
    }

    // Generates/previews a course invitation email (similar to QR code generation).
    @GetMapping("/courses/{courseId}/email")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String generateCourseEmail(@PathVariable Long courseId) {
        Course course = courseService.getCourseById(courseId);
        return outlookService.generateCourseEmailPreview(course);
    }

    // Enrolls a student in a course using course code.
    @PostMapping("/courses/{courseCode}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public CourseEnrollment enrollStudent(@PathVariable String courseCode, @RequestParam Long studentId) {
        return courseEnrollmentService.enrollStudentByCourseCode(studentId, courseCode);
    }

    // Gets all students enrolled in a course.
    @GetMapping("/courses/{courseCode}/students")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<CourseEnrollment> getStudentsInCourse(@PathVariable String courseCode) {
        // Get course by code to verify it exists and get the ID
        Course course = courseService.getCourseByCourseCode(courseCode);
        return courseEnrollmentService.getStudentsInCourse(course.getId());
    }
}
