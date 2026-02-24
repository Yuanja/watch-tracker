package com.tradeintel.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts access to users with the {@code admin} or {@code uber_admin} role.
 *
 * <p>Apply this meta-annotation to controller methods or classes that should only
 * be accessible by platform administrators. Access is cumulative: uber_admin users
 * also pass this check because they carry both {@code ROLE_ADMIN} and
 * {@code ROLE_UBER_ADMIN} authorities.
 *
 * <p>Usage:
 * <pre>{@code
 *     @AdminOnly
 *     @GetMapping("/api/review")
 *     public ResponseEntity<List<ReviewItem>> listPendingReviews() { ... }
 * }</pre>
 *
 * <p>Requires {@code @EnableMethodSecurity} on the security configuration class.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@PreAuthorize("hasAnyRole('ADMIN', 'UBER_ADMIN')")
public @interface AdminOnly {
}
