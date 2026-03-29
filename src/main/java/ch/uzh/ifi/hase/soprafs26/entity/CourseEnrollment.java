package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

//student's enrollment in a course; which students are enrolled in which courses and when they joined

@Entity
@Table(name = "course_enrollments")
public class CourseEnrollment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private LocalDateTime joinedDate;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public LocalDateTime getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(LocalDateTime joinedDate) {
        this.joinedDate = joinedDate;
    }
}
