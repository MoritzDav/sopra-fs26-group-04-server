package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Course;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OutlookServiceTest {

    private OutlookService outlookService;
    private Course course;

    @BeforeEach
    public void setup() {
        outlookService = new OutlookService();

        User teacher = new User();
        teacher.setFirstName("Prof");
        teacher.setLastName("Smith");

        course = new Course();
        course.setId(1L);
        course.setTitle("Math 101");
        course.setDescription("Introduction to Math");
        course.setCourseCode("ABC123");
        course.setTeacher(teacher);
    }

    @Test
    void generateCourseEmailPreview_containsTitle() {
        String result = outlookService.generateCourseEmailPreview(course);
        assertTrue(result.contains("Math 101"));
    }

    @Test
    void generateCourseEmailPreview_containsCourseCode() {
        String result = outlookService.generateCourseEmailPreview(course);
        assertTrue(result.contains("ABC123"));
    }

    @Test
    void generateCourseEmailPreview_containsTeacherName() {
        String result = outlookService.generateCourseEmailPreview(course);
        assertTrue(result.contains("Prof"));
        assertTrue(result.contains("Smith"));
    }

    @Test
    void generateCourseEmailPreview_containsDescription() {
        String result = outlookService.generateCourseEmailPreview(course);
        assertTrue(result.contains("Introduction to Math"));
    }

    @Test
    void generateCourseEmailPreview_returnsHtml() {
        String result = outlookService.generateCourseEmailPreview(course);
        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("</html>"));
    }
}
