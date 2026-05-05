package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LeaderBoardEntryGetDTO {

    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private Long totalPoints;


    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getTotalPoints() {
        return totalPoints;
    }
    public void setTotalPoints(Long totalPoints) {
        this.totalPoints = totalPoints;
    }
}
