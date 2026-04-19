package ch.uzh.ifi.hase.soprafs26.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for whiteboard drawing data sent over WebSocket.
 * Represents a drawing action on the whiteboard by a user.
 */
public class WhiteboardDrawingMessage {

    @JsonProperty("courseId")
    private Long courseId;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("action")
    private String action; // "draw", "clear", "undo", etc.

    @JsonProperty("x")
    private Double x;

    @JsonProperty("y")
    private Double y;

    @JsonProperty("color")
    private String color;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("previousX")
    private Double previousX;

    @JsonProperty("previousY")
    private Double previousY;

    @JsonProperty("timestamp")
    private Long timestamp;

    // Constructors
    public WhiteboardDrawingMessage() {
    }

    public WhiteboardDrawingMessage(Long courseId, Long userId, String action) {
        this.courseId = courseId;
        this.userId = userId;
        this.action = action;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Double getPreviousX() {
        return previousX;
    }

    public void setPreviousX(Double previousX) {
        this.previousX = previousX;
    }

    public Double getPreviousY() {
        return previousY;
    }

    public void setPreviousY(Double previousY) {
        this.previousY = previousY;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
