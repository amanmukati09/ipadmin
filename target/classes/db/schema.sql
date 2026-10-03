-- =====================================================================================
-- Insurance Policy Administration System - Database Schema
-- MySQL 8.0, InnoDB engine, utf8mb4 / utf8mb4_0900_ai_ci charset/collation throughout
--
-- 17 tables total:
--   customer, agent, underwriter, policy, coverage_type, coverage, endorsement,
--   endorsement_history, premium_schedule, installment, payment, payment_allocation,
--   beneficiary, policy_status_history, premium_adjustment_history, renewal_notice,
--   audit_log
--
-- All foreign keys default to ON DELETE RESTRICT ON UPDATE RESTRICT, with the single
-- documented exception of coverage.policy_id (ON DELETE CASCADE), per design.md.
-- =====================================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS renewal_notice;
DROP TABLE IF EXISTS premium_adjustment_history;
DROP TABLE IF EXISTS policy_status_history;
DROP TABLE IF EXISTS beneficiary;
DROP TABLE IF EXISTS payment_allocation;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS installment;
DROP TABLE IF EXISTS premium_schedule;
DROP TABLE IF EXISTS endorsement_history;
DROP TABLE IF EXISTS endorsement;
DROP TABLE IF EXISTS coverage;
DROP TABLE IF EXISTS coverage_type;
DROP TABLE IF EXISTS policy;
DROP TABLE IF EXISTS underwriter;
DROP TABLE IF EXISTS agent;
DROP TABLE IF EXISTS customer;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================================
-- customer (Hibernate entity)
--
-- NOTE: segment_code is NOT part of design.md's documented column list for this table,
-- but is required by the already-implemented Customer.java Hibernate entity (field
-- segmentCode, values STANDARD/PREFERRED/HIGH_RISK) and by CustomerSegmentRouter per
-- design.md's util section. Added here as a documented schema-gap fix.
-- =====================================================================================
CREATE TABLE customer (
    customer_id     BIGINT          NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    date_of_birth   DATE            NOT NULL,
    email           VARCHAR(255)    NULL,
    phone           VARCHAR(20)     NULL,
    segment_code    VARCHAR(20)     NOT NULL, -- STANDARD / PREFERRED / HIGH_RISK (schema-gap fix for Customer.java)
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id),
    UNIQUE KEY uq_customer_email (email),
    UNIQUE KEY uq_customer_phone (phone),
    KEY idx_customer_last_first (last_name, first_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- agent (Hibernate entity)
-- =====================================================================================
CREATE TABLE agent (
    agent_id        BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    license_number  VARCHAR(20)     NOT NULL,
    email           VARCHAR(255)    NULL,
    phone           VARCHAR(20)     NULL,
    PRIMARY KEY (agent_id),
    UNIQUE KEY uq_agent_license (license_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- underwriter (JDBC/stored-procedure backed)
-- =====================================================================================
CREATE TABLE underwriter (
    underwriter_id  BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    license_id      VARCHAR(50)     NOT NULL,
    active_status   ENUM('active','inactive') NOT NULL DEFAULT 'active',
    PRIMARY KEY (underwriter_id),
    UNIQUE KEY uq_underwriter_license (license_id),
    KEY idx_underwriter_active_status (active_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- policy (JDBC/stored-procedure backed)
-- =====================================================================================
CREATE TABLE policy (
    policy_id         BIGINT          NOT NULL AUTO_INCREMENT,
    policy_number     VARCHAR(20)     NOT NULL,
    customer_id       BIGINT          NOT NULL,
    agent_id          BIGINT          NOT NULL,
    underwriter_id    BIGINT          NULL,
    policy_status     ENUM('quoted','bound','active','lapsed','cancelled','renewed') NOT NULL DEFAULT 'quoted',
    term_start_date   DATE            NOT NULL,
    term_end_date     DATE            NOT NULL,
    billing_frequency ENUM('monthly','quarterly','semi_annual','annual') NOT NULL,
    total_premium     DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_id),
    UNIQUE KEY uq_policy_number (policy_number),
    KEY idx_policy_customer (customer_id),
    KEY idx_policy_agent (agent_id),
    KEY idx_policy_underwriter (underwriter_id),
    KEY idx_policy_status (policy_status),
    KEY idx_policy_term_end_date (term_end_date),
    CONSTRAINT fk_policy_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_policy_agent FOREIGN KEY (agent_id) REFERENCES agent (agent_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_policy_underwriter FOREIGN KEY (underwriter_id) REFERENCES underwriter (underwriter_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- coverage_type (supporting lookup table, referenced by coverage FK)
-- =====================================================================================
CREATE TABLE coverage_type (
    coverage_type_id BIGINT          NOT NULL AUTO_INCREMENT,
    type_name        VARCHAR(50)     NOT NULL,
    PRIMARY KEY (coverage_type_id),
    UNIQUE KEY uq_coverage_type_name (type_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- coverage (JDBC/stored-procedure backed)
-- Documented exception: ON DELETE CASCADE for policy_id (coverage rows are owned
-- entirely by their policy and have no independent lifecycle).
-- =====================================================================================
CREATE TABLE coverage (
    coverage_id      BIGINT          NOT NULL AUTO_INCREMENT,
    policy_id        BIGINT          NOT NULL,
    coverage_type_id BIGINT          NOT NULL,
    coverage_limit   DECIMAL(12,2)   NOT NULL,
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (coverage_id),
    KEY idx_coverage_policy (policy_id),
    KEY idx_coverage_type (coverage_type_id),
    CONSTRAINT fk_coverage_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_coverage_type FOREIGN KEY (coverage_type_id) REFERENCES coverage_type (coverage_type_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- endorsement (JDBC/stored-procedure backed)
-- =====================================================================================
CREATE TABLE endorsement (
    endorsement_id    BIGINT          NOT NULL AUTO_INCREMENT,
    policy_id         BIGINT          NOT NULL,
    description       VARCHAR(1000)   NOT NULL,
    effective_date    DATE            NOT NULL,
    premium_adjustment DECIMAL(12,2)  NOT NULL,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (endorsement_id),
    KEY idx_endorsement_policy (policy_id),
    KEY idx_endorsement_effective_date (effective_date),
    CONSTRAINT fk_endorsement_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- endorsement_history (audit table)
-- =====================================================================================
CREATE TABLE endorsement_history (
    endorsement_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    endorsement_id          BIGINT      NOT NULL,
    action                  VARCHAR(20) NOT NULL,
    recorded_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (endorsement_history_id),
    KEY idx_endorsement_history_endorsement (endorsement_id),
    CONSTRAINT fk_endorsement_history_endorsement FOREIGN KEY (endorsement_id) REFERENCES endorsement (endorsement_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- premium_schedule (JDBC/stored-procedure backed)
-- =====================================================================================
CREATE TABLE premium_schedule (
    premium_schedule_id BIGINT        NOT NULL AUTO_INCREMENT,
    policy_id           BIGINT        NOT NULL,
    billing_frequency   ENUM('monthly','quarterly','semi_annual','annual') NOT NULL,
    total_premium        DECIMAL(12,2) NOT NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (premium_schedule_id),
    UNIQUE KEY uq_premium_schedule_policy (policy_id),
    CONSTRAINT fk_premium_schedule_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- installment (JDBC/stored-procedure backed)
-- =====================================================================================
CREATE TABLE installment (
    installment_id      BIGINT          NOT NULL AUTO_INCREMENT,
    premium_schedule_id BIGINT          NOT NULL,
    due_date             DATE            NOT NULL,
    amount_due            DECIMAL(12,2)   NOT NULL,
    amount_paid            DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    installment_status     ENUM('unpaid','paid') NOT NULL DEFAULT 'unpaid',
    PRIMARY KEY (installment_id),
    KEY idx_installment_schedule (premium_schedule_id),
    KEY idx_installment_due_date (due_date),
    KEY idx_installment_status (installment_status),
    CONSTRAINT fk_installment_schedule FOREIGN KEY (premium_schedule_id) REFERENCES premium_schedule (premium_schedule_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- payment (JDBC/stored-procedure backed)
-- =====================================================================================
CREATE TABLE payment (
    payment_id      BIGINT          NOT NULL AUTO_INCREMENT,
    policy_id       BIGINT          NOT NULL,
    payment_amount  DECIMAL(12,2)   NOT NULL,
    payment_date    DATE            NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    KEY idx_payment_policy (policy_id),
    KEY idx_payment_date (payment_date),
    CONSTRAINT fk_payment_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- payment_allocation (supporting table, references payment and installment)
-- =====================================================================================
CREATE TABLE payment_allocation (
    payment_allocation_id BIGINT        NOT NULL AUTO_INCREMENT,
    payment_id             BIGINT        NOT NULL,
    installment_id         BIGINT        NOT NULL,
    amount_applied           DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (payment_allocation_id),
    KEY idx_payment_allocation_payment (payment_id),
    KEY idx_payment_allocation_installment (installment_id),
    CONSTRAINT fk_payment_allocation_payment FOREIGN KEY (payment_id) REFERENCES payment (payment_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_allocation_installment FOREIGN KEY (installment_id) REFERENCES installment (installment_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- beneficiary (Hibernate entity)
-- =====================================================================================
CREATE TABLE beneficiary (
    beneficiary_id        BIGINT          NOT NULL AUTO_INCREMENT,
    policy_id             BIGINT          NOT NULL,
    name                  VARCHAR(100)    NOT NULL,
    relationship_type     VARCHAR(20) NOT NULL,
    allocation_percentage DECIMAL(5,2)    NOT NULL,
    PRIMARY KEY (beneficiary_id),
    KEY idx_beneficiary_policy (policy_id),
    CONSTRAINT fk_beneficiary_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- policy_status_history (audit table)
-- =====================================================================================
CREATE TABLE policy_status_history (
    policy_status_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    policy_id                 BIGINT      NOT NULL,
    previous_status            VARCHAR(20) NULL,
    new_status                   VARCHAR(20) NOT NULL,
    transitioned_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_status_history_id),
    KEY idx_status_history_policy (policy_id),
    KEY idx_status_history_transitioned_at (transitioned_at),
    CONSTRAINT fk_status_history_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- premium_adjustment_history (audit table)
-- =====================================================================================
CREATE TABLE premium_adjustment_history (
    premium_adjustment_history_id BIGINT        NOT NULL AUTO_INCREMENT,
    policy_id                      BIGINT        NOT NULL,
    endorsement_id                  BIGINT        NOT NULL,
    previous_premium                 DECIMAL(12,2) NOT NULL,
    new_premium                        DECIMAL(12,2) NOT NULL,
    adjustment_amount                   DECIMAL(12,2) NOT NULL,
    adjusted_at                          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (premium_adjustment_history_id),
    KEY idx_premium_adj_policy (policy_id),
    KEY idx_premium_adj_endorsement (endorsement_id),
    CONSTRAINT fk_premium_adj_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_premium_adj_endorsement FOREIGN KEY (endorsement_id) REFERENCES endorsement (endorsement_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- renewal_notice (audit table)
-- =====================================================================================
CREATE TABLE renewal_notice (
    renewal_notice_id       BIGINT      NOT NULL AUTO_INCREMENT,
    policy_id                BIGINT      NOT NULL,
    generated_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    term_end_date_snapshot       DATE        NOT NULL,
    PRIMARY KEY (renewal_notice_id),
    KEY idx_renewal_notice_policy (policy_id),
    KEY idx_renewal_notice_generated_at (generated_at),
    CONSTRAINT fk_renewal_notice_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================================
-- audit_log (supporting, generic append-only log; no FK — referenced by stored
-- procedures that write entity_name/entity_id rows for various entities)
-- =====================================================================================
CREATE TABLE audit_log (
    audit_log_id   BIGINT          NOT NULL AUTO_INCREMENT,
    entity_name    VARCHAR(50)     NOT NULL,
    entity_id      BIGINT          NOT NULL,
    action         VARCHAR(20)     NOT NULL,
    logged_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_log_id),
    KEY idx_audit_log_entity (entity_name, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
