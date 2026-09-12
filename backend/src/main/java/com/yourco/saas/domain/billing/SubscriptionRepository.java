package com.yourco.saas.domain.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<Subscription> findByCustomerId(Long customerId);
}