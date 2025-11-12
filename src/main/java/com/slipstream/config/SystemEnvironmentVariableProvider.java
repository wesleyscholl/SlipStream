package com.slipstream.config;

/**
 * Default implementation of EnvironmentVariableProvider that delegates to System.getenv().
 */
public class SystemEnvironmentVariableProvider implements EnvironmentVariableProvider {
    
    @Override
    public String getenv(String name) {
        return System.getenv(name);
    }
}