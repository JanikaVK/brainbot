package org.brainbot.api.repo;
import java.util.Optional;
import org.brainbot.api.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<AppUser,Long> { Optional<AppUser> findByEmail(String email); }
