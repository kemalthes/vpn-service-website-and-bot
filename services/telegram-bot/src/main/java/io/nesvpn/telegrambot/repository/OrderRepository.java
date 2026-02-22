package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUserIdAndVpnPlanId(UUID userId, Long vpnPlanId);

    boolean existsByUserIdAndVpnPlanId(UUID userId, Long vpnPlanId);

    List<Order> findAllByStatus(String status);
}