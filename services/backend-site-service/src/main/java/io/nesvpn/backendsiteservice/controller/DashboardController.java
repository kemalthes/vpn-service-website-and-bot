package io.nesvpn.backendsiteservice.controller;

import io.nesvpn.backendsiteservice.dto.DashboardResponse;
import io.nesvpn.backendsiteservice.dto.VpnPlanResponse;
import io.nesvpn.backendsiteservice.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dashboard", description = "Эндпоинты dashboard")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping(produces = "application/json")
    public DashboardResponse getDashboard() {
        return service.getDashboard();
    }

    @GetMapping(value = "/plans", produces = "application/json")
    public List<VpnPlanResponse> getPlans() {
        return service.getPlans();
    }
}
