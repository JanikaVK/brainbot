package org.brainbot.api.repo;
import java.util.List;
import org.brainbot.api.domain.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CheckInRepository extends JpaRepository<CheckIn,Long> { List<CheckIn> findTop90ByUserIdOrderByCreatedAtDesc(Long userId); void deleteByUserId(Long userId); }
