package com.budget.controller;

import com.budget.dto.CreateSubscriptionRequest;
import com.budget.dto.SubscriptionDTO;
import com.budget.dto.UpdateSubscriptionRequest;
import com.budget.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public List<SubscriptionDTO> getAllSubscriptions() {
        return subscriptionService.getAllSubscriptions();
    }

    @GetMapping("/{id}")
    public SubscriptionDTO getSubscription(@PathVariable Long id) {
        return subscriptionService.getSubscription(id);
    }

    @PostMapping
    public SubscriptionDTO createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
        return subscriptionService.createSubscription(request);
    }

    @PutMapping("/{id}")
    public SubscriptionDTO updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        return subscriptionService.updateSubscription(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}
