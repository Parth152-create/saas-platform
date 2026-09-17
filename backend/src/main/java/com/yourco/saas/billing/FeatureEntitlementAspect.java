package com.yourco.saas.billing;

import com.yourco.saas.domain.billing.Feature;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(10)
public class FeatureEntitlementAspect {

    private static final Logger log = LoggerFactory.getLogger(FeatureEntitlementAspect.class);

    private final FeatureEntitlementService featureEntitlementService;

    public FeatureEntitlementAspect(FeatureEntitlementService featureEntitlementService) {
        this.featureEntitlementService = featureEntitlementService;
    }

    @Before("@annotation(com.yourco.saas.billing.RequiresFeature) || @within(com.yourco.saas.billing.RequiresFeature)")
    public void checkFeatureAccess(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresFeature annotation = method.getAnnotation(RequiresFeature.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequiresFeature.class);
        }

        if (annotation != null) {
            Feature feature = annotation.value();
            log.debug("Checking entitlement for feature {} on method {}", feature, method.getName());

            if (!featureEntitlementService.hasFeature(feature)) {
                log.warn("Access denied: tenant not entitled to feature {}", feature);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Feature " + feature.name() + " is not included in the current subscription plan");
            }
        }
    }
}
