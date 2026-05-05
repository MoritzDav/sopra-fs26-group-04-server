package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class SessionBoardDeselectDTO {
    private Long studentId;
    private String canvasSnapshot;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getCanvasSnapshot() { return canvasSnapshot; }
    public void setCanvasSnapshot(String canvasSnapshot) { this.canvasSnapshot = canvasSnapshot; }
}
