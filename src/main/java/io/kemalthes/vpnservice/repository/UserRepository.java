package io.kemalthes.vpnservice.repository;

import io.kemalthes.vpnservice.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface UserRepository extends CrudRepository<User, UUID> {

    long count();

}
