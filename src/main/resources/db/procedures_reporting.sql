-- ============================================================================
-- Insurance Policy Administration System - Reporting Stored Procedures
-- Requirements: 18.1, 18.9, 18.10, 18.11, 18.18, 18.19, 18.20, 18.21, 20.3
--
-- Contents (49 procedures total):
--   14 named reporting procedures (expiring policies, overdue installments,
--   agent book of business, premium collected by period, lapse rate by
--   agent, endorsement volume by month, average days to bind, beneficiary
--   coverage summary, customer policy count, coverage type distribution,
--   underwriter approval volume, renewal conversion rate, outstanding
--   balance by agent, policy count by status)
--
--   11 remaining coverage/coverage_type/beneficiary CRUD-style support
--   procedures not covered in Task 3.2
--
--   4 audit_log / endorsement_history support procedures
--
--   20 additional reporting/support procedures (per-status-and-agent
--   breakdowns, per-coverage-type premium totals, monthly new-policy
--   counts, payment counts by month, installment counts by status, etc.)
--
-- This file brings the six-file procedure total to exactly 100.
-- ============================================================================

USE insurance_policy_admin;

DELIMITER //

-- ============================================================================
-- SECTION 1: Named reporting procedures (14)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- sp_report_expiring_policies
--
-- Returns policies whose term_end_date falls within p_day_count days of
-- CURDATE(). For p_day_count <= 0, returns an empty result set (the WHERE
-- clause's leading predicate naturally yields zero rows; no error signaled).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_expiring_policies //

CREATE PROCEDURE sp_report_expiring_policies(
    IN p_day_count INT
)
BEGIN
    SELECT p.policy_id, p.policy_number, p.customer_id, p.agent_id, p.policy_status,
           p.term_end_date, p.total_premium
    FROM policy p
    WHERE p_day_count > 0
      AND p.term_end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL p_day_count DAY)
    ORDER BY p.term_end_date ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_overdue_installments
--
-- Returns unpaid installments whose due_date has passed, with policy and
-- customer context.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_overdue_installments //

CREATE PROCEDURE sp_report_overdue_installments()
BEGIN
    SELECT i.installment_id, i.due_date, i.amount_due, i.amount_paid,
           ps.policy_id, pol.policy_number, pol.customer_id,
           c.first_name, c.last_name
    FROM installment i
    INNER JOIN premium_schedule ps ON ps.premium_schedule_id = i.premium_schedule_id
    INNER JOIN policy pol ON pol.policy_id = ps.policy_id
    INNER JOIN customer c ON c.customer_id = pol.customer_id
    WHERE i.installment_status = 'unpaid'
      AND i.due_date < CURDATE()
    ORDER BY i.due_date ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_agent_book_of_business
--
-- Returns all policies for an agent (first result set) plus a summary
-- (count, total premium, status breakdown counts) as a second result set.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_agent_book_of_business //

CREATE PROCEDURE sp_report_agent_book_of_business(
    IN p_agent_id BIGINT
)
BEGIN
    SELECT policy_id, policy_number, customer_id, policy_status, term_start_date,
           term_end_date, billing_frequency, total_premium
    FROM policy
    WHERE agent_id = p_agent_id
    ORDER BY policy_id ASC;

    SELECT COUNT(*) AS policy_count,
           COALESCE(SUM(total_premium), 0.00) AS total_premium,
           SUM(policy_status = 'quoted')    AS quoted_count,
           SUM(policy_status = 'bound')     AS bound_count,
           SUM(policy_status = 'active')    AS active_count,
           SUM(policy_status = 'lapsed')    AS lapsed_count,
           SUM(policy_status = 'cancelled') AS cancelled_count,
           SUM(policy_status = 'renewed')   AS renewed_count
    FROM policy
    WHERE agent_id = p_agent_id;
END //

-- ----------------------------------------------------------------------------
-- sp_report_premium_collected_by_period
--
-- Sums payment_amount grouped by calendar month, for payments between
-- p_start_date and p_end_date (inclusive).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_premium_collected_by_period //

CREATE PROCEDURE sp_report_premium_collected_by_period(
    IN p_start_date DATE,
    IN p_end_date DATE
)
BEGIN
    SELECT DATE_FORMAT(payment_date, '%Y-%m') AS period,
           COUNT(*) AS payment_count,
           SUM(payment_amount) AS total_collected
    FROM payment
    WHERE payment_date BETWEEN p_start_date AND p_end_date
    GROUP BY DATE_FORMAT(payment_date, '%Y-%m')
    ORDER BY period ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_lapse_rate_by_agent
--
-- Per-agent lapsed-policy count, total policy count, and lapse rate
-- (lapsed / total).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_lapse_rate_by_agent //

CREATE PROCEDURE sp_report_lapse_rate_by_agent()
BEGIN
    SELECT a.agent_id, a.name,
           COUNT(p.policy_id) AS total_policy_count,
           SUM(p.policy_status = 'lapsed') AS lapsed_count,
           CASE WHEN COUNT(p.policy_id) = 0 THEN 0.0000
                ELSE ROUND(SUM(p.policy_status = 'lapsed') / COUNT(p.policy_id), 4)
           END AS lapse_rate
    FROM agent a
    LEFT JOIN policy p ON p.agent_id = a.agent_id
    GROUP BY a.agent_id, a.name
    ORDER BY a.agent_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_endorsement_volume_by_month
--
-- Counts endorsements grouped by month for a given year.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_endorsement_volume_by_month //

CREATE PROCEDURE sp_report_endorsement_volume_by_month(
    IN p_year INT
)
BEGIN
    SELECT MONTH(effective_date) AS month_number,
           COUNT(*) AS endorsement_count
    FROM endorsement
    WHERE YEAR(effective_date) = p_year
    GROUP BY MONTH(effective_date)
    ORDER BY month_number ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_average_days_to_bind
--
-- Averages the number of days between a policy's created_at and the
-- transitioned_at of its first 'bound' status-history row, across all
-- policies that have reached 'bound' or beyond.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_average_days_to_bind //

CREATE PROCEDURE sp_report_average_days_to_bind()
BEGIN
    SELECT AVG(DATEDIFF(first_bound.first_bound_at, p.created_at)) AS avg_days_to_bind
    FROM policy p
    INNER JOIN (
        SELECT psh.policy_id, MIN(psh.transitioned_at) AS first_bound_at
        FROM policy_status_history psh
        WHERE psh.new_status = 'bound'
        GROUP BY psh.policy_id
    ) first_bound ON first_bound.policy_id = p.policy_id;
END //

-- ----------------------------------------------------------------------------
-- sp_report_beneficiary_coverage_summary
--
-- Returns the beneficiary list with allocation percentages for a policy
-- (first result set) plus the SUM check of allocation_percentage (second
-- result set).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_beneficiary_coverage_summary //

CREATE PROCEDURE sp_report_beneficiary_coverage_summary(
    IN p_policy_id BIGINT
)
BEGIN
    SELECT beneficiary_id, policy_id, name, relationship_type, allocation_percentage
    FROM beneficiary
    WHERE policy_id = p_policy_id
    ORDER BY beneficiary_id ASC;

    SELECT COALESCE(SUM(allocation_percentage), 0.00) AS total_allocation_percentage,
           COUNT(*) AS beneficiary_count
    FROM beneficiary
    WHERE policy_id = p_policy_id;
END //

-- ----------------------------------------------------------------------------
-- sp_report_customer_policy_count
--
-- Counts policies grouped by customer_id.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_customer_policy_count //

CREATE PROCEDURE sp_report_customer_policy_count()
BEGIN
    SELECT c.customer_id, c.first_name, c.last_name, COUNT(p.policy_id) AS policy_count
    FROM customer c
    LEFT JOIN policy p ON p.customer_id = c.customer_id
    GROUP BY c.customer_id, c.first_name, c.last_name
    ORDER BY c.customer_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_coverage_type_distribution
--
-- Counts coverage rows grouped by coverage_type_id/type_name.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_coverage_type_distribution //

CREATE PROCEDURE sp_report_coverage_type_distribution()
BEGIN
    SELECT ct.coverage_type_id, ct.type_name, COUNT(cv.coverage_id) AS coverage_count
    FROM coverage_type ct
    LEFT JOIN coverage cv ON cv.coverage_type_id = ct.coverage_type_id
    GROUP BY ct.coverage_type_id, ct.type_name
    ORDER BY ct.coverage_type_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_underwriter_approval_volume
--
-- Counts policies grouped by underwriter_id for policies that have been
-- bound/approved (underwriter_id is non-NULL).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_underwriter_approval_volume //

CREATE PROCEDURE sp_report_underwriter_approval_volume()
BEGIN
    SELECT u.underwriter_id, u.name, COUNT(p.policy_id) AS approved_policy_count
    FROM underwriter u
    LEFT JOIN policy p ON p.underwriter_id = u.underwriter_id
    GROUP BY u.underwriter_id, u.name
    ORDER BY u.underwriter_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_renewal_conversion_rate
--
-- Ratio of policies with policy_status = 'renewed' against policies that
-- have reached the end of their term (term_end_date <= CURDATE() and
-- status is one of renewed/lapsed/cancelled -- i.e. the term has run out).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_renewal_conversion_rate //

CREATE PROCEDURE sp_report_renewal_conversion_rate()
BEGIN
    SELECT
        SUM(policy_status = 'renewed') AS renewed_count,
        COUNT(*) AS term_ended_count,
        CASE WHEN COUNT(*) = 0 THEN 0.0000
             ELSE ROUND(SUM(policy_status = 'renewed') / COUNT(*), 4)
        END AS renewal_conversion_rate
    FROM policy
    WHERE term_end_date <= CURDATE()
      AND policy_status IN ('renewed', 'lapsed', 'cancelled');
END //

-- ----------------------------------------------------------------------------
-- sp_report_outstanding_balance_by_agent
--
-- Sums outstanding (amount_due - amount_paid) across unpaid installments,
-- grouped by the policy's agent_id.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_outstanding_balance_by_agent //

CREATE PROCEDURE sp_report_outstanding_balance_by_agent()
BEGIN
    SELECT a.agent_id, a.name,
           COALESCE(SUM(i.amount_due - i.amount_paid), 0.00) AS outstanding_balance
    FROM agent a
    LEFT JOIN policy pol ON pol.agent_id = a.agent_id
    LEFT JOIN premium_schedule ps ON ps.policy_id = pol.policy_id
    LEFT JOIN installment i ON i.premium_schedule_id = ps.premium_schedule_id AND i.installment_status = 'unpaid'
    GROUP BY a.agent_id, a.name
    ORDER BY a.agent_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_policy_count_by_status
--
-- Counts policies grouped by policy_status.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_policy_count_by_status //

CREATE PROCEDURE sp_report_policy_count_by_status()
BEGIN
    SELECT policy_status, COUNT(*) AS policy_count
    FROM policy
    GROUP BY policy_status
    ORDER BY policy_status ASC;
END //

-- ============================================================================
-- SECTION 2: Remaining coverage / coverage_type / beneficiary support
-- procedures (11)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- sp_coverage_update
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_coverage_update //

CREATE PROCEDURE sp_coverage_update(
    IN p_coverage_id BIGINT,
    IN p_coverage_type_id BIGINT,
    IN p_coverage_limit DECIMAL(12,2)
)
BEGIN
    UPDATE coverage
    SET coverage_type_id = p_coverage_type_id,
        coverage_limit   = p_coverage_limit
    WHERE coverage_id = p_coverage_id;
END //

-- ----------------------------------------------------------------------------
-- sp_coverage_delete
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_coverage_delete //

CREATE PROCEDURE sp_coverage_delete(
    IN p_coverage_id BIGINT
)
BEGIN
    DELETE FROM coverage WHERE coverage_id = p_coverage_id;
END //

-- ----------------------------------------------------------------------------
-- sp_coverage_type_get_by_id
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_coverage_type_get_by_id //

CREATE PROCEDURE sp_coverage_type_get_by_id(
    IN p_coverage_type_id BIGINT
)
BEGIN
    SELECT coverage_type_id, type_name
    FROM coverage_type
    WHERE coverage_type_id = p_coverage_type_id;
END //

-- ----------------------------------------------------------------------------
-- sp_coverage_type_update
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_coverage_type_update //

CREATE PROCEDURE sp_coverage_type_update(
    IN p_coverage_type_id BIGINT,
    IN p_type_name VARCHAR(50)
)
BEGIN
    UPDATE coverage_type
    SET type_name = p_type_name
    WHERE coverage_type_id = p_coverage_type_id;
END //

-- ----------------------------------------------------------------------------
-- sp_coverage_type_delete
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_coverage_type_delete //

CREATE PROCEDURE sp_coverage_type_delete(
    IN p_coverage_type_id BIGINT
)
BEGIN
    DELETE FROM coverage_type WHERE coverage_type_id = p_coverage_type_id;
END //

-- ----------------------------------------------------------------------------
-- sp_beneficiary_create
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_beneficiary_create //

CREATE PROCEDURE sp_beneficiary_create(
    IN  p_policy_id BIGINT,
    IN  p_name VARCHAR(100),
    IN  p_relationship_type VARCHAR(20),
    IN  p_allocation_percentage DECIMAL(5,2),
    OUT p_beneficiary_id BIGINT
)
BEGIN
    INSERT INTO beneficiary (policy_id, name, relationship_type, allocation_percentage)
    VALUES (p_policy_id, p_name, p_relationship_type, p_allocation_percentage);

    SET p_beneficiary_id = LAST_INSERT_ID();
END //

-- ----------------------------------------------------------------------------
-- sp_beneficiary_get_by_id
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_beneficiary_get_by_id //

CREATE PROCEDURE sp_beneficiary_get_by_id(
    IN p_beneficiary_id BIGINT
)
BEGIN
    SELECT beneficiary_id, policy_id, name, relationship_type, allocation_percentage
    FROM beneficiary
    WHERE beneficiary_id = p_beneficiary_id;
END //

-- ----------------------------------------------------------------------------
-- sp_beneficiary_list_by_policy_id
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_beneficiary_list_by_policy_id //

CREATE PROCEDURE sp_beneficiary_list_by_policy_id(
    IN p_policy_id BIGINT
)
BEGIN
    SELECT beneficiary_id, policy_id, name, relationship_type, allocation_percentage
    FROM beneficiary
    WHERE policy_id = p_policy_id
    ORDER BY beneficiary_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_beneficiary_update
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_beneficiary_update //

CREATE PROCEDURE sp_beneficiary_update(
    IN p_beneficiary_id BIGINT,
    IN p_name VARCHAR(100),
    IN p_relationship_type VARCHAR(20),
    IN p_allocation_percentage DECIMAL(5,2)
)
BEGIN
    UPDATE beneficiary
    SET name = p_name,
        relationship_type = p_relationship_type,
        allocation_percentage = p_allocation_percentage
    WHERE beneficiary_id = p_beneficiary_id;
END //

-- ----------------------------------------------------------------------------
-- sp_beneficiary_delete
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_beneficiary_delete //

CREATE PROCEDURE sp_beneficiary_delete(
    IN p_beneficiary_id BIGINT
)
BEGIN
    DELETE FROM beneficiary WHERE beneficiary_id = p_beneficiary_id;
END //

-- ----------------------------------------------------------------------------
-- sp_beneficiary_sum_allocation_by_policy_id
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_beneficiary_sum_allocation_by_policy_id //

CREATE PROCEDURE sp_beneficiary_sum_allocation_by_policy_id(
    IN p_policy_id BIGINT
)
BEGIN
    SELECT COALESCE(SUM(allocation_percentage), 0.00) AS total_allocation_percentage
    FROM beneficiary
    WHERE policy_id = p_policy_id;
END //

-- ============================================================================
-- SECTION 3: audit_log / endorsement_history support procedures (4)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- sp_audit_log_create
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_audit_log_create //

CREATE PROCEDURE sp_audit_log_create(
    IN  p_entity_name VARCHAR(50),
    IN  p_entity_id BIGINT,
    IN  p_action VARCHAR(20),
    OUT p_audit_log_id BIGINT
)
BEGIN
    INSERT INTO audit_log (entity_name, entity_id, action)
    VALUES (p_entity_name, p_entity_id, p_action);

    SET p_audit_log_id = LAST_INSERT_ID();
END //

-- ----------------------------------------------------------------------------
-- sp_audit_log_list_by_entity
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_audit_log_list_by_entity //

CREATE PROCEDURE sp_audit_log_list_by_entity(
    IN p_entity_name VARCHAR(50),
    IN p_entity_id BIGINT
)
BEGIN
    SELECT audit_log_id, entity_name, entity_id, action, logged_at
    FROM audit_log
    WHERE entity_name = p_entity_name
      AND entity_id = p_entity_id
    ORDER BY logged_at ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_endorsement_history_create
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_endorsement_history_create //

CREATE PROCEDURE sp_endorsement_history_create(
    IN  p_endorsement_id BIGINT,
    IN  p_action VARCHAR(20),
    OUT p_endorsement_history_id BIGINT
)
BEGIN
    INSERT INTO endorsement_history (endorsement_id, action)
    VALUES (p_endorsement_id, p_action);

    SET p_endorsement_history_id = LAST_INSERT_ID();
END //

-- ----------------------------------------------------------------------------
-- sp_endorsement_history_list_by_endorsement_id
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_endorsement_history_list_by_endorsement_id //

CREATE PROCEDURE sp_endorsement_history_list_by_endorsement_id(
    IN p_endorsement_id BIGINT
)
BEGIN
    SELECT endorsement_history_id, endorsement_id, action, recorded_at
    FROM endorsement_history
    WHERE endorsement_id = p_endorsement_id
    ORDER BY recorded_at ASC;
END //

-- ============================================================================
-- SECTION 4: Additional reporting/support procedures (20)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- sp_report_policy_count_by_status_and_agent
--
-- Counts policies grouped by both agent_id and policy_status.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_policy_count_by_status_and_agent //

CREATE PROCEDURE sp_report_policy_count_by_status_and_agent()
BEGIN
    SELECT agent_id, policy_status, COUNT(*) AS policy_count
    FROM policy
    GROUP BY agent_id, policy_status
    ORDER BY agent_id ASC, policy_status ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_premium_total_by_coverage_type
--
-- Sums coverage_limit grouped by coverage_type, joined through coverage.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_premium_total_by_coverage_type //

CREATE PROCEDURE sp_report_premium_total_by_coverage_type()
BEGIN
    SELECT ct.coverage_type_id, ct.type_name,
           COALESCE(SUM(cv.coverage_limit), 0.00) AS total_coverage_limit
    FROM coverage_type ct
    LEFT JOIN coverage cv ON cv.coverage_type_id = ct.coverage_type_id
    GROUP BY ct.coverage_type_id, ct.type_name
    ORDER BY ct.coverage_type_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_new_policy_count_by_month
--
-- Counts newly created policies grouped by month, for a given year.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_new_policy_count_by_month //

CREATE PROCEDURE sp_report_new_policy_count_by_month(
    IN p_year INT
)
BEGIN
    SELECT MONTH(created_at) AS month_number, COUNT(*) AS new_policy_count
    FROM policy
    WHERE YEAR(created_at) = p_year
    GROUP BY MONTH(created_at)
    ORDER BY month_number ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_payment_count_by_month
--
-- Counts payments grouped by month, for a given year.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_payment_count_by_month //

CREATE PROCEDURE sp_report_payment_count_by_month(
    IN p_year INT
)
BEGIN
    SELECT MONTH(payment_date) AS month_number, COUNT(*) AS payment_count,
           SUM(payment_amount) AS total_amount
    FROM payment
    WHERE YEAR(payment_date) = p_year
    GROUP BY MONTH(payment_date)
    ORDER BY month_number ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_installment_count_by_status
--
-- Counts installments grouped by installment_status.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_installment_count_by_status //

CREATE PROCEDURE sp_report_installment_count_by_status()
BEGIN
    SELECT installment_status, COUNT(*) AS installment_count,
           COALESCE(SUM(amount_due), 0.00) AS total_amount_due,
           COALESCE(SUM(amount_paid), 0.00) AS total_amount_paid
    FROM installment
    GROUP BY installment_status
    ORDER BY installment_status ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_customer_count_by_segment
--
-- Counts customers grouped by segment_code.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_customer_count_by_segment //

CREATE PROCEDURE sp_report_customer_count_by_segment()
BEGIN
    SELECT segment_code, COUNT(*) AS customer_count
    FROM customer
    GROUP BY segment_code
    ORDER BY segment_code ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_agent_policy_count
--
-- Counts policies grouped by agent_id (distinct from book-of-business,
-- a lightweight per-agent total used for dashboard summaries).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_agent_policy_count //

CREATE PROCEDURE sp_report_agent_policy_count()
BEGIN
    SELECT a.agent_id, a.name, COUNT(p.policy_id) AS policy_count
    FROM agent a
    LEFT JOIN policy p ON p.agent_id = a.agent_id
    GROUP BY a.agent_id, a.name
    ORDER BY a.agent_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_underwriter_active_count
--
-- Counts underwriters grouped by active_status.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_underwriter_active_count //

CREATE PROCEDURE sp_report_underwriter_active_count()
BEGIN
    SELECT active_status, COUNT(*) AS underwriter_count
    FROM underwriter
    GROUP BY active_status
    ORDER BY active_status ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_endorsement_adjustment_total_by_policy
--
-- Sums premium_adjustment across endorsements, grouped by policy_id.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_endorsement_adjustment_total_by_policy //

CREATE PROCEDURE sp_report_endorsement_adjustment_total_by_policy(
    IN p_policy_id BIGINT
)
BEGIN
    SELECT policy_id, COUNT(*) AS endorsement_count,
           COALESCE(SUM(premium_adjustment), 0.00) AS total_adjustment
    FROM endorsement
    WHERE policy_id = p_policy_id
    GROUP BY policy_id;
END //

-- ----------------------------------------------------------------------------
-- sp_report_renewal_notice_count_by_month
--
-- Counts renewal notices generated per month, for a given year.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_renewal_notice_count_by_month //

CREATE PROCEDURE sp_report_renewal_notice_count_by_month(
    IN p_year INT
)
BEGIN
    SELECT MONTH(generated_at) AS month_number, COUNT(*) AS renewal_notice_count
    FROM renewal_notice
    WHERE YEAR(generated_at) = p_year
    GROUP BY MONTH(generated_at)
    ORDER BY month_number ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_average_premium_by_billing_frequency
--
-- Averages total_premium grouped by billing_frequency.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_average_premium_by_billing_frequency //

CREATE PROCEDURE sp_report_average_premium_by_billing_frequency()
BEGIN
    SELECT billing_frequency, COUNT(*) AS policy_count,
           ROUND(AVG(total_premium), 2) AS average_premium
    FROM policy
    GROUP BY billing_frequency
    ORDER BY billing_frequency ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_beneficiary_count_by_relationship_type
--
-- Counts beneficiaries grouped by relationship_type.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_beneficiary_count_by_relationship_type //

CREATE PROCEDURE sp_report_beneficiary_count_by_relationship_type()
BEGIN
    SELECT relationship_type, COUNT(*) AS beneficiary_count
    FROM beneficiary
    GROUP BY relationship_type
    ORDER BY relationship_type ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_payment_allocation_total_by_installment
--
-- Sums amount_applied from payment_allocation, grouped by installment_id,
-- for a given policy (joined through premium_schedule).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_payment_allocation_total_by_installment //

CREATE PROCEDURE sp_report_payment_allocation_total_by_installment(
    IN p_policy_id BIGINT
)
BEGIN
    SELECT i.installment_id, i.due_date, i.amount_due,
           COALESCE(SUM(pa.amount_applied), 0.00) AS total_applied
    FROM installment i
    INNER JOIN premium_schedule ps ON ps.premium_schedule_id = i.premium_schedule_id
    LEFT JOIN payment_allocation pa ON pa.installment_id = i.installment_id
    WHERE ps.policy_id = p_policy_id
    GROUP BY i.installment_id, i.due_date, i.amount_due
    ORDER BY i.due_date ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_policy_term_length_distribution
--
-- Counts policies grouped by their term length in days (DATEDIFF between
-- term_end_date and term_start_date).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_policy_term_length_distribution //

CREATE PROCEDURE sp_report_policy_term_length_distribution()
BEGIN
    SELECT DATEDIFF(term_end_date, term_start_date) AS term_length_days,
           COUNT(*) AS policy_count
    FROM policy
    GROUP BY DATEDIFF(term_end_date, term_start_date)
    ORDER BY term_length_days ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_coverage_count_by_policy
--
-- Counts coverage rows grouped by policy_id.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_coverage_count_by_policy //

CREATE PROCEDURE sp_report_coverage_count_by_policy()
BEGIN
    SELECT policy_id, COUNT(*) AS coverage_count,
           COALESCE(SUM(coverage_limit), 0.00) AS total_coverage_limit
    FROM coverage
    GROUP BY policy_id
    ORDER BY policy_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_overdue_installment_count_by_agent
--
-- Counts overdue (unpaid, past due_date) installments grouped by the
-- policy's agent_id.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_overdue_installment_count_by_agent //

CREATE PROCEDURE sp_report_overdue_installment_count_by_agent()
BEGIN
    SELECT a.agent_id, a.name, COUNT(i.installment_id) AS overdue_installment_count
    FROM agent a
    LEFT JOIN policy pol ON pol.agent_id = a.agent_id
    LEFT JOIN premium_schedule ps ON ps.policy_id = pol.policy_id
    LEFT JOIN installment i ON i.premium_schedule_id = ps.premium_schedule_id
        AND i.installment_status = 'unpaid'
        AND i.due_date < CURDATE()
    GROUP BY a.agent_id, a.name
    ORDER BY a.agent_id ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_policy_status_history_count_by_policy
--
-- Counts policy_status_history rows grouped by policy_id (number of
-- transitions a policy has gone through).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_policy_status_history_count_by_policy //

CREATE PROCEDURE sp_report_policy_status_history_count_by_policy(
    IN p_policy_id BIGINT
)
BEGIN
    SELECT policy_id, COUNT(*) AS transition_count
    FROM policy_status_history
    WHERE policy_id = p_policy_id
    GROUP BY policy_id;
END //

-- ----------------------------------------------------------------------------
-- sp_report_audit_log_count_by_entity_name
--
-- Counts audit_log rows grouped by entity_name.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_audit_log_count_by_entity_name //

CREATE PROCEDURE sp_report_audit_log_count_by_entity_name()
BEGIN
    SELECT entity_name, COUNT(*) AS audit_log_count
    FROM audit_log
    GROUP BY entity_name
    ORDER BY entity_name ASC;
END //

-- ----------------------------------------------------------------------------
-- sp_report_top_agents_by_total_premium
--
-- Returns agents ranked by total_premium of their policies, limited to the
-- top p_limit_count agents.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_top_agents_by_total_premium //

CREATE PROCEDURE sp_report_top_agents_by_total_premium(
    IN p_limit_count INT
)
BEGIN
    DECLARE v_limit_count INT;

    IF p_limit_count IS NULL OR p_limit_count <= 0 THEN
        SET v_limit_count = 10;
    ELSE
        SET v_limit_count = p_limit_count;
    END IF;

    SELECT a.agent_id, a.name, COALESCE(SUM(p.total_premium), 0.00) AS total_premium
    FROM agent a
    LEFT JOIN policy p ON p.agent_id = a.agent_id
    GROUP BY a.agent_id, a.name
    ORDER BY total_premium DESC
    LIMIT v_limit_count;
END //

-- ----------------------------------------------------------------------------
-- sp_report_premium_adjustment_history_total_by_policy
--
-- Sums adjustment_amount from premium_adjustment_history, grouped by
-- policy_id, for a given policy.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_premium_adjustment_history_total_by_policy //

CREATE PROCEDURE sp_report_premium_adjustment_history_total_by_policy(
    IN p_policy_id BIGINT
)
BEGIN
    SELECT policy_id, COUNT(*) AS adjustment_count,
           COALESCE(SUM(adjustment_amount), 0.00) AS total_adjustment_amount
    FROM premium_adjustment_history
    WHERE policy_id = p_policy_id
    GROUP BY policy_id;
END //

DELIMITER ;
