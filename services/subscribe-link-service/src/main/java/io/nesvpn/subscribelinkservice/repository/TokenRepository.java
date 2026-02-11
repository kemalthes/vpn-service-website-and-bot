package io.nesvpn.subscribelinkservice.repository;

import io.nesvpn.subscribelinkservice.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TokenRepository extends JpaRepository<Token, Long> {
}
