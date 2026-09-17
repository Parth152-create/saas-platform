package com.yourco.saas.billing.dto;

import com.yourco.saas.domain.billing.Feature;

import java.util.Set;

public record FeatureEntitlementsResponse(
        String plan,
        String status,
        Set<Feature> features
) {}
