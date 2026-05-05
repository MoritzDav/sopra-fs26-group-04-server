package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class BrowniePointsPostDTO {
    private Long studentId;
    private int points;


    public Long getStudentId() {
        return studentId;
    }
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public int getPoints() {
        return points;
    }
    public void setPoints(int points) {
        this.points = points;
    }
}
