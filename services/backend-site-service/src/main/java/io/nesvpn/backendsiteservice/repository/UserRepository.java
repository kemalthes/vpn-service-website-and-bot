package io.nesvpn.backendsiteservice.repository;

import io.nesvpn.backendsiteservice.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface UserRepository extends CrudRepository<User, UUID> {
}
