-- =====================================================================================
-- Insurance Policy Administration System - Stored Procedures: Endorsement / Premium
-- MySQL 8.0
--
-- Contents:
--   CRUD (4 procs): endorsement       -> sp_endorsement_create, sp_endorsement_get_by_id,
--                                        sp_endorsement_list_by_policy_id, sp_endorsement_update
--   CRUD (4 procs): premium_schedule/installment ->
--                                        sp_premium_schedule_create, sp_premium_schedule_get_by_policy_id,
--                                        sp_installment_get_by_id, sp_installment_update
--   Business logic : sp_endorsement_calculate_proration (DECIMAL proration, OUT status code)
--                     sp_premium_schedule_generate_installments (floor-rounded installments,
--                       remainder absorbed into the final installment)
--                     sp_installment_regenerate_remaining (endorsement-triggered regeneration,
--                       preserves paid/past-due installments)
--
-- Per Requirement 18.19, the CRUD procedures below contain only parameter validation
-- plus the single corresponding data operation -- no branching beyond that.
-- =====================================================================================
DELIMITER //

-- =====================================================================================
-- sp_endorsement_create
-- Creates a new Endorsement row for a Policy.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_endorsement_create //
CREATE PROCEDURE sp_endorsement_create (
    IN  p_policy_id          BIGINT,
    IN  p_description        VARCHAR(1000),
    IN  p_effective_date     DATE,
    IN  p_premium_adjustment DECIMAL(12,2),
    OUT p_endorsement_id     BIGINT
)
BEGIN
    INSERT INTO endorsement (policy_id, description, effective_date, premium_adjustment)
    VALUES (p_policy_id, p_description, p_effective_date, p_premium_adjustment);
    SET p_endorsement_id = LAST_INSERT_ID();
END //

-- =====================================================================================
-- sp_endorsement_get_by_id
-- Reads a single Endorsement row by endorsement_id.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_endorsement_get_by_id //
CREATE PROCEDURE sp_endorsement_get_by_id (
    IN p_endorsement_id BIGINT
)
BEGIN
    SELECT endorsement_id, policy_id, description, effective_date, premium_adjustment, created_at
    FROM endorsement
    WHERE endorsement_id = p_endorsement_id;
END //

-- =====================================================================================
-- sp_endorsement_list_by_policy_id
-- Lists all Endorsement rows for a given Policy.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_endorsement_list_by_policy_id //
CREATE PROCEDURE sp_endorsement_list_by_policy_id (
    IN p_policy_id BIGINT
)
BEGIN
    SELECT endorsement_id, policy_id, description, effective_date, premium_adjustment, created_at
    FROM endorsement
    WHERE policy_id = p_policy_id
    ORDER BY effective_date ASC, endorsement_id ASC;
END //

-- =====================================================================================
-- sp_endorsement_update
-- Updates the mutable fields of an Endorsement row.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_endorsement_update //
CREATE PROCEDURE sp_endorsement_update (
    IN p_endorsement_id      BIGINT,
    IN p_description         VARCHAR(1000),
    IN p_effective_date      DATE,
    IN p_premium_adjustment  DECIMAL(12,2)
)
BEGIN
    UPDATE endorsement
    SET description        = p_description,
        effective_date     = p_effective_date,
        premium_adjustment = p_premium_adjustment
    WHERE endorsement_id = p_endorsement_id;
END //

-- =====================================================================================
-- sp_premium_schedule_create
-- Creates a new Premium_Schedule row for a Policy.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_premium_schedule_create //
CREATE PROCEDURE sp_premium_schedule_create (
    IN  p_policy_id            BIGINT,
    IN  p_billing_frequency    VARCHAR(20),
    IN  p_total_premium        DECIMAL(12,2),
    OUT p_premium_schedule_id  BIGINT
)
BEGIN
    INSERT INTO premium_schedule (policy_id, billing_frequency, total_premium)
    VALUES (p_policy_id, p_billing_frequency, p_total_premium);
    SET p_premium_schedule_id = LAST_INSERT_ID();
END //

-- =====================================================================================
-- sp_premium_schedule_get_by_policy_id
-- Reads the single Premium_Schedule row for a given Policy.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_premium_schedule_get_by_policy_id //
CREATE PROCEDURE sp_premium_schedule_get_by_policy_id (
    IN p_policy_id BIGINT
)
BEGIN
    SELECT premium_schedule_id, policy_id, billing_frequency, total_premium, created_at
    FROM premium_schedule
    WHERE policy_id = p_policy_id;
END //

-- =====================================================================================
-- sp_installment_get_by_id
-- Reads a single Installment row by installment_id.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_installment_get_by_id //
CREATE PROCEDURE sp_installment_get_by_id (
    IN p_installment_id BIGINT
)
BEGIN
    SELECT installment_id, premium_schedule_id, due_date, amount_due, amount_paid, installment_status
    FROM installment
    WHERE installment_id = p_installment_id;
END //

-- =====================================================================================
-- sp_installment_update
-- Updates the mutable fields of an Installment row (amount paid / status).
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_installment_update //
CREATE PROCEDURE sp_installment_update (
    IN p_installment_id     BIGINT,
    IN p_amount_paid        DECIMAL(12,2),
    IN p_installment_status VARCHAR(20)
)
BEGIN
    UPDATE installment
    SET amount_paid        = p_amount_paid,
        installment_status = p_installment_status
    WHERE installment_id = p_installment_id;
END //

-- =====================================================================================
-- sp_endorsement_calculate_proration
-- Computes the prorated premium adjustment for an endorsement against an active Policy:
--   daysRemaining  = DATEDIFF(policy.term_end_date, p_effective_date)
--   totalTermDays  = DATEDIFF(policy.term_end_date, policy.term_start_date)
--   proratedAdjustment = ROUND(p_coverage_delta * daysRemaining / totalTermDays, 2)
-- using DECIMAL arithmetic throughout to avoid floating-point drift.
--
-- p_status_code OUT values:
--   0 = success
--   1 = policy not endorsable (policy_status <> 'active') or not found
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_endorsement_calculate_proration //
CREATE PROCEDURE sp_endorsement_calculate_proration (
    IN  p_policy_id            BIGINT,
    IN  p_coverage_delta       DECIMAL(12,2),
    IN  p_effective_date       DATE,
    OUT p_prorated_adjustment  DECIMAL(12,2),
    OUT p_status_code          INT
)
BEGIN
    DECLARE v_policy_status   VARCHAR(20);
    DECLARE v_term_start_date DATE;
    DECLARE v_term_end_date   DATE;
    DECLARE v_days_remaining  INT;
    DECLARE v_total_term_days INT;

    SELECT policy_status, term_start_date, term_end_date
    INTO v_policy_status, v_term_start_date, v_term_end_date
    FROM policy
    WHERE policy_id = p_policy_id;

    IF v_policy_status IS NULL OR v_policy_status <> 'active' THEN
        SET p_status_code = 1;
        SET p_prorated_adjustment = NULL;
    ELSE
        SET v_days_remaining  = DATEDIFF(v_term_end_date, p_effective_date);
        SET v_total_term_days = DATEDIFF(v_term_end_date, v_term_start_date);
        SET p_prorated_adjustment = ROUND(p_coverage_delta * v_days_remaining / v_total_term_days, 2);
        SET p_status_code = 0;
    END IF;
END //

-- =====================================================================================
-- sp_premium_schedule_generate_installments
-- Generates a full set of Installment rows for a newly created Premium_Schedule, using
-- the floor-rounded per-installment algorithm from design.md:
--   installmentCount : monthly -> 12, quarterly -> 4, semi_annual -> 2, annual -> 1
--   monthOffset      : monthly -> 1, quarterly -> 3, semi_annual -> 6, annual -> 12
--   baseAmount        = FLOOR(p_total_premium * 100 / installmentCount) / 100
--   installments 1..(count-1) = baseAmount, due dates spaced monthOffset months apart
--     starting at p_term_start_date
--   installment count     = p_total_premium - (baseAmount * (installmentCount - 1))
--     (absorbs the rounding remainder), due at the final spaced due date
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_premium_schedule_generate_installments //
CREATE PROCEDURE sp_premium_schedule_generate_installments (
    IN p_policy_id          BIGINT,
    IN p_total_premium      DECIMAL(12,2),
    IN p_billing_frequency  VARCHAR(20),
    IN p_term_start_date    DATE
)
BEGIN
    DECLARE v_premium_schedule_id BIGINT;
    DECLARE v_installment_count   INT;
    DECLARE v_month_offset        INT;
    DECLARE v_base_amount          DECIMAL(12,2);
    DECLARE v_last_amount           DECIMAL(12,2);
    DECLARE v_index                  INT DEFAULT 1;
    DECLARE v_due_date                 DATE;

    SELECT premium_schedule_id INTO v_premium_schedule_id
    FROM premium_schedule
    WHERE policy_id = p_policy_id;

    CASE p_billing_frequency
        WHEN 'monthly'     THEN SET v_installment_count = 12, v_month_offset = 1;
        WHEN 'quarterly'   THEN SET v_installment_count = 4,  v_month_offset = 3;
        WHEN 'semi_annual' THEN SET v_installment_count = 2,  v_month_offset = 6;
        WHEN 'annual'      THEN SET v_installment_count = 1,  v_month_offset = 12;
        ELSE SET v_installment_count = 12, v_month_offset = 1;
    END CASE;

    SET v_base_amount = FLOOR(p_total_premium * 100 / v_installment_count) / 100;
    SET v_last_amount = p_total_premium - (v_base_amount * (v_installment_count - 1));

    WHILE v_index <= v_installment_count DO
        SET v_due_date = DATE_ADD(p_term_start_date, INTERVAL (v_month_offset * (v_index - 1)) MONTH);
        IF v_index < v_installment_count THEN
            INSERT INTO installment (premium_schedule_id, due_date, amount_due, amount_paid, installment_status)
            VALUES (v_premium_schedule_id, v_due_date, v_base_amount, 0.00, 'unpaid');
        ELSE
            INSERT INTO installment (premium_schedule_id, due_date, amount_due, amount_paid, installment_status)
            VALUES (v_premium_schedule_id, v_due_date, v_last_amount, 0.00, 'unpaid');
        END IF;
        SET v_index = v_index + 1;
    END WHILE;
END //

-- =====================================================================================
-- sp_installment_regenerate_remaining
-- Endorsement-triggered installment regeneration: leaves installments that are already
-- paid, or whose due_date is strictly before p_effective_date, unchanged. Deletes and
-- regenerates the remaining not-yet-due, unpaid installments so their amounts reflect
-- the new adjusted total premium, using the same floor + remainder algorithm, spread
-- evenly across the remaining installment slots and due dates.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_installment_regenerate_remaining //
CREATE PROCEDURE sp_installment_regenerate_remaining (
    IN p_policy_id           BIGINT,
    IN p_effective_date      DATE,
    IN p_new_remaining_total DECIMAL(12,2)
)
BEGIN
    DECLARE v_premium_schedule_id BIGINT;
    DECLARE v_remaining_count     INT;
    DECLARE v_base_amount          DECIMAL(12,2);
    DECLARE v_last_amount           DECIMAL(12,2);
    DECLARE v_index                  INT DEFAULT 1;
    DECLARE v_done                     INT DEFAULT 0;
    DECLARE v_due_date                   DATE;
    DECLARE due_date_cursor CURSOR FOR
        SELECT due_date
        FROM installment
        WHERE premium_schedule_id = v_premium_schedule_id
          AND installment_status = 'unpaid'
          AND due_date >= p_effective_date
        ORDER BY due_date ASC;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SELECT premium_schedule_id INTO v_premium_schedule_id
    FROM premium_schedule
    WHERE policy_id = p_policy_id;

    SELECT COUNT(*) INTO v_remaining_count
    FROM installment
    WHERE premium_schedule_id = v_premium_schedule_id
      AND installment_status = 'unpaid'
      AND due_date >= p_effective_date;

    IF v_remaining_count > 0 THEN
        -- Capture the existing due dates for the remaining (not-yet-due, unpaid)
        -- installments before deleting them, so the regenerated rows keep the same
        -- due-date cadence.
        DROP TEMPORARY TABLE IF EXISTS tmp_remaining_due_dates;
        CREATE TEMPORARY TABLE tmp_remaining_due_dates (
            seq_num  INT AUTO_INCREMENT PRIMARY KEY,
            due_date DATE NOT NULL
        );

        OPEN due_date_cursor;
        read_loop: LOOP
            FETCH due_date_cursor INTO v_due_date;
            IF v_done = 1 THEN
                LEAVE read_loop;
            END IF;
            INSERT INTO tmp_remaining_due_dates (due_date) VALUES (v_due_date);
        END LOOP;
        CLOSE due_date_cursor;

        DELETE FROM installment
        WHERE premium_schedule_id = v_premium_schedule_id
          AND installment_status = 'unpaid'
          AND due_date >= p_effective_date;

        SET v_base_amount = FLOOR(p_new_remaining_total * 100 / v_remaining_count) / 100;
        SET v_last_amount = p_new_remaining_total - (v_base_amount * (v_remaining_count - 1));

        SET v_index = 1;
        WHILE v_index <= v_remaining_count DO
            SELECT due_date INTO v_due_date
            FROM tmp_remaining_due_dates
            WHERE seq_num = v_index;

            IF v_index < v_remaining_count THEN
                INSERT INTO installment (premium_schedule_id, due_date, amount_due, amount_paid, installment_status)
                VALUES (v_premium_schedule_id, v_due_date, v_base_amount, 0.00, 'unpaid');
            ELSE
                INSERT INTO installment (premium_schedule_id, due_date, amount_due, amount_paid, installment_status)
                VALUES (v_premium_schedule_id, v_due_date, v_last_amount, 0.00, 'unpaid');
            END IF;
            SET v_index = v_index + 1;
        END WHILE;

        DROP TEMPORARY TABLE IF EXISTS tmp_remaining_due_dates;
    END IF;
END //

DELIMITER ;
