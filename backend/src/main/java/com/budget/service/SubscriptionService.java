package com.budget.service;

import com.budget.dto.CreateSubscriptionRequest;
import com.budget.dto.SubscriptionDTO;
import com.budget.dto.UpdateSubscriptionRequest;
import com.budget.model.BudgetItem;
import com.budget.model.PlanItem;
import com.budget.model.Subscription;
import com.budget.repository.BudgetItemRepository;
import com.budget.repository.PlanItemRepository;
import com.budget.repository.PlanRepository;
import com.budget.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanItemRepository planItemRepository;
    private final PlanRepository planRepository;
    private final BudgetItemRepository budgetItemRepository;

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

        // Capture the old name BEFORE updating — used to find name-matched legacy plan items
        String oldName = subscription.getName();

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
        propagateSubscriptionUpdate(subscription, oldName);
        return SubscriptionDTO.fromEntity(subscription);
    }

    /**
     * When a subscription's name or amount changes, update all linked plan items and
     * recalculate the plan total → budget item planned amount for each affected plan.
     * Handles both FK-linked items (new) and legacy items matched by flag+oldName (existing plans).
     * Backfills the FK on legacy items so future updates work via FK alone.
     */
    private void propagateSubscriptionUpdate(Subscription subscription, String oldName) {
        List<PlanItem> linkedItems = planItemRepository.findLinkedToSubscription(subscription.getId(), oldName);
        if (linkedItems.isEmpty()) return;

        for (PlanItem item : linkedItems) {
            item.setName(subscription.getName());
            item.setAmount(subscription.getAmount());
            // Backfill the FK so future updates flow through the FK path
            if (item.getSubscription() == null) {
                item.setSubscription(subscription);
            }
        }
        planItemRepository.saveAll(linkedItems);

        // Recalculate total for each affected plan and sync to budget item
        Set<Long> planIds = linkedItems.stream()
                .map(pi -> pi.getPlan().getId())
                .collect(Collectors.toSet());
        for (Long planId : planIds) {
            recalculatePlanTotal(planId);
        }
    }

    private void recalculatePlanTotal(Long planId) {
        List<PlanItem> allItems = planItemRepository.findByPlanIdOrderByDisplayOrderAsc(planId);
        BigDecimal total = allItems.stream()
                .map(PlanItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        planRepository.findById(planId).ifPresent(plan -> {
            BudgetItem budgetItem = plan.getBudgetItem();
            budgetItem.setPlannedAmount(total);
            budgetItemRepository.save(budgetItem);
        });
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
