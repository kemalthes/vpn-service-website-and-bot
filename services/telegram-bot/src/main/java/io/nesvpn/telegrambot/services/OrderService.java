package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.enums.TransactionType;
import io.nesvpn.telegrambot.model.Order;
import io.nesvpn.telegrambot.enums.OrderStatus;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BalanceService balanceService;

    @Transactional
    public Order createOrder(UUID userId, VpnPlan plan) {
        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setVpnPlanId(plan.getId());
        newOrder.setStatus(OrderStatus.PAID.getValue());

        Order savedOrder = orderRepository.save(newOrder);
        balanceService.subtractBalance(userId, BigDecimal.valueOf(plan.getPrice()), TransactionType.SUBSCRIPTION_PURCHASE, "Продление VPN-подписки на " + plan.getDuration() + " дней");

        return savedOrder;
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
    }

    public Order getOrderByUserAndPlan(UUID userId, Long vpnPlanId) {
        return orderRepository.findByUserIdAndVpnPlanId(userId, vpnPlanId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Order not found for user %s and plan %d", userId, vpnPlanId)
                ));
    }

    public boolean orderExists(UUID userId, Long vpnPlanId) {
        return orderRepository.existsByUserIdAndVpnPlanId(userId, vpnPlanId);
    }


    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findAllByStatus(status.getValue());
    }
}