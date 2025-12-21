package io.kemalthes.vpnservice.repository;

import io.kemalthes.vpnservice.entity.Token;
import org.springframework.data.repository.CrudRepository;

public interface TokenRepository extends CrudRepository<Token, Integer> {

    long countAllByStatus(String status);

}
