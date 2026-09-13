package com.yourco.saas.billing.dto;

import com.yourco.saas.domain.billing.PlanTier;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutSessionRequest(@NotNull PlanTier planTier) {}