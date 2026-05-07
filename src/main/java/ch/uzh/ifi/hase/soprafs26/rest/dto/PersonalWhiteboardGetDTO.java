package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class PersonalWhiteboardGetDTO {

    private Long whiteboardId;
    private String canvasSnapshot;


    //Getters and Setters
    public Long getWhiteboardId() { return whiteboardId; }
    public void setWhiteboardId(Long whiteboardId) { this.whiteboardId = whiteboardId; }

    public String getCanvasSnapshot() { return canvasSnapshot; }
    public void setCanvasSnapshot(String canvasSnapshot) { this.canvasSnapshot = canvasSnapshot; }
}
