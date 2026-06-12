package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByTgId(Long tgId);

    Optional<User> findById(UUID id);

    List<User> findAllByIdIn(Collection<UUID> id);

    Optional<User> findByReferralCode(String referralCode);

    List<User> findAllByReferredBy(Long referredBy);

    boolean existsByTgId(Long tgId);

    boolean existsByTgIdAndRoleIgnoreCase(Long tgId, String role);

    @Query("SELECT u.tgId FROM User u WHERE LOWER(u.role) = LOWER(:role) AND u.tgId IS NOT NULL")
    List<Long> findTgIdsByRole(@Param("role") String role);

    int countByReferredBy(Long referredBy);
}
