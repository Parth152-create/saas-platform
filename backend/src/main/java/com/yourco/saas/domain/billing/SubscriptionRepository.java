package com.yourco.saas.domain.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    List<Subscription> findByCustomerIdOrderByUpdatedAtDesc(Long customerId);

    default List<Subscription> findByCustomerId(Long customerId) {
        return findByCustomerIdOrderByUpdatedAtDesc(customerId);
    }
}