package com.tradeintel.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts access exclusively to users with the {@code uber_admin} role.
 *
 * <p>Apply this meta-annotation to controller methods or classes that expose
 * sensitive platform operations such as user management, cost reports, audit logs,
 * and WhatsApp group configuration.
 *
 * <p>Usage:
 * <pre>{@code
 *     @UberAdminOnly
 *     @GetMapping("/api/admin/users")
 *     public ResponseEntity<Page<UserDTO>> listAllUsers(Pageable pageable) { ... }
 * }</pre>
 *
 * <p>Requires {@code @EnableMethodSecurity} on the security configuration class.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@PreAuthorize("hasRole('UBER_ADMIN')")
public @interface UberAdminOnly {
}
