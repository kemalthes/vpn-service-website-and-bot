package io.nesvpn.backendsiteservice.service;

import io.nesvpn.backendsiteservice.dto.DashboardResponse;
import io.nesvpn.backendsiteservice.dto.VpnPlanResponse;
import io.nesvpn.backendsiteservice.enums.TokenStatus;
import io.nesvpn.backendsiteservice.mapper.VpnPlanMapper;
import io.nesvpn.backendsiteservice.repository.CommentRepository;
import io.nesvpn.backendsiteservice.repository.OrderRepository;
import io.nesvpn.backendsiteservice.repository.TokenRepository;
import io.nesvpn.backendsiteservice.repository.UserRepository;
import io.nesvpn.backendsiteservice.repository.VpnPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TokenRepository tokenRepository;
    private final OrderRepository orderRepository;
    private final VpnPlanRepository vpnPlanRepository;
    private final VpnPlanMapper vpnPlanMapper;

    @Value("${project.dashboard-comment.min-score}")
    private double minScore;
    @Value("${project.dashboard-comment.min-length}")
    private int minLength;
    @Value("${project.dashboard-comment.limit}")
    private int limit;

    @Cacheable(value = "dashboard")
    public DashboardResponse getDashboard() {
        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .averageScore(Optional.ofNullable(commentRepository.getAverageScore()).orElse(0.))
                .activeSubscriptions(tokenRepository.countAllByStatus(
                        TokenStatus.ACTIVE.name().toLowerCase()))
                .totalOrders(orderRepository.count())
                .featuredComments(
                        commentRepository.findRandomComments(minScore, minLength, limit))
                .build();
    }

    @Cacheable(value = "vpnPlans")
    public List<VpnPlanResponse> getPlans() {
        return vpnPlanMapper.toDtoList(vpnPlanRepository.findAll());
    }
}
