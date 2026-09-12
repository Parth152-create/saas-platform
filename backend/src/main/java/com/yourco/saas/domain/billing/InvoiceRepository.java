package com.yourco.saas.domain.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId);
    List<Invoice> findByCustomerId(Long customerId);
}