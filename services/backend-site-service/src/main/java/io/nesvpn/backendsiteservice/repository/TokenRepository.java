package io.nesvpn.backendsiteservice.repository;

import io.nesvpn.backendsiteservice.entity.Token;
import org.springframework.data.repository.CrudRepository;

public interface TokenRepository extends CrudRepository<Token, Integer> {

    long countAllByStatus(String status);

}
