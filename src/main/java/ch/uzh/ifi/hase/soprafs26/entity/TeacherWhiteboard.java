package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "teacher_whiteboards")
public class TeacherWhiteboard extends Whiteboard {

    @Column(nullable = false)
    private boolean isShared;

    @OneToOne
    @JoinColumn(name = "current_page_id")
    private WhiteboardPage currentPage;

    @OneToOne(mappedBy = "teacherWhiteboard")
    private Session session;
    

    //getters and setters
    public boolean isShared() { 
        return isShared;
    }
    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    public WhiteboardPage getCurrentPage() {
        return currentPage;
    }
    public void setCurrentPage(WhiteboardPage currentPage) {
        this.currentPage = currentPage;
    }

    public Session getSession() {
        return session;
    }
    public void setSession(Session session) {
        this.session = session;
    }
}
