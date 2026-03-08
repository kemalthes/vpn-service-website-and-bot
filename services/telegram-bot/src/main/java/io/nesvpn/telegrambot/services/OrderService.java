package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.enums.TransactionType;
import io.nesvpn.telegrambot.handler.VpnBot;
import io.nesvpn.telegrambot.model.Order;
import io.nesvpn.telegrambot.enums.OrderStatus;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.rabbit.OrderPaidEvent;
import io.nesvpn.telegrambot.repository.OrderRepository;
import io.nesvpn.telegrambot.util.DisplayTelegramUsername;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final VpnBot vpnBot;
    private final OrderRepository orderRepository;
    private final BalanceService balanceService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(User user, VpnPlan plan) {
        Order newOrder = new Order();
        newOrder.setUserId(user.getId());
        newOrder.setVpnPlanId(plan.getId());
        newOrder.setStatus(OrderStatus.PAID.getValue());
        Order savedOrder = orderRepository.save(newOrder);
        balanceService.subtractBalance(
                user.getId(),
                BigDecimal.valueOf(plan.getPrice()),
                TransactionType.SUBSCRIPTION_PURCHASE,
                "Продление VPN-подписки на " + plan.getDuration() + " дней");
        eventPublisher.publishEvent(new OrderPaidEvent(user.getId(),
                savedOrder.getId(),
                plan.getId(),
                DisplayTelegramUsername.getDisplayName(vpnBot, user.getTgId())));
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