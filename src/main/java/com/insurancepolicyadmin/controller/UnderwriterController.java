package com.insurancepolicyadmin.controller;

import com.insurancepolicyadmin.dto.request.UnderwriterCreateRequest;
import com.insurancepolicyadmin.dto.response.UnderwriterResponse;
import com.insurancepolicyadmin.service.UnderwriterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Underwriter management (design.md REST API table, endpoints 27-28).
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/underwriters} - create an Underwriter (endpoint 27)</li>
 *   <li>{@code GET  /api/underwriters/{underwriterId}} - get an Underwriter by id (endpoint 28)</li>
 * </ul>
 *
 * <p>This is a thin controller: it delegates all business logic to {@link UnderwriterService}.</p>
 */
@RestController
@RequestMapping("/underwriters")
public class UnderwriterController {

    private final UnderwriterService underwriterService;

    public UnderwriterController(UnderwriterService underwriterService) {
        this.underwriterService = underwriterService;
    }

    /**
     * Creates a new Underwriter (design.md endpoint 27).
     *
     * @param request the Underwriter field values
     * @return 201 with the created Underwriter
     */
    @PostMapping
    public ResponseEntity<UnderwriterResponse> create(@RequestBody UnderwriterCreateRequest request) {
        UnderwriterResponse response = underwriterService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves an Underwriter by identifier (design.md endpoint 28).
     *
     * @param underwriterId the Underwriter identifier
     * @return 200 with the Underwriter
     */
    @GetMapping("/{underwriterId}")
    public ResponseEntity<UnderwriterResponse> getById(@PathVariable Long underwriterId) {
        return ResponseEntity.ok(underwriterService.getById(underwriterId));
    }
}
