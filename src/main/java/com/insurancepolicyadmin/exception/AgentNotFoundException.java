package com.insurancepolicyadmin.exception;

/**
 * Thrown when a requested Agent identifier does not exist.
 * Validates: Requirements 4.4, 4.7, 7.3, 13.8.
 */
public class AgentNotFoundException extends ApplicationException {

    public AgentNotFoundException(String message) {
        super("AGENT_NOT_FOUND", 404, message);
    }

    public AgentNotFoundException(Long agentId) {
        super("AGENT_NOT_FOUND", 404, "Agent not found: " + agentId);
    }
}
