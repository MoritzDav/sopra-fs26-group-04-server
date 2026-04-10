package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;

import java.io.Serializable;
import java.time.LocalDateTime;


import jakarta.persistence.*;

@Entity
@Table(name = "Sessions")
public class Session implements Serializable{
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long sessionId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime start;

    @ManyToOne
    @JoinColumn(name = "selected_whiteboard_id")
    private PersonalWhiteboard selectedWhiteboard;

    @Enumerated(EnumType.STRING)
    private SessionMode mode;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course; 

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "teacher_whiteboard_id")
    private TeacherWhiteboard teacherWhiteboard;

    public Long getSessionId() { 
        return sessionId; 
    }

    public void setSessionId(Long sessionId) { 
        this.sessionId = sessionId; 
    }

    public String getTitle() { 
        return title; 
    }

    public void setTitle(String title) { 
        this.title = title; 
    }

    public boolean isActive() { 
        return active; 
    }

    public void setActive(boolean active) { 
        this.active = active; 
    }

    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }
    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }

    public LocalDateTime getStart() { 
        return start; 
    }
    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public PersonalWhiteboard getSelectedWhiteboard() { 
        return selectedWhiteboard; 
    }
    public void setSelectedWhiteboard(PersonalWhiteboard selectedWhiteboard) { 
        this.selectedWhiteboard = selectedWhiteboard; 
    }

    public SessionMode getMode() { 
        return mode; 
    }
    public void setMode(SessionMode mode) { 
        this.mode = mode; 
    }

    public Course getCourse() { 
        return course; 
    }
    public void setCourse(Course course) { 
        this.course = course; 
    }

    public TeacherWhiteboard getTeacherWhiteboard() {
        return teacherWhiteboard;
    }
    public void setTeacherWhiteboard(TeacherWhiteboard teacherWhiteboard) { 
        this.teacherWhiteboard = teacherWhiteboard;
    }
}

