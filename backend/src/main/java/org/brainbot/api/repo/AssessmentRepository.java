package org.brainbot.api.repo;
import java.util.List;
import org.brainbot.api.domain.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AssessmentRepository extends JpaRepository<Assessment,Long> { List<Assessment> findTop50ByUserIdOrderByCreatedAtDesc(Long userId); void deleteByUserId(Long userId); }
