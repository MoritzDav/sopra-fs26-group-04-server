package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
import jakarta.persistence.*;

@Entity
@Table(name = "whiteboard_pages")
public class WhiteboardPage implements Serializable{

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private int pageNumber;

    @Column
    private String backgroundFile;

    @Lob
    @Column
    private String canvasSnapshot;

    @ManyToOne
    @JoinColumn(name = "whiteboard_id", nullable = false)
    private Whiteboard whiteboard;


    //getters and setters

    public Long getId() { 
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public int getPageNumber() {
        return pageNumber;
    }
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getBackgroundFile() {
        return backgroundFile;
    }
    public void setBackgroundFile(String backgroundFile) {
        this.backgroundFile = backgroundFile;
    }

    public String getCanvasSnapshot() {
        return canvasSnapshot;
    }
    public void setCanvasSnapshot(String canvasSnapshot) {
        this.canvasSnapshot = canvasSnapshot;
    }

    public Whiteboard getWhiteboard() {
        return whiteboard;
    }
    public void setWhiteboard(Whiteboard whiteboard) {
        this.whiteboard = whiteboard;
    }
}

    
