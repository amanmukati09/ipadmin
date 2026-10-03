-- ============================================================================
-- Insurance Policy Administration System - Status History / Renewal Notice
-- Stored Procedures
-- Requirements: 18.2, 18.6, 18.7, 18.17, 18.19, 18.20
--
-- CRUD-equivalent procedures for the append-only/audit tables
-- `policy_status_history` and `renewal_notice` (these tables have no
-- business-level "update" operation since history rows are immutable once
-- written; "delete" is retained only as an administrative/corrective
-- operation per the spirit of CRUD for an audit table), plus three
-- explicit cursor-based procedures and the transactional renewal-notice
-- generation procedure with duplicate-cycle detection.
-- ============================================================================

USE insurance_policy_admin;

DELIMITER //

-- ----------------------------------------------------------------------------
-- sp_policy_status_history_create
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_policy_status_history_create //

CREATE PROCEDURE sp_policy_status_history_create(
    IN p_policy_id BIGINT,
    IN p_previous_status VARCHAR(20),
    IN p_new_status VARCHAR(20),
    OUT p_policy_status_history_id BIGINT
)
BEGIN
    IF p_policy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_id must not be NULL';
    END IF;

    IF p_new_status IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_new_status must not be NULL';
    END IF;

    INSERT INTO policy_status_history (policy_id, previous_status, new_status)
    VALUES (p_policy_id, p_previous_status, p_new_status);

    SET p_policy_status_history_id = LAST_INSERT_ID();
END //

-- ----------------------------------------------------------------------------
-- sp_policy_status_history_get_by_id
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_policy_status_history_get_by_id //

CREATE PROCEDURE sp_policy_status_history_get_by_id(
    IN p_policy_status_history_id BIGINT
)
BEGIN
    IF p_policy_status_history_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_status_history_id must not be NULL';
    END IF;

    SELECT *
    FROM policy_status_history
    WHERE policy_status_history_id = p_policy_status_history_id;
END //

-- ----------------------------------------------------------------------------
-- sp_policy_status_history_list_by_policy
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_policy_status_history_list_by_policy //

CREATE PROCEDURE sp_policy_status_history_list_by_policy(
    IN p_policy_id BIGINT
)
BEGIN
    IF p_policy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_id must not be NULL';
    END IF;

    SELECT *
    FROM policy_status_history
    WHERE policy_id = p_policy_id
    ORDER BY transitioned_at ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_policy_status_history_delete
--
-- Administrative/corrective operation only; policy_status_history is an
-- append-only audit trail and this procedure is not expected to be called
-- as part of normal business flows.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_policy_status_history_delete //

CREATE PROCEDURE sp_policy_status_history_delete(
    IN p_policy_status_history_id BIGINT,
    OUT p_rows_affected INT
)
BEGIN
    IF p_policy_status_history_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_status_history_id must not be NULL';
    END IF;

    DELETE FROM policy_status_history
    WHERE policy_status_history_id = p_policy_status_history_id;

    SET p_rows_affected = ROW_COUNT();
END //

-- ----------------------------------------------------------------------------
-- sp_renewal_notice_create
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_renewal_notice_create //

CREATE PROCEDURE sp_renewal_notice_create(
    IN p_policy_id BIGINT,
    IN p_term_end_date_snapshot DATE,
    OUT p_renewal_notice_id BIGINT
)
BEGIN
    IF p_policy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_id must not be NULL';
    END IF;

    IF p_term_end_date_snapshot IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_term_end_date_snapshot must not be NULL';
    END IF;

    INSERT INTO renewal_notice (policy_id, term_end_date_snapshot)
    VALUES (p_policy_id, p_term_end_date_snapshot);

    SET p_renewal_notice_id = LAST_INSERT_ID();
END //

-- ----------------------------------------------------------------------------
-- sp_renewal_notice_get_by_id
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_renewal_notice_get_by_id //

CREATE PROCEDURE sp_renewal_notice_get_by_id(
    IN p_renewal_notice_id BIGINT
)
BEGIN
    IF p_renewal_notice_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_renewal_notice_id must not be NULL';
    END IF;

    SELECT *
    FROM renewal_notice
    WHERE renewal_notice_id = p_renewal_notice_id;
END //

-- ----------------------------------------------------------------------------
-- sp_renewal_notice_list_by_policy
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_renewal_notice_list_by_policy //

CREATE PROCEDURE sp_renewal_notice_list_by_policy(
    IN p_policy_id BIGINT
)
BEGIN
    IF p_policy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_id must not be NULL';
    END IF;

    SELECT *
    FROM renewal_notice
    WHERE policy_id = p_policy_id
    ORDER BY generated_at DESC;
END //

-- ----------------------------------------------------------------------------
-- sp_renewal_notice_delete
--
-- Administrative/corrective operation only; renewal_notice is an append-only
-- audit trail and this procedure is not expected to be called as part of
-- normal business flows.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_renewal_notice_delete //

CREATE PROCEDURE sp_renewal_notice_delete(
    IN p_renewal_notice_id BIGINT,
    OUT p_rows_affected INT
)
BEGIN
    IF p_renewal_notice_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_renewal_notice_id must not be NULL';
    END IF;

    DELETE FROM renewal_notice
    WHERE renewal_notice_id = p_renewal_notice_id;

    SET p_rows_affected = ROW_COUNT();
END //

-- ----------------------------------------------------------------------------
-- sp_policy_status_history_cursor
--
-- Cursor-based procedure (explicit DECLARE CURSOR / OPEN / FETCH loop /
-- CLOSE) returning all policy_status_history rows for a policy, ordered by
-- transitioned_at ascending, via a temporary table populated row-by-row.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_policy_status_history_cursor //

CREATE PROCEDURE sp_policy_status_history_cursor(
    IN p_policy_id BIGINT
)
BEGIN
    DECLARE v_done INT DEFAULT FALSE;
    DECLARE v_policy_status_history_id BIGINT;
    DECLARE v_policy_id BIGINT;
    DECLARE v_previous_status VARCHAR(20);
    DECLARE v_new_status VARCHAR(20);
    DECLARE v_transitioned_at TIMESTAMP;

    DECLARE cur_status_history CURSOR FOR
        SELECT policy_status_history_id, policy_id, previous_status, new_status, transitioned_at
        FROM policy_status_history
        WHERE policy_id = p_policy_id
        ORDER BY transitioned_at ASC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;

    IF p_policy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_id must not be NULL';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_policy_status_history_cursor;
    CREATE TEMPORARY TABLE tmp_policy_status_history_cursor (
        policy_status_history_id BIGINT,
        policy_id BIGINT,
        previous_status VARCHAR(20),
        new_status VARCHAR(20),
        transitioned_at TIMESTAMP
    );

    OPEN cur_status_history;

    read_loop: LOOP
        FETCH cur_status_history INTO v_policy_status_history_id, v_policy_id, v_previous_status, v_new_status, v_transitioned_at;
        IF v_done THEN
            LEAVE read_loop;
        END IF;

        INSERT INTO tmp_policy_status_history_cursor
            (policy_status_history_id, policy_id, previous_status, new_status, transitioned_at)
        VALUES
            (v_policy_status_history_id, v_policy_id, v_previous_status, v_new_status, v_transitioned_at);
    END LOOP;

    CLOSE cur_status_history;

    SELECT policy_status_history_id, policy_id, previous_status, new_status, transitioned_at
    FROM tmp_policy_status_history_cursor
    ORDER BY transitioned_at ASC;

    DROP TEMPORARY TABLE IF EXISTS tmp_policy_status_history_cursor;
END //

-- ----------------------------------------------------------------------------
-- sp_premium_schedule_installments_cursor
--
-- Cursor-based procedure returning all installment rows for a policy's
-- premium schedule, ordered by due_date ascending.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_premium_schedule_installments_cursor //

CREATE PROCEDURE sp_premium_schedule_installments_cursor(
    IN p_policy_id BIGINT
)
BEGIN
    DECLARE v_done INT DEFAULT FALSE;
    DECLARE v_installment_id BIGINT;
    DECLARE v_premium_schedule_id BIGINT;
    DECLARE v_due_date DATE;
    DECLARE v_amount_due DECIMAL(12,2);
    DECLARE v_amount_paid DECIMAL(12,2);
    DECLARE v_installment_status VARCHAR(10);

    DECLARE cur_installments CURSOR FOR
        SELECT i.installment_id, i.premium_schedule_id, i.due_date, i.amount_due, i.amount_paid, i.installment_status
        FROM installment i
        INNER JOIN premium_schedule ps ON ps.premium_schedule_id = i.premium_schedule_id
        WHERE ps.policy_id = p_policy_id
        ORDER BY i.due_date ASC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;

    IF p_policy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_id must not be NULL';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_installments_cursor;
    CREATE TEMPORARY TABLE tmp_installments_cursor (
        installment_id BIGINT,
        premium_schedule_id BIGINT,
        due_date DATE,
        amount_due DECIMAL(12,2),
        amount_paid DECIMAL(12,2),
        installment_status VARCHAR(10)
    );

    OPEN cur_installments;

    read_loop: LOOP
        FETCH cur_installments INTO v_installment_id, v_premium_schedule_id, v_due_date, v_amount_due, v_amount_paid, v_installment_status;
        IF v_done THEN
            LEAVE read_loop;
        END IF;

        INSERT INTO tmp_installments_cursor
            (installment_id, premium_schedule_id, due_date, amount_due, amount_paid, installment_status)
        VALUES
            (v_installment_id, v_premium_schedule_id, v_due_date, v_amount_due, v_amount_paid, v_installment_status);
    END LOOP;

    CLOSE cur_installments;

    SELECT installment_id, premium_schedule_id, due_date, amount_due, amount_paid, installment_status
    FROM tmp_installments_cursor
    ORDER BY due_date ASC;

    DROP TEMPORARY TABLE IF EXISTS tmp_installments_cursor;
END //

-- ----------------------------------------------------------------------------
-- sp_payment_history_cursor
--
-- Cursor-based procedure returning all payment rows for a policy, ordered
-- by payment_date descending.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_payment_history_cursor //

CREATE PROCEDURE sp_payment_history_cursor(
    IN p_policy_id BIGINT
)
BEGIN
    DECLARE v_done INT DEFAULT FALSE;
    DECLARE v_payment_id BIGINT;
    DECLARE v_policy_id BIGINT;
    DECLARE v_payment_amount DECIMAL(12,2);
    DECLARE v_payment_date DATE;
    DECLARE v_created_at TIMESTAMP;

    DECLARE cur_payments CURSOR FOR
        SELECT payment_id, policy_id, payment_amount, payment_date, created_at
        FROM payment
        WHERE policy_id = p_policy_id
        ORDER BY payment_date DESC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;

    IF p_policy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_id must not be NULL';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_payment_history_cursor;
    CREATE TEMPORARY TABLE tmp_payment_history_cursor (
        payment_id BIGINT,
        policy_id BIGINT,
        payment_amount DECIMAL(12,2),
        payment_date DATE,
        created_at TIMESTAMP
    );

    OPEN cur_payments;

    read_loop: LOOP
        FETCH cur_payments INTO v_payment_id, v_policy_id, v_payment_amount, v_payment_date, v_created_at;
        IF v_done THEN
            LEAVE read_loop;
        END IF;

        INSERT INTO tmp_payment_history_cursor
            (payment_id, policy_id, payment_amount, payment_date, created_at)
        VALUES
            (v_payment_id, v_policy_id, v_payment_amount, v_payment_date, v_created_at);
    END LOOP;

    CLOSE cur_payments;

    SELECT payment_id, policy_id, payment_amount, payment_date, created_at
    FROM tmp_payment_history_cursor
    ORDER BY payment_date DESC;

    DROP TEMPORARY TABLE IF EXISTS tmp_payment_history_cursor;
END //

-- ----------------------------------------------------------------------------
-- sp_renewal_notice_generate
--
-- Transactional renewal-notice generation with lookahead-window eligibility
-- check and duplicate-cycle detection.
--
-- p_status_code:
--   0 = success, renewal notice created
--   1 = policy not found, or policy's term_end_date is not within
--       p_lookahead_days of CURDATE() (not yet eligible / already expired)
--   2 = duplicate - a renewal_notice already exists for this policy with
--       the same term_end_date_snapshot (current term cycle); no new row
--       inserted
--   3 = unexpected error during insert; transaction rolled back
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_renewal_notice_generate //

CREATE PROCEDURE sp_renewal_notice_generate(
    IN p_policy_id BIGINT,
    IN p_lookahead_days INT,
    OUT p_renewal_notice_id BIGINT,
    OUT p_status_code INT
)
BEGIN
    DECLARE v_term_end_date DATE;
    DECLARE v_existing_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_renewal_notice_id = NULL;
        SET p_status_code = 3;
    END;

    IF p_policy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_policy_id must not be NULL';
    END IF;

    IF p_lookahead_days IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'p_lookahead_days must not be NULL';
    END IF;

    SET p_renewal_notice_id = NULL;
    SET p_status_code = NULL;

    SELECT term_end_date INTO v_term_end_date
    FROM policy
    WHERE policy_id = p_policy_id;

    IF v_term_end_date IS NULL THEN
        SET p_status_code = 1;
    ELSEIF v_term_end_date > DATE_ADD(CURDATE(), INTERVAL p_lookahead_days DAY)
        OR v_term_end_date < CURDATE() THEN
        SET p_status_code = 1;
    ELSE
        START TRANSACTION;

        SELECT COUNT(*) INTO v_existing_count
        FROM renewal_notice
        WHERE policy_id = p_policy_id
          AND term_end_date_snapshot = v_term_end_date;

        IF v_existing_count > 0 THEN
            SET p_status_code = 2;
            COMMIT;
        ELSE
            INSERT INTO renewal_notice (policy_id, term_end_date_snapshot)
            VALUES (p_policy_id, v_term_end_date);

            SET p_renewal_notice_id = LAST_INSERT_ID();
            SET p_status_code = 0;

            COMMIT;
        END IF;
    END IF;
END //

DELIMITER ;
