package com.insurancepolicyadmin.controller;

import com.insurancepolicyadmin.dto.request.AgentCreateRequest;
import com.insurancepolicyadmin.dto.request.AgentUpdateRequest;
import com.insurancepolicyadmin.dto.response.AgentResponse;
import com.insurancepolicyadmin.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Agent management (design.md REST API table, endpoints 19-22).
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST   /api/agents} - create an Agent (endpoint 19)</li>
 *   <li>{@code GET    /api/agents/{agentId}} - get an Agent by id (endpoint 20)</li>
 *   <li>{@code PUT    /api/agents/{agentId}} - update an Agent (endpoint 21)</li>
 *   <li>{@code DELETE /api/agents/{agentId}} - delete an Agent (endpoint 22)</li>
 * </ul>
 *
 * <p>This is a thin controller: it delegates all business logic to the Agent methods of
 * {@link CustomerService} (which owns both Customer and Agent management per the design).</p>
 */
@RestController
@RequestMapping("/agents")
public class AgentController {

    private final CustomerService customerService;

    public AgentController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Creates a new Agent (design.md endpoint 19).
     *
     * @param request the Agent field values
     * @return 201 with the created Agent
     */
    @PostMapping
    public ResponseEntity<AgentResponse> create(@RequestBody AgentCreateRequest request) {
        AgentResponse response = customerService.createAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves an Agent by identifier (design.md endpoint 20).
     *
     * @param agentId the Agent identifier
     * @return 200 with the Agent
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<AgentResponse> getById(@PathVariable Long agentId) {
        return ResponseEntity.ok(customerService.getAgentById(agentId));
    }

    /**
     * Updates an existing Agent (design.md endpoint 21).
     *
     * @param agentId the Agent identifier
     * @param request the updated Agent field values
     * @return 200 with the updated Agent
     */
    @PutMapping("/{agentId}")
    public ResponseEntity<AgentResponse> update(@PathVariable Long agentId,
                                                @RequestBody AgentUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateAgent(agentId, request));
    }

    /**
     * Deletes an Agent (design.md endpoint 22).
     *
     * @param agentId the Agent identifier
     * @return 204 with no content
     */
    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> delete(@PathVariable Long agentId) {
        customerService.deleteAgent(agentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
