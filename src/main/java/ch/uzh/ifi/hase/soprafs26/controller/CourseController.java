package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CoursePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CourseGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.CourseService;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;


@RestController
public class CourseController {
    
    private final CourseService courseService;

	CourseController(CourseService courseService) {
		this.courseService = courseService;
	}


	@PostMapping("/courses")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public CourseGetDTO createCourse(@RequestBody CoursePostDTO coursePostDTO) {

        Course courseInput = DTOMapper.INSTANCE.convertCoursePostDTOtoEntity(coursePostDTO);

        Course createdCourse = courseService.newCourse(courseInput, coursePostDTO.getTeacherId());

        return DTOMapper.INSTANCE.convertEntitiytoCourseGetDTO(createdCourse);
    }

    // Generates and returns a QR code image for a course.
    @GetMapping("/courses/{courseId}/qr")
    @ResponseStatus(HttpStatus.OK)
    public byte[] getQRCode(@PathVariable Long courseId) {
        Course course = courseService.getCourseById(courseId);
        return courseService.generateQRCode(course);
    }
}
