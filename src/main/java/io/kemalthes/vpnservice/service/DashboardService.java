package io.kemalthes.vpnservice.service;

import io.kemalthes.vpnservice.dto.DashboardResponse;
import io.kemalthes.vpnservice.dto.VpnPlanResponse;
import io.kemalthes.vpnservice.enums.TokenStatus;
import io.kemalthes.vpnservice.mapper.VpnPlanMapper;
import io.kemalthes.vpnservice.repository.CommentRepository;
import io.kemalthes.vpnservice.repository.OrderRepository;
import io.kemalthes.vpnservice.repository.TokenRepository;
import io.kemalthes.vpnservice.repository.UserRepository;
import io.kemalthes.vpnservice.repository.VpnPlanRepository;
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
