package com.bt.fretchain.primary.controller.partner;

import com.bt.fretchain.exception.BadRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;

/**
 * Partner endpoints fill the shop / restaurant from the login before validating,
 * so the shared request DTOs (which require shopUuid / restaurantUuid) can be reused.
 */
@Component
@RequiredArgsConstructor
public class PartnerRequestValidator {

    private final Validator validator;

    public <T> T validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            ConstraintViolation<T> first = violations.stream()
                    .min(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .orElseThrow();
            throw new BadRequestException(first.getPropertyPath() + " " + first.getMessage());
        }
        return request;
    }
}
