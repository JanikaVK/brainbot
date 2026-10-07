package org.brainbot.api.repo;
import java.util.List;
import org.brainbot.api.domain.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SupportRequestRepository extends JpaRepository<SupportRequest,Long> { List<SupportRequest> findTop50ByUserIdOrderByCreatedAtDesc(Long userId); void deleteByUserId(Long userId); }
