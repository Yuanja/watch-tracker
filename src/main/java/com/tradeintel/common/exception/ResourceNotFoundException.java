package com.tradeintel.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource cannot be found in the data store.
 * Maps to an HTTP 404 response via {@link ResponseStatus} and is also
 * handled explicitly in {@link GlobalExceptionHandler} to produce a
 * consistent JSON error body.
 *
 * <p>Usage example:
 * <pre>{@code
 *     User user = userRepository.findById(id)
 *         .orElseThrow(() -> new ResourceNotFoundException("User", id));
 * }</pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /** Identifies the resource type for structured logging and error messages. */
    private final String resourceType;

    /**
     * Creates a {@code ResourceNotFoundException} with a formatted message.
     *
     * @param resourceType the type of the missing resource (e.g. "User", "Listing")
     * @param identifier   the identifier that was not found (typically a UUID or string)
     */
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(resourceType + " not found: " + identifier);
        this.resourceType = resourceType;
    }

    /**
     * Creates a {@code ResourceNotFoundException} with a custom message.
     *
     * @param message human-readable description of what was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = "Resource";
    }

    /**
     * Returns the type of resource that was not found.
     *
     * @return resource type string, e.g. "User" or "Listing"
     */
    public String getResourceType() {
        return resourceType;
    }
}
