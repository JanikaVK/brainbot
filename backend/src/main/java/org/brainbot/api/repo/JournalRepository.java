package org.brainbot.api.repo;
import java.util.List;
import java.util.Optional;
import org.brainbot.api.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
public interface JournalRepository extends JpaRepository<JournalEntry,Long> { List<JournalEntry> findTop50ByUserIdOrderByUpdatedAtDesc(Long userId); Optional<JournalEntry> findByIdAndUserId(Long id,Long userId); void deleteByUserId(Long userId); }
