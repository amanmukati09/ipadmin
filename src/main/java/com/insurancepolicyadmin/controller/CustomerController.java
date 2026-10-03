package com.insurancepolicyadmin.controller;

import com.insurancepolicyadmin.dto.request.CustomerCreateRequest;
import com.insurancepolicyadmin.dto.response.CustomerResponse;
import com.insurancepolicyadmin.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Customer management (design.md REST API table, endpoints 16-18).
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/customers} - create a Customer (endpoint 16)</li>
 *   <li>{@code GET  /api/customers/{customerId}} - get a Customer by id (endpoint 17)</li>
 *   <li>{@code GET  /api/customers/search} - search Customers (endpoint 18)</li>
 * </ul>
 *
 * <p>This is a thin controller: it delegates all business logic (field validation,
 * duplicate rejection, search-criteria handling) to {@link CustomerService}.</p>
 */
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Creates a new Customer (design.md endpoint 16).
     *
     * @param request the Customer field values
     * @return 201 with the created Customer
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a Customer by identifier (design.md endpoint 17).
     *
     * @param customerId the Customer identifier
     * @return 200 with the Customer
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getById(customerId));
    }

    /**
     * Searches Customers by last name and/or first name (design.md endpoint 18). At least one
     * criterion is required; the service enforces that rule and raises {@code NO_SEARCH_CRITERIA}
     * when none is supplied.
     *
     * @param lastName  the optional last-name criterion
     * @param firstName the optional first-name criterion
     * @return 200 with the matching Customers (up to 100)
     */
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> search(
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "firstName", required = false) String firstName) {
        return ResponseEntity.ok(customerService.search(lastName, firstName));
    }
}
