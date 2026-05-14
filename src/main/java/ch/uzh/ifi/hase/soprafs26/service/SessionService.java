package ch.uzh.ifi.hase.soprafs26.service;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import ch.uzh.ifi.hase.soprafs26.rest.SessionWebSocketHandler;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionBoardSelectDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.repository.*;
import ch.uzh.ifi.hase.soprafs26.constant.SessionMode;
import ch.uzh.ifi.hase.soprafs26.constant.UserRole;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.WhiteboardStateDTO;

@Service
@Transactional
public class SessionService {

    private final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final WhiteboardPageRepository whiteboardPageRepository;
    private final ChatMessageService chatMessageService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final SessionWebSocketHandler sessionWebSocketHandler;
    private final PersonalWhiteboardRepository personalWhiteboardRepository;
    private final SessionFileRepository sessionFileRepository;
    private final GeminiSummaryService geminiSummaryService;


    public SessionService(@Qualifier("sessionRepository") SessionRepository sessionRepository,
                          @Qualifier("courseRepository") CourseRepository courseRepository,
                          @Qualifier("userRepository") UserRepository userRepository,
                          @Qualifier("whiteboardPageRepository") WhiteboardPageRepository whiteboardPageRepository,
                          @Qualifier("chatMessageService") ChatMessageService chatMessageService,
                          @Qualifier("courseEnrollmentRepository") CourseEnrollmentRepository courseEnrollmentRepository,
                          @Qualifier("personalWhiteboardRepository") PersonalWhiteboardRepository personalWhiteboardRepository,
                          @Qualifier("sessionFileRepository") SessionFileRepository sessionFileRepository,
                          SessionWebSocketHandler sessionWebSocketHandler,
                          GeminiSummaryService geminiSummaryService) {
        this.sessionRepository = sessionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.whiteboardPageRepository = whiteboardPageRepository;
        this.chatMessageService = chatMessageService;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.personalWhiteboardRepository = personalWhiteboardRepository;
        this.sessionFileRepository = sessionFileRepository;
        this.sessionWebSocketHandler = sessionWebSocketHandler;
        this.geminiSummaryService = geminiSummaryService;
    }

    //Create and start session
    public Session startSession(Long courseId, String token, Session sessionInput) {

        User user = userRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        if (user.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers are allowed to start a session");
        }

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No course with that courseId found"));

        if (!course.getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not owner of this course");
        }

        Session session = new Session();
        session.setTitle(sessionInput.getTitle());
        session.setMode(SessionMode.NORMAL);
        session.setCourse(course);
        session.setCreatedAt(LocalDateTime.now());
        session.setActive(true);
        session.setStart(LocalDateTime.now());

        TeacherWhiteboard teacherWhiteboard = new TeacherWhiteboard();
        teacherWhiteboard.setShared(false);
        teacherWhiteboard.setLocked(false);
        teacherWhiteboard.setTeacherLayerReadOnly(true);

        WhiteboardPage firstPage = new WhiteboardPage();
        firstPage.setPageNumber(1);
        firstPage.setWhiteboard(teacherWhiteboard);

        teacherWhiteboard.addPage(firstPage);
        teacherWhiteboard.setCurrentPage(firstPage);
        session.setTeacherWhiteboard(teacherWhiteboard);

        session = sessionRepository.save(session);
        sessionRepository.flush();

        log.debug("Created session: {}", session.getSessionId());
        return session;
    }

    //Join session as a student
    public PersonalWhiteboard joinSession(Long courseId, Long sessionId, String token){

        //Fetch user
        User user = getUserByToken(token);
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can join a session");
        }

        //Fetch session
        Session session = getSessionById(sessionId);
        if (!session.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not active");
        }

        boolean isEnrolled = courseEnrollmentRepository
                .findByStudentIdAndCourseId(user.getId(), courseId).isPresent();

        if (!isEnrolled){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this course");
        }

        // Return existing whiteboard if already joined
        Optional<PersonalWhiteboard> existing = personalWhiteboardRepository
                .findByOwnerIdAndSessionSessionId(user.getId(), sessionId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new PersonalWhiteboard
        PersonalWhiteboard whiteboard = new PersonalWhiteboard();
        whiteboard.setOwner(user);
        whiteboard.setSession(session);
        whiteboard.setLocked(false);
        whiteboard.setTeacherLayerReadOnly(true);
        whiteboard.setVisible(true);

        WhiteboardPage firstPage = new WhiteboardPage();
        firstPage.setPageNumber(1);
        firstPage.setWhiteboard(whiteboard);

        whiteboard.getPages().add(firstPage);
        whiteboard.setCurrentPage(firstPage);

        whiteboard = personalWhiteboardRepository.save(whiteboard);
        personalWhiteboardRepository.flush();

        log.debug("Student {} joined session {} and got whiteboard {}", user.getId(), sessionId, whiteboard.getWhiteboardId());
        return whiteboard;

    }


    public WhiteboardStateDTO getWhiteboardState(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        TeacherWhiteboard wb = session.getTeacherWhiteboard();
        if (wb != null && wb.getCurrentPage() != null) {
            dto.setCanvasSnapshot(wb.getCurrentPage().getCanvasSnapshot());
        }
        return dto;
    }

    //Save whiteboard as a teacher
    public void saveWhiteboardState(Long sessionId, String token, String canvasSnapshot) {
        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        TeacherWhiteboard wb = session.getTeacherWhiteboard();
        if (wb == null || wb.getCurrentPage() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Whiteboard page not found");
        }

        WhiteboardPage page = wb.getCurrentPage();
        page.setCanvasSnapshot(canvasSnapshot);
        whiteboardPageRepository.save(page);
    }

    //Save whiteboard as a student
    public void savePersonalWhiteboardState(Long courseId, Long sessionId, String token, String canvasSnapshot) {
        User user = getUserByToken(token);

        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can save their own whiteboard");
        }

        getSessionById(sessionId);

        PersonalWhiteboard whiteboard = personalWhiteboardRepository
                .findByOwnerIdAndSessionSessionId(user.getId(), sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal whiteboard not found"));

        if (whiteboard.getCurrentPage() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Whiteboard page not found");
        }

        WhiteboardPage page = whiteboard.getCurrentPage();
        page.setCanvasSnapshot(canvasSnapshot);
        whiteboardPageRepository.save(page);

        log.debug("Student {} saved personal whiteboard in session {}", user.getId(), sessionId);
    }

    //Teacher loads whiteboard of a student
    public WhiteboardStateDTO getStudentWhiteboardState(Long sessionId, Long studentId) {
        getSessionById(sessionId);

        PersonalWhiteboard whiteboard = personalWhiteboardRepository
                .findByOwnerIdAndSessionSessionId(studentId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student whiteboard not found"));

        WhiteboardStateDTO dto = new WhiteboardStateDTO();
        if (whiteboard.getCurrentPage() != null) {
            dto.setCanvasSnapshot(whiteboard.getCurrentPage().getCanvasSnapshot());
        }
        return dto;
    }

    //End session
    public void endSession(Long sessionId, String token) {
        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        TeacherWhiteboard teacherWhiteboard = session.getTeacherWhiteboard();
        if (teacherWhiteboard != null && teacherWhiteboard.getCurrentPage() != null) {
            WhiteboardPage currentPage = teacherWhiteboard.getCurrentPage();
            String canvasSnapshot = currentPage.getCanvasSnapshot();

            if (canvasSnapshot != null && !canvasSnapshot.isBlank()) {
                try {
                    byte[] whiteboardPdf = createWhiteboardPdfFromCanvasSnapshot(canvasSnapshot);
                    String encodedPdf = Base64.getEncoder().encodeToString(whiteboardPdf);
                    currentPage.setBackgroundFile("data:application/pdf;base64," + encodedPdf);
                    whiteboardPageRepository.save(currentPage);
                } catch (ResponseStatusException e) {
                    log.warn("Skipping whiteboard PDF export for session {}: {}", sessionId, e.getReason());
                }
            }
        }

        session.setActive(false);
        chatMessageService.deleteSessionMessages(sessionId);

        sessionRepository.save(session);
        sessionRepository.flush();
        log.debug("Ended session {}", sessionId);
    }

    private byte[] createWhiteboardPdfFromCanvasSnapshot(String canvasSnapshot) {
        BufferedImage image = decodeCanvasSnapshotImage(canvasSnapshot);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            PDImageXObject imageObject = LosslessFactory.createFromImage(document, image);

            float margin = 36f;
            float maxWidth = pageSize.getWidth() - (2 * margin);
            float maxHeight = pageSize.getHeight() - (2 * margin);

            float imageWidth = image.getWidth();
            float imageHeight = image.getHeight();
            float scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);

            float drawWidth = imageWidth * scale;
            float drawHeight = imageHeight * scale;
            float x = (pageSize.getWidth() - drawWidth) / 2f;
            float y = (pageSize.getHeight() - drawHeight) / 2f;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(imageObject, x, y, drawWidth, drawHeight);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate whiteboard PDF");
        }
    }

    private BufferedImage decodeCanvasSnapshotImage(String canvasSnapshot) {
        String data = canvasSnapshot.trim();

        if (data.startsWith("data:")) {
            int commaIndex = data.indexOf(',');
            if (commaIndex < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canvas snapshot is malformed");
            }

            String metadata = data.substring(0, commaIndex).toLowerCase();
            if (!metadata.startsWith("data:image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canvas snapshot is not an image");
            }

            data = data.substring(commaIndex + 1);
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canvas snapshot cannot be decoded");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canvas snapshot image format is unsupported");
            }
            return image;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canvas snapshot cannot be parsed as an image");
        }
    }

    //Display sessions in a course dashboard
    public List<Session> getSessionsByCourse(Long courseId, String token) {
        User user = getUserByToken(token);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean isTeacher = course.getTeacher().getId().equals(user.getId());
        boolean isStudent = courseEnrollmentRepository.findByStudentIdAndCourseId(user.getId(), course.getId()).isPresent();

        if (!isTeacher && !isStudent) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this course");
        }

        return sessionRepository.findByCourseId(courseId);
    }

    //Select student whiteboard and broadcast it
    public void selectStudentBoard(Long courseId, Long sessionId, String token, Long studentId){

        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a student");
        }

        //Fetch whiteboard of the student from the DB
        PersonalWhiteboard whiteboard = personalWhiteboardRepository
                .findByOwnerIdAndSessionSessionId(studentId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student whiteboard not found"));

        session.setMode(SessionMode.STUDENT);
        session.setSelectedWhiteboard(whiteboard);
        sessionRepository.save(session);
        sessionRepository.flush();

        sessionWebSocketHandler.broadcastCollaborationStart(sessionId.toString(), studentId, whiteboard.getWhiteboardId());
        log.debug("Teacher selected student {} board in session {}", studentId, sessionId);
    }

    //Deselect student whiteboard
    public void deselectStudentBoard(Long courseId, Long sessionId, String token, Long studentId, String canvasSnapshot) {
        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        PersonalWhiteboard whiteboard = personalWhiteboardRepository
                .findByOwnerIdAndSessionSessionId(studentId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student whiteboard not found"));

        if (whiteboard.getCurrentPage() != null) {
            WhiteboardPage page = whiteboard.getCurrentPage();
            page.setCanvasSnapshot(canvasSnapshot);
            whiteboardPageRepository.save(page);
        }

        session.setMode(SessionMode.NORMAL);
        session.setSelectedWhiteboard(null);
        sessionRepository.save(session);
        sessionRepository.flush();

        sessionWebSocketHandler.broadcastCollaborationEnd(sessionId.toString());
        log.debug("Teacher deselected student board in session {}", sessionId);
    }

    public void toggleCollaboration(Long sessionId, String token, boolean collaborationActive) {
        User user = getUserByToken(token);
        validateTeacher(user);

        Session session = getSessionById(sessionId);
        validateSessionOwner(session, user);

        if (collaborationActive) {
            session.setMode(SessionMode.MULTI_MODE);
            session.setSelectedWhiteboard(null);
            sessionRepository.save(session);
            sessionRepository.flush();
            sessionWebSocketHandler.broadcastCollaborationStart(sessionId.toString(), null, null);
            log.debug("Enabled collaboration mode in session {}", sessionId);
            return;
        }

        session.setMode(SessionMode.NORMAL);
        session.setSelectedWhiteboard(null);
        sessionRepository.save(session);
        sessionRepository.flush();
        sessionWebSocketHandler.broadcastCollaborationEnd(sessionId.toString());
        log.debug("Disabled collaboration mode in session {}", sessionId);
    }


    public List<SessionFile> getSessionFiles(Long sessionId, String token) {
        getUserByToken(token);
        getSessionById(sessionId);
        return sessionFileRepository.findBySessionSessionId(sessionId);
    }

    public byte[] getCourseWhiteboardPdf(Long courseId, String token) {
        User user = getUserByToken(token);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean isTeacher = course.getTeacher().getId().equals(user.getId());
        boolean isStudent = courseEnrollmentRepository.findByStudentIdAndCourseId(user.getId(), courseId).isPresent();
        if (!isTeacher && !isStudent) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this course");
        }

        List<Session> sessions = sessionRepository.findByCourseId(courseId);
        sessions.sort(Comparator.comparing(Session::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        for (Session session : sessions) {
            if (session.getTeacherWhiteboard() == null || session.getTeacherWhiteboard().getCurrentPage() == null) {
                continue;
            }

            String backgroundFile = session.getTeacherWhiteboard().getCurrentPage().getBackgroundFile();
            if (backgroundFile == null || backgroundFile.isBlank()) {
                continue;
            }

            return decodeStoredPdf(backgroundFile);
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No teacher whiteboard PDF found for this course");
    }

    public SessionFile uploadSessionFile(Long sessionId, String token, MultipartFile file) {
        getUserByToken(token);
        Session session = getSessionById(sessionId);

        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are allowed");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file");
        }

        SessionFile sessionFile = new SessionFile();
        sessionFile.setFileName(file.getOriginalFilename());
        sessionFile.setFileType(contentType);
        sessionFile.setData(data);
        sessionFile.setUploadedAt(LocalDateTime.now());
        sessionFile.setSession(session);

        sessionFile = sessionFileRepository.save(sessionFile);
        sessionFileRepository.flush();

        log.debug("Uploaded file {} to session {}", file.getOriginalFilename(), sessionId);
        return sessionFile;
    }

    public byte[] summarizeSessionFileToPdf(Long sessionId, Long fileId, String token) {
        getUserByToken(token);
        getSessionById(sessionId);

        SessionFile sessionFile = sessionFileRepository.findByIdAndSessionSessionId(fileId, sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found in session"));

        if (!"application/pdf".equals(sessionFile.getFileType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files can be summarized");
        }

        String extractedText = extractTextFromPdf(sessionFile.getData());
        if (extractedText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No extractable text found in PDF");
        }

        String summaryText = geminiSummaryService.summarizeText(extractedText);
        return createSummaryPdf(summaryText);
    }

    private String extractTextFromPdf(byte[] pdfData) {
        try (PDDocument document = PDDocument.load(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse PDF content");
        }
    }

    private byte[] createSummaryPdf(String summaryText) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50f;
            float yPosition = page.getMediaBox().getHeight() - margin;
            float leading = 16f;
            float maxWidth = page.getMediaBox().getWidth() - (2 * margin);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("PDF Summary");
            contentStream.endText();

            yPosition -= 28f;
            contentStream.setFont(PDType1Font.HELVETICA, 11);

            for (String line : wrapText(summaryText, PDType1Font.HELVETICA, 11, maxWidth)) {
                if (yPosition <= margin) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(PDType1Font.HELVETICA, 11);
                    yPosition = page.getMediaBox().getHeight() - margin;
                }

                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(line);
                contentStream.endText();
                yPosition -= leading;
            }

            contentStream.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate summary PDF");
        }
    }

    private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> wrappedLines = new ArrayList<>();
        for (String paragraph : text.split("\\r?\\n")) {
            if (paragraph.isBlank()) {
                wrappedLines.add("");
                continue;
            }

            String[] words = paragraph.split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
                float width = font.getStringWidth(candidate) / 1000 * fontSize;
                if (width <= maxWidth) {
                    currentLine = new StringBuilder(candidate);
                } else {
                    if (!currentLine.isEmpty()) {
                        wrappedLines.add(currentLine.toString());
                    }
                    currentLine = new StringBuilder(word);
                }
            }

            if (!currentLine.isEmpty()) {
                wrappedLines.add(currentLine.toString());
            }
        }
        return wrappedLines;
    }

    private byte[] decodeStoredPdf(String storedValue) {
        String data = storedValue.trim();

        if (data.startsWith("data:")) {
            int commaIndex = data.indexOf(',');
            if (commaIndex < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stored teacher PDF is malformed");
            }

            String metadata = data.substring(0, commaIndex).toLowerCase();
            if (!metadata.contains("application/pdf")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stored teacher file is not a PDF");
            }

            data = data.substring(commaIndex + 1);
        }

        try {
            return Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stored teacher PDF cannot be decoded");
        }
    }

    //Helper functions
    // Fetch user via token including validation
    private User getUserByToken(String token) {
        return userRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
    }

    // Check if user is a teacher
    private void validateTeacher(User user) {
        if (user.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can do this");
        }
    }

    // Fetch session by ID
    private Session getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    // Check if teacher owns the session
    private void validateSessionOwner(Session session, User user) {
        if (!session.getCourse().getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the teacher of this session");
        }
    }
}