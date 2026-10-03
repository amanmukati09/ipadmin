-- =====================================================================================
-- Insurance Policy Administration System - Stored Procedures: Payment
-- MySQL 8.0
--
-- Contents:
--   CRUD (4 procs): payment / payment_allocation ->
--                     sp_payment_create, sp_payment_get_by_id,
--                     sp_payment_list_by_policy_id, sp_payment_allocation_get_by_payment_id
--   Business logic : sp_payment_apply (transactional, oldest-unpaid-first allocation,
--                                       OUT status code)
--
-- Per Requirement 18.19, the CRUD procedures below contain only parameter validation
-- plus the single corresponding data operation -- no branching beyond that.
-- =====================================================================================

DELIMITER //

-- =====================================================================================
-- sp_payment_create
-- Creates a new Payment row.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_payment_create //
CREATE PROCEDURE sp_payment_create (
    IN  p_policy_id      BIGINT,
    IN  p_payment_amount DECIMAL(12,2),
    IN  p_payment_date   DATE,
    OUT p_payment_id     BIGINT
)
BEGIN
    INSERT INTO payment (policy_id, payment_amount, payment_date)
    VALUES (p_policy_id, p_payment_amount, p_payment_date);

    SET p_payment_id = LAST_INSERT_ID();
END //

-- =====================================================================================
-- sp_payment_get_by_id
-- Reads a single Payment row by payment_id.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_payment_get_by_id //
CREATE PROCEDURE sp_payment_get_by_id (
    IN p_payment_id BIGINT
)
BEGIN
    SELECT payment_id, policy_id, payment_amount, payment_date, created_at
    FROM payment
    WHERE payment_id = p_payment_id;
END //

-- =====================================================================================
-- sp_payment_list_by_policy_id
-- Lists all Payment rows for a given Policy.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_payment_list_by_policy_id //
CREATE PROCEDURE sp_payment_list_by_policy_id (
    IN p_policy_id BIGINT
)
BEGIN
    SELECT payment_id, policy_id, payment_amount, payment_date, created_at
    FROM payment
    WHERE policy_id = p_policy_id
    ORDER BY payment_id ASC;
END //

-- =====================================================================================
-- sp_payment_allocation_get_by_payment_id
-- Lists all Payment_Allocation rows for a given Payment.
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_payment_allocation_get_by_payment_id //
CREATE PROCEDURE sp_payment_allocation_get_by_payment_id (
    IN p_payment_id BIGINT
)
BEGIN
    SELECT payment_allocation_id, payment_id, installment_id, amount_applied
    FROM payment_allocation
    WHERE payment_id = p_payment_id
    ORDER BY payment_allocation_id ASC;
END //

-- =====================================================================================
-- sp_payment_apply
-- Applies a payment to a Policy's outstanding Installments, oldest-unpaid-first
-- (by due_date ascending). Rejects the payment (no state change) when the amount
-- exceeds the Policy's total remaining unpaid balance across its Premium_Schedule,
-- including the case where the remaining balance is already zero.
--
-- p_status_code OUT values:
--   0 = success
--   1 = overpayment (amount exceeds remaining unpaid balance, including a zero balance)
-- =====================================================================================
DROP PROCEDURE IF EXISTS sp_payment_apply //
CREATE PROCEDURE sp_payment_apply (
    IN  p_policy_id      BIGINT,
    IN  p_amount         DECIMAL(12,2),
    IN  p_payment_date   DATE,
    OUT p_payment_id     BIGINT,
    OUT p_status_code    INT
)
BEGIN
    DECLARE v_remaining_balance   DECIMAL(12,2);
    DECLARE v_remaining_payment   DECIMAL(12,2);
    DECLARE v_installment_id      BIGINT;
    DECLARE v_installment_due     DECIMAL(12,2);
    DECLARE v_apply_amount        DECIMAL(12,2);
    DECLARE v_done                INT DEFAULT 0;

    DECLARE installment_cursor CURSOR FOR
        SELECT i.installment_id, (i.amount_due - i.amount_paid) AS remaining_due
        FROM installment i
        INNER JOIN premium_schedule ps ON ps.premium_schedule_id = i.premium_schedule_id
        WHERE ps.policy_id = p_policy_id
          AND i.installment_status != 'paid'
        ORDER BY i.due_date ASC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COALESCE(SUM(i.amount_due - i.amount_paid), 0.00)
    INTO v_remaining_balance
    FROM installment i
    INNER JOIN premium_schedule ps ON ps.premium_schedule_id = i.premium_schedule_id
    WHERE ps.policy_id = p_policy_id
      AND i.installment_status != 'paid';

    IF v_remaining_balance <= 0.00 OR p_amount > v_remaining_balance THEN
        SET p_status_code = 1;
        SET p_payment_id = NULL;
    ELSE
        START TRANSACTION;

        INSERT INTO payment (policy_id, payment_amount, payment_date)
        VALUES (p_policy_id, p_amount, p_payment_date);

        SET p_payment_id = LAST_INSERT_ID();
        SET v_remaining_payment = p_amount;
        SET v_done = 0;

        OPEN installment_cursor;

        allocation_loop: LOOP
            FETCH installment_cursor INTO v_installment_id, v_installment_due;

            IF v_done = 1 OR v_remaining_payment <= 0.00 THEN
                LEAVE allocation_loop;
            END IF;

            IF v_installment_due <= v_remaining_payment THEN
                SET v_apply_amount = v_installment_due;
            ELSE
                SET v_apply_amount = v_remaining_payment;
            END IF;

            INSERT INTO payment_allocation (payment_id, installment_id, amount_applied)
            VALUES (p_payment_id, v_installment_id, v_apply_amount);

            UPDATE installment
            SET installment_status = CASE
                    WHEN amount_paid + v_apply_amount >= amount_due THEN 'paid'
                    ELSE installment_status
                END,
                amount_paid = amount_paid + v_apply_amount
            WHERE installment_id = v_installment_id;

            SET v_remaining_payment = v_remaining_payment - v_apply_amount;
        END LOOP;

        CLOSE installment_cursor;

        SET p_status_code = 0;
        COMMIT;
    END IF;
END //

DELIMITER ;
