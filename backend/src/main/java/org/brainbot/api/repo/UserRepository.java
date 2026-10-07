package org.brainbot.api.repo;
import java.util.Optional;
import org.brainbot.api.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface UserRepository extends JpaRepository<AppUser,Long> {
    Optional<AppUser> findByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from AppUser u where u.email=:email") Optional<AppUser> findByEmailForUpdate(@Param("email") String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from AppUser u where u.id=:id") Optional<AppUser> findByIdForUpdate(@Param("id") Long id);
}
