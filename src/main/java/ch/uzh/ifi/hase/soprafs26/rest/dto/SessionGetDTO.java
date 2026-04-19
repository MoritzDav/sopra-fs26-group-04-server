package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;

import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;

public class SessionGetDTO {
    
    private Long sessionId;
    private String title;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime start;
    private Long selectedWhiteboardId;
    private SessionMode mode;
    private Long courseId;
    private Long teacherWhiteboardId;

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

    public Boolean getActive() {
        return active;
    }
    public void setActive(Boolean active) {
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

    public Long getSelectedWhiteboardId() {
        return selectedWhiteboardId;
    }
    public void setSelectedWhiteboardId(Long selectedWhiteboardId) {
        this.selectedWhiteboardId = selectedWhiteboardId;
    }

    public SessionMode getMode() {
        return mode;
    }
    public void setMode(SessionMode mode) {
        this.mode = mode;
    }

    public Long getCourseId() {
        return courseId;
    }
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getTeacherWhiteboardId() {
        return teacherWhiteboardId;
    }
    public void setTeacherWhiteboardId(Long teacherWhiteboardId) {
        this.teacherWhiteboardId = teacherWhiteboardId;
    }

}
