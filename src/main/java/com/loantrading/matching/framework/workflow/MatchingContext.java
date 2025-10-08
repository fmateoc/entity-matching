package com.loantrading.matching.framework.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A stateful context object that is passed through each step of a workflow.
 *
 * It acts as a container for data, holding the initial input, intermediate
 * results, and the final output of the workflow. Its map-based design
 * allows it to be flexible and carry any type of data needed by the
 * workflow steps.
 */
public class MatchingContext {

    private final Map<String, Object> data = new HashMap<>();

    /**
     * Puts a value into the context.
     *
     * @param key   The key to store the data under.
     * @param value The value to be stored.
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Gets a value from the context.
     *
     * @param key The key of the data to retrieve.
     * @param <T> The expected type of the value.
     * @return An Optional containing the value if it exists and is of the correct type, otherwise an empty Optional.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Gets a value from the context, throwing an exception if it is not found.
     *
     * @param key The key of the data to retrieve.
     * @param <T> The expected type of the value.
     * @return The value.
     * @throws IllegalStateException if the key is not found or the value is not of the expected type.
     */
    public <T> T getRequired(String key, Class<T> type) {
        return get(key, type)
                .orElseThrow(() -> new IllegalStateException("Required key '" + key + "' not found in MatchingContext or is not of type " + type.getSimpleName()));
    }
}