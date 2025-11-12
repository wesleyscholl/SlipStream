package com.slipstream.config;

/**
 * Interface for providing environment variable access.
 * Allows for easier testing by enabling dependency injection of environment variable providers.
 */
public interface EnvironmentVariableProvider {
    
    /**
     * Gets the value of an environment variable.
     * 
     * @param name the name of the environment variable
     * @return the value of the environment variable, or null if not set
     */
    String getenv(String name);
}