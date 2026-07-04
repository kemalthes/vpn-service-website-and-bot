package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.enums.OrderOperationType;
import io.nesvpn.telegrambot.enums.TransactionType;
import io.nesvpn.telegrambot.model.Order;
import io.nesvpn.telegrambot.enums.OrderStatus;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.rabbit.DeviceLimitChangePaidEvent;
import io.nesvpn.telegrambot.rabbit.OrderPaidEvent;
import io.nesvpn.telegrambot.repository.OrderRepository;
import io.nesvpn.telegrambot.repository.TokenRepository;
import io.nesvpn.telegrambot.repository.UserRepository;
import io.nesvpn.telegrambot.repository.VpnPlanRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BalanceService balanceService;
    private final SubscriptionDevicePricingService pricingService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final VpnPlanRepository vpnPlanRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(User user, VpnPlan plan) {
        return createOrder(
                user,
                plan,
                "Продление VPN-подписки на " + plan.getDuration() + " дней"
        );
    }

    @Transactional
    public Order createOrder(User user, VpnPlan plan, String balanceDescription) {
        OrderOperationType operationType = plan.getPrice() == 0
                ? OrderOperationType.NEW_SUBSCRIPTION
                : OrderOperationType.SUBSCRIPTION_RENEWAL;
        Integer targetMaxDevices = plan.getDefaultDevices() != null ? plan.getDefaultDevices() : 3;
        BigDecimal totalPrice = BigDecimal.valueOf(plan.getPrice());
        return createImmediatePaidOrder(
                user,
                null,
                plan,
                operationType,
                targetMaxDevices,
                totalPrice,
                balanceDescription
        );
    }

    @Transactional
    public Order upsertRenewalDraft(User user, Token token, Integer targetMaxDevices) {
        Order order = orderRepository.findByUserIdAndOperationTypeAndStatus(
                        user.getId(),
                        OrderOperationType.SUBSCRIPTION_RENEWAL.name(),
                        OrderStatus.DRAFT.getValue())
                .orElseGet(Order::new);

        order.setUserId(user.getId());
        order.setTokenId(token.getId());
        order.setVpnPlanId(null);
        order.setStatus(OrderStatus.DRAFT.getValue());
        order.setOperationType(OrderOperationType.SUBSCRIPTION_RENEWAL.name());
        order.setTargetMaxDevices(targetMaxDevices);
        order.setTotalPrice(null);
        return orderRepository.save(order);
    }

    @Transactional
    public Order attachPlanToRenewalDraft(User user, Long draftOrderId, VpnPlan plan) {
        Order order = orderRepository.findByIdForUpdate(draftOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + draftOrderId));
        validateDraftOwner(order, user, OrderOperationType.SUBSCRIPTION_RENEWAL);

        order.setVpnPlanId(plan.getId());
        order.setTotalPrice(pricingService.calculateRenewalPrice(plan, order.getTargetMaxDevices()));
        return orderRepository.save(order);
    }

    @Transactional
    public Order upsertDeviceLimitChangeDraft(User user, Token token, Integer targetMaxDevices) {
        Order order = orderRepository.findByUserIdAndOperationTypeAndStatus(
                        user.getId(),
                        OrderOperationType.DEVICE_LIMIT_CHANGE.name(),
                        OrderStatus.DRAFT.getValue())
                .orElseGet(Order::new);

        order.setUserId(user.getId());
        order.setTokenId(token.getId());
        order.setVpnPlanId(null);
        order.setStatus(OrderStatus.DRAFT.getValue());
        order.setOperationType(OrderOperationType.DEVICE_LIMIT_CHANGE.name());
        order.setTargetMaxDevices(targetMaxDevices);
        order.setTotalPrice(pricingService.calculateDeviceLimitChangePrice(token, targetMaxDevices));
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmDraft(User user, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
        if (!order.getUserId().equals(user.getId())) {
            throw new IllegalStateException("Order user mismatch");
        }
        if (!OrderStatus.DRAFT.getValue().equals(order.getStatus())) {
            return order;
        }

        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + user.getId()));
        BigDecimal totalPrice = recalculateTotalPrice(order);
        BigDecimal balance = lockedUser.getBalance() != null ? lockedUser.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(totalPrice) < 0) {
            throw new NotEnoughBalanceException(totalPrice, balance);
        }

        if (totalPrice.compareTo(BigDecimal.ZERO) > 0) {
            balanceService.subtractBalance(
                    lockedUser.getId(),
                    totalPrice,
                    TransactionType.SUBSCRIPTION_PURCHASE,
                    getBalanceDescription(order)
            );
        }

        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.PAID.getValue());
        Order savedOrder = orderRepository.save(order);
        publishPaidEvent(lockedUser, savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order createImmediatePaidOrder(
            User user,
            Token token,
            VpnPlan plan,
            OrderOperationType operationType,
            Integer targetMaxDevices,
            BigDecimal totalPrice,
            String balanceDescription
    ) {
        Order newOrder = new Order();
        newOrder.setUserId(user.getId());
        newOrder.setTokenId(token != null ? token.getId() : null);
        newOrder.setVpnPlanId(plan != null ? plan.getId() : null);
        newOrder.setOperationType(operationType.name());
        newOrder.setTargetMaxDevices(targetMaxDevices);
        newOrder.setTotalPrice(totalPrice);
        newOrder.setStatus(OrderStatus.PAID.getValue());
        Order savedOrder = orderRepository.save(newOrder);

        if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) > 0) {
            balanceService.subtractBalance(
                    user.getId(),
                    totalPrice,
                    TransactionType.SUBSCRIPTION_PURCHASE,
                    balanceDescription);
        }

        publishPaidEvent(user, savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order createLucky777BonusOrder(User user, VpnPlan plan, Integer diceValue) {
        Token token = tokenRepository.findByUserId(user.getId()).orElse(null);
        Order newOrder = new Order();
        newOrder.setUserId(user.getId());
        newOrder.setTokenId(token != null ? token.getId() : null);
        newOrder.setVpnPlanId(plan.getId());
        newOrder.setOperationType(OrderOperationType.LUCKY_777_BONUS.name());
        newOrder.setTargetMaxDevices(token != null ? pricingService.maxDevices(token) : null);
        newOrder.setTotalPrice(BigDecimal.ZERO);
        newOrder.setStatus(OrderStatus.PAID.getValue());
        Order savedOrder = orderRepository.save(newOrder);

        String description = plan.getDuration() == 3
                ? "Вы выиграли в 777 дополнительные 3 дня"
                : "Вы выиграли в 777 дополнительный 1 день";

        balanceService.addBalance(
                user.getId(),
                BigDecimal.ZERO,
                TransactionType.LUCKY_777_WIN,
                description + " (dice: " + diceValue + ")");

        eventPublisher.publishEvent(new OrderPaidEvent(user.getId(),
                savedOrder.getId(),
                plan.getId(),
                user.getTgId()));
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

    private void validateDraftOwner(Order order, User user, OrderOperationType operationType) {
        if (!order.getUserId().equals(user.getId())) {
            throw new IllegalStateException("Order user mismatch");
        }
        if (!OrderStatus.DRAFT.getValue().equals(order.getStatus())) {
            throw new IllegalStateException("Order is not a draft");
        }
        if (!operationType.name().equals(order.getOperationType())) {
            throw new IllegalStateException("Order operation mismatch");
        }
    }

    private BigDecimal recalculateTotalPrice(Order order) {
        OrderOperationType operationType = OrderOperationType.valueOf(order.getOperationType());
        return switch (operationType) {
            case SUBSCRIPTION_RENEWAL -> {
                if (order.getVpnPlanId() == null) {
                    throw new IllegalStateException("Draft renewal order has no plan");
                }
                VpnPlan plan = vpnPlanRepository.findById(order.getVpnPlanId())
                        .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + order.getVpnPlanId()));
                yield pricingService.calculateRenewalPrice(plan, order.getTargetMaxDevices());
            }
            case DEVICE_LIMIT_CHANGE -> {
                if (order.getTokenId() == null) {
                    throw new IllegalStateException("Device limit change order has no token");
                }
                Token token = tokenRepository.findByIdForUpdate(order.getTokenId())
                        .orElseThrow(() -> new EntityNotFoundException("Token not found with id: " + order.getTokenId()));
                yield pricingService.calculateDeviceLimitChangePrice(token, order.getTargetMaxDevices());
            }
            default -> throw new IllegalStateException("Unsupported draft operation: " + operationType);
        };
    }

    private String getBalanceDescription(Order order) {
        OrderOperationType operationType = OrderOperationType.valueOf(order.getOperationType());
        if (operationType == OrderOperationType.DEVICE_LIMIT_CHANGE) {
            Token token = order.getTokenId() != null
                    ? tokenRepository.findById(order.getTokenId()).orElse(null)
                    : null;
            if (token != null) {
                String validTo = token.getValidTo() != null
                        ? token.getValidTo().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        : "конца срока";
                return String.format(
                        "Изменение лимита устройств: %d -> %d до %s",
                        pricingService.maxDevices(token),
                        order.getTargetMaxDevices(),
                        validTo);
            }
            return "Изменение лимита устройств до " + order.getTargetMaxDevices();
        }
        return "Продление VPN-подписки";
    }

    private void publishPaidEvent(User user, Order order) {
        OrderOperationType operationType = OrderOperationType.valueOf(order.getOperationType());
        if (operationType == OrderOperationType.DEVICE_LIMIT_CHANGE) {
            eventPublisher.publishEvent(new DeviceLimitChangePaidEvent(
                    user.getId(),
                    order.getId(),
                    order.getTokenId(),
                    order.getTargetMaxDevices(),
                    user.getTgId()));
            return;
        }

        eventPublisher.publishEvent(new OrderPaidEvent(user.getId(),
                order.getId(),
                order.getVpnPlanId(),
                user.getTgId()));
    }
}
