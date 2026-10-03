-- =====================================================================================
-- Insurance Policy Administration System - Stored Procedures: Policy / Coverage
-- MySQL 8.0
--
-- Contents:
--   CRUD (4 procs)        : policy          -> sp_policy_create, sp_policy_read,
--                                              sp_policy_update, sp_policy_delete
--   Support CRUD (4 procs): coverage / coverage_type ->
--                                              sp_coverage_create, sp_coverage_get_by_policy_id,
--                                              sp_coverage_type_create, sp_coverage_type_list
--   Business logic        : sp_policy_status_transition (transactional, OUT status code)
--                            sp_policy_search (AND-combination of optional filters)
--
-- Per Requirement 18.19, the CRUD procedures below contain only parameter validation
-- plus the single corresponding data operation -- no branching beyond that.
-- =====================================================================================

DELIMITER //

-- =====================================================================================
-- sp_policy_create
-- Creates a new Policy row with initial policy_status = 'quoted'.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_policy_create //
CREATE PROCEDURE sp_policy_create (
    IN  p_policy_number     VARCHAR(20),
    IN  p_customer_id       BIGINT,
    IN  p_agent_id          BIGINT,
    IN  p_term_start_date   DATE,
    IN  p_term_end_date     DATE,
    IN  p_billing_frequency VARCHAR(20),
    OUT p_policy_id         BIGINT
)
BEGIN
    INSERT INTO policy (policy_number, customer_id, agent_id, policy_status,
                         term_start_date, term_end_date, billing_frequency, total_premium)
    VALUES (p_policy_number, p_customer_id, p_agent_id, 'quoted',
            p_term_start_date, p_term_end_date, p_billing_frequency, 0.00);

    SET p_policy_id = LAST_INSERT_ID();
END //

-- =====================================================================================
-- sp_policy_read
-- Reads a single Policy row by policy_id.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_policy_read //
CREATE PROCEDURE sp_policy_read (
    IN p_policy_id BIGINT
)
BEGIN
    SELECT policy_id, policy_number, customer_id, agent_id, underwriter_id, policy_status,
           term_start_date, term_end_date, billing_frequency, total_premium, created_at
    FROM policy
    WHERE policy_id = p_policy_id;
END //

-- =====================================================================================
-- sp_policy_update
-- Updates the mutable, non-status fields of a Policy row.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_policy_update //
CREATE PROCEDURE sp_policy_update (
    IN p_policy_id         BIGINT,
    IN p_agent_id          BIGINT,
    IN p_underwriter_id    BIGINT,
    IN p_term_end_date     DATE,
    IN p_billing_frequency VARCHAR(20),
    IN p_total_premium     DECIMAL(12,2)
)
BEGIN
    UPDATE policy
    SET agent_id          = p_agent_id,
        underwriter_id    = p_underwriter_id,
        term_end_date     = p_term_end_date,
        billing_frequency = p_billing_frequency,
        total_premium     = p_total_premium
    WHERE policy_id = p_policy_id;
END //

-- =====================================================================================
-- sp_policy_delete
-- Deletes a Policy row by policy_id.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_policy_delete //
CREATE PROCEDURE sp_policy_delete (
    IN p_policy_id BIGINT
)
BEGIN
    DELETE FROM policy WHERE policy_id = p_policy_id;
END //

-- =====================================================================================
-- sp_coverage_create
-- Creates a new Coverage row for a Policy.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_coverage_create //
CREATE PROCEDURE sp_coverage_create (
    IN  p_policy_id        BIGINT,
    IN  p_coverage_type_id BIGINT,
    IN  p_coverage_limit   DECIMAL(12,2),
    OUT p_coverage_id      BIGINT
)
BEGIN
    INSERT INTO coverage (policy_id, coverage_type_id, coverage_limit)
    VALUES (p_policy_id, p_coverage_type_id, p_coverage_limit);

    SET p_coverage_id = LAST_INSERT_ID();
END //

-- =====================================================================================
-- sp_coverage_get_by_policy_id
-- Lists all Coverage rows for a given Policy.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_coverage_get_by_policy_id //
CREATE PROCEDURE sp_coverage_get_by_policy_id (
    IN p_policy_id BIGINT
)
BEGIN
    SELECT coverage_id, policy_id, coverage_type_id, coverage_limit, created_at
    FROM coverage
    WHERE policy_id = p_policy_id
    ORDER BY coverage_id ASC;
END //

-- =====================================================================================
-- sp_coverage_type_create
-- Creates a new Coverage_Type lookup row.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_coverage_type_create //
CREATE PROCEDURE sp_coverage_type_create (
    IN  p_type_name       VARCHAR(50),
    OUT p_coverage_type_id BIGINT
)
BEGIN
    INSERT INTO coverage_type (type_name)
    VALUES (p_type_name);

    SET p_coverage_type_id = LAST_INSERT_ID();
END //

-- =====================================================================================
-- sp_coverage_type_list
-- Lists all Coverage_Type lookup rows.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_coverage_type_list //
CREATE PROCEDURE sp_coverage_type_list ()
BEGIN
    SELECT coverage_type_id, type_name
    FROM coverage_type
    ORDER BY type_name ASC;
END //

-- =====================================================================================
-- sp_policy_status_transition
-- Validates and executes a Policy_Status transition, inserting a Policy_Status_History
-- record atomically with the policy update.
--
-- Permitted transitions (per design.md's state machine):
--   quoted  -> bound
--   bound   -> active
--   active  -> lapsed
--   active  -> cancelled
--   active  -> renewed
--   lapsed  -> cancelled
--
-- p_status_code OUT values:
--   0 = success
--   1 = invalid transition (current/new status pair not in the permitted set)
--   2 = policy not found
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_policy_status_transition //
CREATE PROCEDURE sp_policy_status_transition (
    IN  p_policy_id   BIGINT,
    IN  p_new_status  VARCHAR(20),
    OUT p_status_code INT
)
BEGIN
    DECLARE v_current_status VARCHAR(20);
    DECLARE v_found_count    INT;
    DECLARE v_is_permitted   INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT COUNT(*), MAX(policy_status)
    INTO v_found_count, v_current_status
    FROM policy
    WHERE policy_id = p_policy_id
    FOR UPDATE;

    IF v_found_count = 0 THEN
        SET p_status_code = 2;
        ROLLBACK;
    ELSE
        IF  (v_current_status = 'quoted' AND p_new_status = 'bound')
         OR (v_current_status = 'bound'  AND p_new_status = 'active')
         OR (v_current_status = 'active' AND p_new_status = 'lapsed')
         OR (v_current_status = 'active' AND p_new_status = 'cancelled')
         OR (v_current_status = 'active' AND p_new_status = 'renewed')
         OR (v_current_status = 'lapsed' AND p_new_status = 'cancelled')
        THEN
            SET v_is_permitted = 1;
        END IF;

        IF v_is_permitted = 0 THEN
            SET p_status_code = 1;
            ROLLBACK;
        ELSE
            UPDATE policy
            SET policy_status = p_new_status
            WHERE policy_id = p_policy_id;

            INSERT INTO policy_status_history (policy_id, previous_status, new_status)
            VALUES (p_policy_id, v_current_status, p_new_status);

            SET p_status_code = 0;
            COMMIT;
        END IF;
    END IF;
END //

-- =====================================================================================
-- sp_policy_search
-- Searches Policy records by any combination of customer_id, policy_number,
-- policy_status, and agent_id. Any supplied, non-NULL parameter restricts the result
-- set (AND-combined); any omitted/NULL parameter is excluded from the filter entirely.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_policy_search //
CREATE PROCEDURE sp_policy_search (
    IN p_customer_id   BIGINT,
    IN p_policy_number VARCHAR(20),
    IN p_status        VARCHAR(20),
    IN p_agent_id      BIGINT
)
BEGIN
    SELECT policy_id, policy_number, customer_id, agent_id, underwriter_id, policy_status,
           term_start_date, term_end_date, billing_frequency, total_premium, created_at
    FROM policy
    WHERE (p_customer_id   IS NULL OR customer_id   = p_customer_id)
      AND (p_policy_number IS NULL OR policy_number = p_policy_number)
      AND (p_status        IS NULL OR policy_status = p_status)
      AND (p_agent_id       IS NULL OR agent_id      = p_agent_id)
    ORDER BY policy_id ASC;
END //

DELIMITER ;
