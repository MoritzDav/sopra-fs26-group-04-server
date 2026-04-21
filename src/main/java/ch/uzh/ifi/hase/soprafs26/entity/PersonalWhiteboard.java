package ch.uzh.ifi.hase.soprafs26.entity;


import jakarta.persistence.*;

@Entity
@Table(name = "personal_whiteboards")
public class PersonalWhiteboard extends Whiteboard {

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private boolean isVisible;

    @ManyToOne
    @JoinColumn(name = "background_content_id")
    private TeacherWhiteboard backgroundContent;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    // getters and setters

    public User getOwner() {
        return owner;
    }
    public void setOwner(User owner) {
        this.owner = owner;
    }

    public boolean isVisible() {
        return isVisible;
    }
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public TeacherWhiteboard getBackgroundContent() {
        return backgroundContent;
    }
    public void setBackgroundContent(TeacherWhiteboard backgroundContent) {
        this.backgroundContent = backgroundContent;
    }

    public Session getSession() {
        return session;
    }
    public void setSession(Session session) {
        this.session = session;
    }
}
