package com.insurancepolicyadmin.service.impl;

import com.insurancepolicyadmin.dao.UnderwriterDao;
import com.insurancepolicyadmin.dto.request.UnderwriterCreateRequest;
import com.insurancepolicyadmin.dto.response.UnderwriterResponse;
import com.insurancepolicyadmin.exception.UnderwriterNotFoundException;
import com.insurancepolicyadmin.model.plain.Underwriter;
import com.insurancepolicyadmin.service.UnderwriterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Underwriter management service (design.md REST API table, endpoints 27-28).
 *
 * <p>Delegates all Underwriter persistence to the Spring-JDBC-backed {@link UnderwriterDao}
 * (stored procedures through {@code SimpleJdbcCall}). Transactions are bound to the JDBC
 * transaction manager, matching the DAO that owns Underwriter writes; the read operation is
 * marked {@code readOnly}.</p>
 */
@Service
@Transactional("jdbcTransactionManager")
public class UnderwriterServiceImpl implements UnderwriterService {

    private static final Logger LOG = LoggerFactory.getLogger(UnderwriterServiceImpl.class);

    private final UnderwriterDao underwriterDao;

    @Autowired
    public UnderwriterServiceImpl(UnderwriterDao underwriterDao) {
        this.underwriterDao = underwriterDao;
    }

    @Override
    public UnderwriterResponse create(UnderwriterCreateRequest request) {
        LOG.info("Creating Underwriter");
        Underwriter underwriter = new Underwriter();
        underwriter.setName(request.getName());
        underwriter.setLicenseId(request.getLicenseId());
        underwriter.setActiveStatus(request.getActiveStatus());
        Long underwriterId = underwriterDao.create(underwriter);
        underwriter.setUnderwriterId(underwriterId);
        return toResponse(underwriter);
    }

    @Override
    @Transactional("jdbcTransactionManager")
    public UnderwriterResponse getById(Long underwriterId) {
        Underwriter underwriter = underwriterDao.findById(underwriterId)
                .orElseThrow(() -> new UnderwriterNotFoundException(underwriterId));
        return toResponse(underwriter);
    }

    private UnderwriterResponse toResponse(Underwriter underwriter) {
        UnderwriterResponse response = new UnderwriterResponse();
        response.setUnderwriterId(underwriter.getUnderwriterId());
        response.setName(underwriter.getName());
        response.setLicenseId(underwriter.getLicenseId());
        response.setActiveStatus(underwriter.getActiveStatus());
        return response;
    }
}
