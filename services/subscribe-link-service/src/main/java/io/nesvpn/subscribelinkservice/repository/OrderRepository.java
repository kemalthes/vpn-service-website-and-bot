package io.nesvpn.subscribelinkservice.repository;

import io.nesvpn.subscribelinkservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
