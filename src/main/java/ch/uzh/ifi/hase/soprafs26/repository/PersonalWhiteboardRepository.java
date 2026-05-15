package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.PersonalWhiteboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalWhiteboardRepository extends JpaRepository<PersonalWhiteboard, Long> {

    Optional<PersonalWhiteboard> findByOwnerIdAndSessionSessionId(Long ownerId, Long sessionId);

    List<PersonalWhiteboard> findBySessionSessionId(Long sessionId);
}
