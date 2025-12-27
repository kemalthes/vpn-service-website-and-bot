package io.kemalthes.vpnservice.service;

import io.kemalthes.vpnservice.dto.DashboardResponse;
import io.kemalthes.vpnservice.dto.VpnPlanResponse;
import io.kemalthes.vpnservice.mapper.VpnPlanMapper;
import io.kemalthes.vpnservice.repository.CommentRepository;
import io.kemalthes.vpnservice.repository.OrderRepository;
import io.kemalthes.vpnservice.repository.TokenRepository;
import io.kemalthes.vpnservice.repository.UserRepository;
import io.kemalthes.vpnservice.repository.VpnPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Cacheable(value = "dashboard")
    public DashboardResponse getDashboard() {
        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .averageScore(Optional.ofNullable(commentRepository.getAverageScore()).orElse(0.))
                .activeSubscriptions(tokenRepository.countAllByStatus("active"))
                .totalOrders(orderRepository.count())
                .featuredComments(
                        commentRepository.findRandomComments(4.5, 10, 5))
                .build();
    }

    @Cacheable(value = "vpnPlans")
    public List<VpnPlanResponse> getPlans() {
        return vpnPlanMapper.toDtoList(vpnPlanRepository.findAll());
    }
}
