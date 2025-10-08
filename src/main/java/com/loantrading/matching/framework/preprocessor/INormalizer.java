package com.loantrading.matching.framework.preprocessor;

/**
 * An interface for components that normalize data into a canonical format.
 *
 * Normalization is a critical pre-processing step to clean and standardize
 * data before matching, reducing superficial differences. Implementations
 * could perform tasks like stripping corporate suffixes from names,
 * standardizing addresses, or formatting dates consistently.
 *
 * @param <T> The type of the data to be normalized (e.g., String, Date).
 */
@FunctionalInterface
public interface INormalizer<T> {

    /**
     * Normalizes the given input value.
     *
     * @param input The value to be normalized.
     * @return The normalized value.
     */
    T normalize(T input);
}