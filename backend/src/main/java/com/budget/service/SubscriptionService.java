package com.budget.service;

import com.budget.dto.CreateSubscriptionRequest;
import com.budget.dto.SubscriptionDTO;
import com.budget.dto.UpdateSubscriptionRequest;
import com.budget.model.Subscription;
import com.budget.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public List<SubscriptionDTO> getAllSubscriptions() {
        return subscriptionRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(SubscriptionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public SubscriptionDTO getSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found: " + id));
        return SubscriptionDTO.fromEntity(subscription);
    }

    @Transactional
    public SubscriptionDTO createSubscription(CreateSubscriptionRequest request) {
        Subscription subscription = new Subscription();
        subscription.setName(request.getName());
        subscription.setAmount(request.getAmount());
        subscription.setBillingDay(request.getBillingDay());
        subscription.setCategory(request.getCategory());
        subscription.setRecurrence(request.getRecurrence());
        subscription.setIsActive(true);

        subscription = subscriptionRepository.save(subscription);
        return SubscriptionDTO.fromEntity(subscription);
    }

    @Transactional
    public SubscriptionDTO updateSubscription(Long id, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found: " + id));

        if (request.getName() != null) {
            subscription.setName(request.getName());
        }
        if (request.getAmount() != null) {
            subscription.setAmount(request.getAmount());
        }
        if (request.getBillingDay() != null) {
            subscription.setBillingDay(request.getBillingDay());
        }
        if (request.getCategory() != null) {
            subscription.setCategory(request.getCategory());
        }
        if (request.getRecurrence() != null) {
            subscription.setRecurrence(request.getRecurrence());
        }

        subscription = subscriptionRepository.save(subscription);
        return SubscriptionDTO.fromEntity(subscription);
    }

    @Transactional
    public void deleteSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found: " + id));

        // Soft delete - just mark as inactive
        subscription.setIsActive(false);
        subscriptionRepository.save(subscription);
    }
}
