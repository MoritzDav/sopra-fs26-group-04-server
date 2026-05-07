package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.SessionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("sessionFileRepository")
public interface SessionFileRepository extends JpaRepository<SessionFile, Long> {
    List<SessionFile> findBySessionSessionId(Long sessionId);
}