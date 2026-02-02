package io.nesvpn.backendsiteservice.repository;

import io.nesvpn.backendsiteservice.entity.Order;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OrderRepository extends CrudRepository<Order, Integer> {

    boolean existsByUserIdAndStatus(UUID uuid, String status);
}
