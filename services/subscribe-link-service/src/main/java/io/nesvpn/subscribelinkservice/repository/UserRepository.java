package io.nesvpn.subscribelinkservice.repository;

import io.nesvpn.subscribelinkservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
