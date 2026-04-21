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
import ch.uzh.ifi.hase.soprafs26.rest.dto.CoursePutDTO;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import java.util.Random;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;

import org.springframework.beans.factory.annotation.Value;

@Service
@Transactional
public class CourseService {
 
    private final Logger log = LoggerFactory.getLogger(CourseService.class);

	private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

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

        newCourse.setCourseCode(generateUniqueCourseCode());

        newCourse = courseRepository.save(newCourse);
        courseRepository.flush();

        log.debug("Created Information for Course: {}", newCourse);
        return newCourse;
    }

    //Update course credentials i.e. title, description, picture
    public void updateCourse(Long courseId, String token, CoursePutDTO coursePutDTO){
    
        //Fetch the requesting user from the DB
        User requestingUser = userRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User/teacher not found"));
    
        //Fetch the respective Course from the DB
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        //See if requesting user matches course teacher
        if (!course.getTeacher().getId().equals(requestingUser.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of the course");
        }

        //Change the respecting fields if they exist in the DTO
        if (coursePutDTO.getTitle() != null) course.setTitle(coursePutDTO.getTitle());
        if (coursePutDTO.getDescription() != null) course.setDescription(coursePutDTO.getDescription());
        if (coursePutDTO.getPictureURL() != null) course.setPictureURL(coursePutDTO.getPictureURL());  

        course = courseRepository.save(course);
        courseRepository.flush();

        log.debug("Updated course informations for Course: {}", course);    
    }

    //Delete a course as the corresponding teacher
    public void deleteCourse(Long courseId, String token){
        
        //Fetch the requesting user from the DB
        User requestingUser = userRepository.findByToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User/teacher not found"));

        //Fetch the respective Course from the DB
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        //See if requesting user matches course teacher
        if (!course.getTeacher().getId().equals(requestingUser.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of the course");
        }

        courseRepository.delete(course);

        log.debug("Deletec course: {}", course);    
    
    }
   
    private String generateUniqueCourseCode() {
        String courseCode;
        boolean isUnique;

        do {
            courseCode = generateRandomCode();
            isUnique = courseRepository.findByCourseCode(courseCode) == null;
        } while (!isUnique);

        return courseCode;
    }

   
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return code.toString();
    }


    // Retrieves a course by its ID from the database.
    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    // Retrieves a course by its course code from the database.
    public Course getCourseByCourseCode(String courseCode) {
        Course course = courseRepository.findByCourseCode(courseCode);
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course code not found");
        }
        return course;
    }

    // Generates a QR code for the given course that redirects to the course page.
    public byte[] generateQRCode(Course course) {
        try {
            // Create a URL that points to the course page
            String courseUrl = String.format(
                "%s/courses/%d",
                baseUrl,
                course.getId()
            );

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(courseUrl, BarcodeFormat.QR_CODE, 300, 300);
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate QR code: " + e.getMessage());
        }
    }
}
