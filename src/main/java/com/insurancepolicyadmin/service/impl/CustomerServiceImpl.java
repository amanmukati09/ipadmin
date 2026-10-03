package com.insurancepolicyadmin.service.impl;

import com.insurancepolicyadmin.dao.AgentDao;
import com.insurancepolicyadmin.dao.CustomerDao;
import com.insurancepolicyadmin.dto.request.AgentCreateRequest;
import com.insurancepolicyadmin.dto.request.AgentUpdateRequest;
import com.insurancepolicyadmin.dto.request.CustomerCreateRequest;
import com.insurancepolicyadmin.dto.response.AgentResponse;
import com.insurancepolicyadmin.dto.response.CustomerResponse;
import com.insurancepolicyadmin.exception.AgentNotFoundException;
import com.insurancepolicyadmin.exception.CustomerNotFoundException;
import com.insurancepolicyadmin.exception.DuplicateCustomerException;
import com.insurancepolicyadmin.exception.DuplicateLicenseException;
import com.insurancepolicyadmin.exception.FieldError;
import com.insurancepolicyadmin.exception.ValidationException;
import com.insurancepolicyadmin.model.entity.Agent;
import com.insurancepolicyadmin.model.entity.Customer;
import com.insurancepolicyadmin.service.CustomerService;
import com.insurancepolicyadmin.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Customer management service (Requirement 3) and Agent management service (Requirement 4).
 *
 * <p>Delegates all Customer persistence to the Hibernate-backed {@link CustomerDao} and all
 * Agent persistence to the Hibernate-backed {@link AgentDao}. Field validation uses the static
 * {@link ValidationUtils} helpers, aggregating every per-field failure into a single
 * {@link ValidationException} so the caller sees all problems at once (Req 3.3). Duplicate
 * Customer email/phone submissions are rejected before persistence via
 * {@link CustomerDao#existsByEmailOrPhone(String, String)} (Req 3.8); duplicate Agent license
 * numbers are rejected via {@link AgentDao#existsByLicenseNumber(String)} (Req 4.2).</p>
 *
 * <p>Transactions are bound to the Hibernate transaction manager, matching the DAOs that own
 * Customer and Agent writes. Read operations are marked {@code readOnly}.</p>
 */
@Service
@Transactional("hibernateTransactionManager")
public class CustomerServiceImpl implements CustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MIN_LICENSE_LENGTH = 5;
    private static final int MAX_LICENSE_LENGTH = 20;

    private final CustomerDao customerDao;
    private final AgentDao agentDao;

    @Autowired
    public CustomerServiceImpl(CustomerDao customerDao, AgentDao agentDao) {
        this.customerDao = customerDao;
        this.agentDao = agentDao;
    }

    @Override
    public CustomerResponse create(CustomerCreateRequest request) {
        LOG.info("Creating Customer");
        validateFields(request);
        String email = trimToNull(request.getEmail());
        String phone = trimToNull(request.getPhone());
        // Reject duplicate email/phone before persisting (Req 3.8).
        if (customerDao.existsByEmailOrPhone(email, phone)) {
            throw new DuplicateCustomerException(
                    "A Customer with the same email or phone already exists");
        }
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName().trim());
        customer.setLastName(request.getLastName().trim());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setSegmentCode(trimToNull(request.getSegmentCode()));
        Long generatedId = customerDao.create(customer);
        customer.setCustomerId(generatedId);
        LOG.info("Created Customer {}", generatedId);
        return toResponse(customer);
    }

    @Override
    @Transactional(value = "hibernateTransactionManager", readOnly = true)
    public List<CustomerResponse> search(String lastName, String firstName) {
        String lastNameCriterion = trimToNull(lastName);
        String firstNameCriterion = trimToNull(firstName);
        // At least one search criterion is required (Req 3.7).
        if (lastNameCriterion == null && firstNameCriterion == null) {
            throw new ValidationException("At least one search criterion is required");
        }
        List<Customer> customers = customerDao.search(lastNameCriterion, firstNameCriterion);
        List<CustomerResponse> responses = new ArrayList<>();
        if (customers != null) {
            for (Customer customer : customers) {
                responses.add(toResponse(customer));
            }
        }
        // Empty list on no results is not an error (Req 3.6).
        return responses;
    }

    @Override
    @Transactional(value = "hibernateTransactionManager", readOnly = true)
    public CustomerResponse getById(Long customerId) {
        if (customerId == null) {
            throw new CustomerNotFoundException("Customer identifier is required");
        }
        Optional<Customer> customer = customerDao.findById(customerId);
        if (!customer.isPresent()) {
            throw new CustomerNotFoundException(customerId);
        }
        return toResponse(customer.get());
    }

    // ---- Agent management (Requirement 4) -------------------------------

    @Override
    public AgentResponse createAgent(AgentCreateRequest request) {
        LOG.info("Creating Agent");
        if (request == null) {
            throw new ValidationException("Agent request body is required");
        }
        String name = trimToNull(request.getName());
        String licenseNumber = trimToNull(request.getLicenseNumber());
        String email = trimToNull(request.getEmail());
        String phone = trimToNull(request.getPhone());
        validateAgentFields(name, licenseNumber, email, phone);
        // Reject a duplicate license number before persisting (Req 4.2).
        if (agentDao.existsByLicenseNumber(licenseNumber)) {
            throw new DuplicateLicenseException(
                    "An Agent with license number " + licenseNumber + " already exists");
        }
        Agent agent = new Agent();
        agent.setName(name);
        agent.setLicenseNumber(licenseNumber);
        agent.setEmail(email);
        agent.setPhone(phone);
        Long generatedId = agentDao.create(agent);
        agent.setAgentId(generatedId);
        LOG.info("Created Agent {}", generatedId);
        return toAgentResponse(agent);
    }

    @Override
    @Transactional(value = "hibernateTransactionManager", readOnly = true)
    public AgentResponse getAgentById(Long agentId) {
        if (agentId == null) {
            throw new AgentNotFoundException("Agent identifier is required");
        }
        Optional<Agent> agent = agentDao.findById(agentId);
        if (!agent.isPresent()) {
            throw new AgentNotFoundException(agentId);
        }
        return toAgentResponse(agent.get());
    }

    @Override
    public AgentResponse updateAgent(Long agentId, AgentUpdateRequest request) {
        LOG.info("Updating Agent {}", agentId);
        if (agentId == null) {
            throw new AgentNotFoundException("Agent identifier is required");
        }
        if (request == null) {
            throw new ValidationException("Agent request body is required");
        }
        Optional<Agent> existing = agentDao.findById(agentId);
        if (!existing.isPresent()) {
            throw new AgentNotFoundException(agentId);
        }
        String name = trimToNull(request.getName());
        String licenseNumber = trimToNull(request.getLicenseNumber());
        String email = trimToNull(request.getEmail());
        String phone = trimToNull(request.getPhone());
        validateAgentFields(name, licenseNumber, email, phone);
        Agent agent = existing.get();
        // If the license number changes, re-check for a duplicate (Req 4.2).
        if (!licenseNumber.equals(agent.getLicenseNumber())
                && agentDao.existsByLicenseNumber(licenseNumber)) {
            throw new DuplicateLicenseException(
                    "An Agent with license number " + licenseNumber + " already exists");
        }
        agent.setName(name);
        agent.setLicenseNumber(licenseNumber);
        agent.setEmail(email);
        agent.setPhone(phone);
        agentDao.update(agent);
        LOG.info("Updated Agent {}", agentId);
        return toAgentResponse(agent);
    }

    @Override
    public void deleteAgent(Long agentId) {
        LOG.info("Deleting Agent {}", agentId);
        if (agentId == null) {
            throw new AgentNotFoundException("Agent identifier is required");
        }
        Optional<Agent> existing = agentDao.findById(agentId);
        if (!existing.isPresent()) {
            throw new AgentNotFoundException(agentId);
        }
        agentDao.delete(agentId);
        LOG.info("Deleted Agent {}", agentId);
    }

    // ---- helpers --------------------------------------------------------

    /**
     * Validates all Customer fields and aggregates every failure into one
     * {@link ValidationException} (Req 3.3). Contact information requires at least one of email
     * or phone; whichever is supplied must match its format via {@link ValidationUtils}.
     */
    private void validateFields(CustomerCreateRequest request) {
        if (request == null) {
            throw new ValidationException("Customer request body is required");
        }
        List<FieldError> errors = new ArrayList<>();
        String firstName = request.getFirstName();
        if (firstName == null || firstName.trim().isEmpty()) {
            errors.add(new FieldError("firstName", "First name is required"));
        } else if (firstName.trim().length() > MAX_NAME_LENGTH) {
            errors.add(new FieldError("firstName",
                    "First name must not exceed " + MAX_NAME_LENGTH + " characters"));
        }
        String lastName = request.getLastName();
        if (lastName == null || lastName.trim().isEmpty()) {
            errors.add(new FieldError("lastName", "Last name is required"));
        } else if (lastName.trim().length() > MAX_NAME_LENGTH) {
            errors.add(new FieldError("lastName",
                    "Last name must not exceed " + MAX_NAME_LENGTH + " characters"));
        }
        if (request.getDateOfBirth() == null) {
            errors.add(new FieldError("dateOfBirth", "Date of birth is required"));
        } else if (!ValidationUtils.isNotFutureDate(request.getDateOfBirth())) {
            errors.add(new FieldError("dateOfBirth", "Date of birth must not be in the future"));
        }
        String email = trimToNull(request.getEmail());
        String phone = trimToNull(request.getPhone());
        // Contact information: at least one of email or phone is required (Req 3.1/3.3).
        if (email == null && phone == null) {
            errors.add(new FieldError("contactInformation",
                    "At least one of email or phone is required"));
        }
        if (email != null) {
            if (email.length() > MAX_EMAIL_LENGTH) {
                errors.add(new FieldError("email",
                        "Email must not exceed " + MAX_EMAIL_LENGTH + " characters"));
            } else if (!ValidationUtils.isValidEmail(email)) {
                errors.add(new FieldError("email", "Email format is invalid"));
            }
        }
        if (phone != null) {
            if (phone.length() > MAX_PHONE_LENGTH) {
                errors.add(new FieldError("phone",
                        "Phone must not exceed " + MAX_PHONE_LENGTH + " characters"));
            } else if (!ValidationUtils.isValidPhone(phone)) {
                errors.add(new FieldError("phone", "Phone format is invalid"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Customer validation failed", errors);
        }
    }

    /**
     * Validates Agent fields and aggregates every failure into one {@link ValidationException}
     * (Req 4.1/4.5): name 1-100 characters, license number alphanumeric 5-20 characters, and
     * contact information requiring at least one valid email or phone.
     */
    private void validateAgentFields(String name, String licenseNumber, String email,
            String phone) {
        List<FieldError> errors = new ArrayList<>();
        if (name == null) {
            errors.add(new FieldError("name", "Name is required"));
        } else if (name.length() > MAX_NAME_LENGTH) {
            errors.add(new FieldError("name",
                    "Name must not exceed " + MAX_NAME_LENGTH + " characters"));
        }
        if (licenseNumber == null) {
            errors.add(new FieldError("licenseNumber", "License number is required"));
        } else if (licenseNumber.length() < MIN_LICENSE_LENGTH
                || licenseNumber.length() > MAX_LICENSE_LENGTH
                || !licenseNumber.matches("[A-Za-z0-9]+")) {
            errors.add(new FieldError("licenseNumber",
                    "License number must be alphanumeric and between " + MIN_LICENSE_LENGTH
                            + " and " + MAX_LICENSE_LENGTH + " characters"));
        }
        // Contact information: at least one of email or phone is required (Req 4.1/4.5).
        if (email == null && phone == null) {
            errors.add(new FieldError("contactInformation",
                    "At least one of email or phone is required"));
        }
        if (email != null) {
            if (email.length() > MAX_EMAIL_LENGTH) {
                errors.add(new FieldError("email",
                        "Email must not exceed " + MAX_EMAIL_LENGTH + " characters"));
            } else if (!ValidationUtils.isValidEmail(email)) {
                errors.add(new FieldError("email", "Email format is invalid"));
            }
        }
        if (phone != null) {
            if (phone.length() > MAX_PHONE_LENGTH) {
                errors.add(new FieldError("phone",
                        "Phone must not exceed " + MAX_PHONE_LENGTH + " characters"));
            } else if (!ValidationUtils.isValidPhone(phone)) {
                errors.add(new FieldError("phone", "Phone format is invalid"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Agent validation failed", errors);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setCustomerId(customer.getCustomerId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setDateOfBirth(customer.getDateOfBirth());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setSegmentCode(customer.getSegmentCode());
        response.setCreatedAt(customer.getCreatedAt());
        return response;
    }

    private static AgentResponse toAgentResponse(Agent agent) {
        AgentResponse response = new AgentResponse();
        response.setAgentId(agent.getAgentId());
        response.setName(agent.getName());
        response.setLicenseNumber(agent.getLicenseNumber());
        response.setEmail(agent.getEmail());
        response.setPhone(agent.getPhone());
        return response;
    }
}
