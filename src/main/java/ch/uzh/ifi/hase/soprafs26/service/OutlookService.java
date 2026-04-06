package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs26.entity.Course;

// generates course invitation emails that can be copy-pasted and sent manually

@Service
@Transactional
public class OutlookService {

    private final Logger log = LoggerFactory.getLogger(OutlookService.class);

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;


    public String generateCourseEmailPreview(Course course) {
        return buildCourseEmailContent(course);
    }

    private String buildCourseEmailContent(Course course) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "    <style>" +
                        "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        "        .container { max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }" +
                        "        h2 { color: #0078d4; }" +
                        "        h3 { color: #333; margin-top: 20px; }" +
                        "        .course-details { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0; }" +
                        "        .detail-row { margin: 10px 0; }" +
                        "        .label { font-weight: bold; color: #0078d4; }" +
                        "        .button { display: inline-block; background-color: #0078d4; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin-top: 15px; }" +
                        "        .button:hover { background-color: #106ebe; }" +
                        "        .footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #ddd; font-size: 12px; color: #666; }" +
                        "    </style>" +
                        "</head>" +
                        "<body>" +
                        "    <div class=\"container\">" +
                        "        <h2>Course Invitation</h2>" +
                        "        <p>You have been invited to join a course!</p>" +
                        "        " +
                        "        <div class=\"course-details\">" +
                        "            <h3>%s</h3>" +
                        "            " +
                        "            <div class=\"detail-row\">" +
                        "                <span class=\"label\">Description:</span><br/>" +
                        "                %s" +
                        "            </div>" +
                        "            " +
                        "            <div class=\"detail-row\">" +
                        "                <span class=\"label\">Course Code:</span><br/>" +
                        "                %s" +
                        "            </div>" +
                        "            " +
                        "            <div class=\"detail-row\">" +
                        "                <span class=\"label\">Instructor:</span><br/>" +
                        "                %s %s" +
                        "            </div>" +
                        "        </div>" +
                        "        " +
                        "        <p>" +
                        "            <a href=\"%s/courses/%d\" class=\"button\">View Course</a>" +
                        "        </p>" +
                        "        " +
                        "        <div class=\"footer\">" +
                        "            <p>This is an automated message. Please do not reply to this email.</p>" +
                        "        </div>" +
                        "    </div>" +
                        "</body>" +
                        "</html>",
                course.getTitle(),
                course.getDescription(),
                course.getCourseCode(),
                course.getTeacher().getFirstName(),
                course.getTeacher().getLastName(),
                baseUrl,
                course.getId()
        );
    }
}

