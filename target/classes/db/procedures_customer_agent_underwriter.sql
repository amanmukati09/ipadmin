-- ============================================================================
-- Insurance Policy Administration System - Stored Procedures
-- Domain: customer, agent, underwriter
--
-- 12 single-operation CRUD procedures (4 each for customer, agent, underwriter)
-- plus 1 helper procedure (sp_underwriter_is_active).
--
-- Per Requirement 18.19: the CRUD procedures below contain ONLY parameter
-- validation (NULL/required-field checks signaled via SIGNAL SQLSTATE) plus
-- the single corresponding data operation. No additional branching,
-- multi-step logic, or secondary data operations are present.
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_customer_create;
DROP PROCEDURE IF EXISTS sp_customer_get_by_id;
DROP PROCEDURE IF EXISTS sp_customer_update;
DROP PROCEDURE IF EXISTS sp_customer_delete;
DROP PROCEDURE IF EXISTS sp_agent_create;
DROP PROCEDURE IF EXISTS sp_agent_get_by_id;
DROP PROCEDURE IF EXISTS sp_agent_update;
DROP PROCEDURE IF EXISTS sp_agent_delete;
DROP PROCEDURE IF EXISTS sp_underwriter_create;
DROP PROCEDURE IF EXISTS sp_underwriter_get_by_id;
DROP PROCEDURE IF EXISTS sp_underwriter_update;
DROP PROCEDURE IF EXISTS sp_underwriter_delete;
DROP PROCEDURE IF EXISTS sp_underwriter_is_active;

DELIMITER $$

-- ----------------------------------------------------------------------------
-- customer CRUD
-- ----------------------------------------------------------------------------

CREATE PROCEDURE sp_customer_create(
    IN p_first_name     VARCHAR(100),
    IN p_last_name      VARCHAR(100),
    IN p_date_of_birth  DATE,
    IN p_email          VARCHAR(255),
    IN p_phone          VARCHAR(20),
    IN p_segment_code   VARCHAR(20),
    OUT p_customer_id   BIGINT
)
BEGIN
    IF p_first_name IS NULL OR p_last_name IS NULL OR p_date_of_birth IS NULL OR p_segment_code IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_customer_create: first_name, last_name, date_of_birth, and segment_code are required';
    END IF;

    INSERT INTO customer (first_name, last_name, date_of_birth, email, phone, segment_code)
    VALUES (p_first_name, p_last_name, p_date_of_birth, p_email, p_phone, p_segment_code);

    SET p_customer_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_customer_get_by_id(
    IN p_customer_id BIGINT
)
BEGIN
    IF p_customer_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_customer_get_by_id: customer_id is required';
    END IF;

    SELECT customer_id, first_name, last_name, date_of_birth, email, phone, segment_code, created_at
    FROM customer
    WHERE customer_id = p_customer_id;
END$$

CREATE PROCEDURE sp_customer_update(
    IN p_customer_id    BIGINT,
    IN p_first_name     VARCHAR(100),
    IN p_last_name      VARCHAR(100),
    IN p_date_of_birth  DATE,
    IN p_email          VARCHAR(255),
    IN p_phone          VARCHAR(20),
    IN p_segment_code   VARCHAR(20)
)
BEGIN
    IF p_customer_id IS NULL OR p_first_name IS NULL OR p_last_name IS NULL OR p_date_of_birth IS NULL OR p_segment_code IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_customer_update: customer_id, first_name, last_name, date_of_birth, and segment_code are required';
    END IF;

    UPDATE customer
    SET first_name = p_first_name,
        last_name = p_last_name,
        date_of_birth = p_date_of_birth,
        email = p_email,
        phone = p_phone,
        segment_code = p_segment_code
    WHERE customer_id = p_customer_id;
END$$

CREATE PROCEDURE sp_customer_delete(
    IN p_customer_id BIGINT
)
BEGIN
    IF p_customer_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_customer_delete: customer_id is required';
    END IF;

    DELETE FROM customer WHERE customer_id = p_customer_id;
END$$

-- ----------------------------------------------------------------------------
-- agent CRUD
-- ----------------------------------------------------------------------------

CREATE PROCEDURE sp_agent_create(
    IN p_name            VARCHAR(100),
    IN p_license_number  VARCHAR(20),
    IN p_email            VARCHAR(255),
    IN p_phone            VARCHAR(20),
    OUT p_agent_id        BIGINT
)
BEGIN
    IF p_name IS NULL OR p_license_number IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_agent_create: name and license_number are required';
    END IF;

    INSERT INTO agent (name, license_number, email, phone)
    VALUES (p_name, p_license_number, p_email, p_phone);

    SET p_agent_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_agent_get_by_id(
    IN p_agent_id BIGINT
)
BEGIN
    IF p_agent_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_agent_get_by_id: agent_id is required';
    END IF;

    SELECT agent_id, name, license_number, email, phone
    FROM agent
    WHERE agent_id = p_agent_id;
END$$

CREATE PROCEDURE sp_agent_update(
    IN p_agent_id        BIGINT,
    IN p_name             VARCHAR(100),
    IN p_license_number   VARCHAR(20),
    IN p_email             VARCHAR(255),
    IN p_phone             VARCHAR(20)
)
BEGIN
    IF p_agent_id IS NULL OR p_name IS NULL OR p_license_number IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_agent_update: agent_id, name, and license_number are required';
    END IF;

    UPDATE agent
    SET name = p_name,
        license_number = p_license_number,
        email = p_email,
        phone = p_phone
    WHERE agent_id = p_agent_id;
END$$

CREATE PROCEDURE sp_agent_delete(
    IN p_agent_id BIGINT
)
BEGIN
    IF p_agent_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_agent_delete: agent_id is required';
    END IF;

    DELETE FROM agent WHERE agent_id = p_agent_id;
END$$

-- ----------------------------------------------------------------------------
-- underwriter CRUD
-- ----------------------------------------------------------------------------

CREATE PROCEDURE sp_underwriter_create(
    IN p_name            VARCHAR(100),
    IN p_license_id       VARCHAR(50),
    IN p_active_status     VARCHAR(10),
    OUT p_underwriter_id   BIGINT
)
BEGIN
    IF p_name IS NULL OR p_license_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_underwriter_create: name and license_id are required';
    END IF;

    INSERT INTO underwriter (name, license_id, active_status)
    VALUES (p_name, p_license_id, COALESCE(p_active_status, 'active'));

    SET p_underwriter_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_underwriter_get_by_id(
    IN p_underwriter_id BIGINT
)
BEGIN
    IF p_underwriter_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_underwriter_get_by_id: underwriter_id is required';
    END IF;

    SELECT underwriter_id, name, license_id, active_status
    FROM underwriter
    WHERE underwriter_id = p_underwriter_id;
END$$

CREATE PROCEDURE sp_underwriter_update(
    IN p_underwriter_id   BIGINT,
    IN p_name              VARCHAR(100),
    IN p_license_id         VARCHAR(50),
    IN p_active_status       VARCHAR(10)
)
BEGIN
    IF p_underwriter_id IS NULL OR p_name IS NULL OR p_license_id IS NULL OR p_active_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_underwriter_update: underwriter_id, name, license_id, and active_status are required';
    END IF;

    UPDATE underwriter
    SET name = p_name,
        license_id = p_license_id,
        active_status = p_active_status
    WHERE underwriter_id = p_underwriter_id;
END$$

CREATE PROCEDURE sp_underwriter_delete(
    IN p_underwriter_id BIGINT
)
BEGIN
    IF p_underwriter_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_underwriter_delete: underwriter_id is required';
    END IF;

    DELETE FROM underwriter WHERE underwriter_id = p_underwriter_id;
END$$

-- ----------------------------------------------------------------------------
-- underwriter helper: active-status check
-- ----------------------------------------------------------------------------

CREATE PROCEDURE sp_underwriter_is_active(
    IN p_underwriter_id BIGINT,
    OUT p_is_active     TINYINT
)
BEGIN
    IF p_underwriter_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'sp_underwriter_is_active: underwriter_id is required';
    END IF;

    SET p_is_active = (
        SELECT CASE WHEN active_status = 'active' THEN 1 ELSE 0 END
        FROM underwriter
        WHERE underwriter_id = p_underwriter_id
    );

    IF p_is_active IS NULL THEN
        SET p_is_active = 0;
    END IF;
END$$

DELIMITER ;
