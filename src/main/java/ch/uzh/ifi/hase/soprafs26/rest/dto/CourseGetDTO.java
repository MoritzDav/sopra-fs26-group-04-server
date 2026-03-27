package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class CourseGetDTO {
    
    private Long id;
    private String title;
    private String description;
    private String courseCode;
    private String pictureURL;
    private Long teacherId;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode){
        this.courseCode = courseCode;
    }

	public String getPictureURL() {
		return pictureURL;
	}

	public void setPictureURL(String pictureURL) {
		this.pictureURL = pictureURL;
	}

	public Long getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(Long teacherId) {
		this.teacherId = teacherId;
	}


}
