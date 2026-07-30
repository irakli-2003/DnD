package com.dnd.data;

/**
 * Unchecked exception representing a failure to read or write persisted
 * campaign data (JSON files, the ID registry, campaign directories, etc.).
 *
 * This is the single exception type used consistently across the data layer
 * ({@link JsonRepository}, {@link IdHandler}, and campaign storage) so callers
 * only need to handle one failure mode for I/O problems. It intentionally
 * does NOT cover validation failures (invalid input, duplicate ids, etc.) -
 * those continue to use {@link IllegalArgumentException}, since they represent
 * a different class of problem (caller error, not an environment/I-O failure).
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}

