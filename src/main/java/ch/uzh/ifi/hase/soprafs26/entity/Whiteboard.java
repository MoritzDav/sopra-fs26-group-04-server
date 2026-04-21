package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import ch.uzh.ifi.hase.soprafs26.constant.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "whiteboards")
public abstract class Whiteboard implements Serializable{

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long whiteboardId;

    @Column(nullable = false)
    private boolean locked;

    @Enumerated(EnumType.STRING)
    private WhiteboardMode mode;

    @Column(nullable = false)
    private boolean teacherLayerReadOnly;

    @OneToMany(mappedBy = "whiteboard", cascade = CascadeType.ALL)
    private List<WhiteboardPage> pages = new ArrayList<>();


    
    //getters and setters
    public Long getWhiteboardId() {
        return whiteboardId;
    }
    public void setWhiteboardId(Long whiteboardId) {
        this.whiteboardId = whiteboardId;
    }

    public boolean isLocked() {
        return locked;
    }
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public WhiteboardMode getMode() {
        return mode;
    }
    public void setMode(WhiteboardMode mode) {
        this.mode = mode;
    }

    public boolean isTeacherLayerReadOnly() {
        return teacherLayerReadOnly;
    }
    public void setTeacherLayerReadOnly(boolean teacherLayerReadOnly) {
        this.teacherLayerReadOnly = teacherLayerReadOnly;
    }

    public List<WhiteboardPage> getPages() {
        return pages;
    }
    public void setPages(List<WhiteboardPage> pages) {
        this.pages = pages;
    }

    public void addPage(WhiteboardPage page){
        this.pages.add(page);
        page.setWhiteboard(this);
    }
}
