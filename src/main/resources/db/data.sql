-- =====================================================================================
-- Insurance Policy Administration System - Seed Data
-- MySQL 8.0
--
-- Loads reference data (Customer, Agent, Underwriter, Coverage_Type) via plain INSERT
-- statements, then builds realistic Policy lifecycles via CALL statements against the
-- already-deployed stored procedures (sp_policy_create, sp_policy_status_transition,
-- sp_coverage_create, sp_premium_schedule_create + sp_premium_schedule_generate_installments,
-- sp_payment_apply, sp_endorsement_create + sp_endorsement_calculate_proration,
-- sp_beneficiary_create, sp_renewal_notice_generate, etc.) so derived/computed data
-- (installment amounts, payment allocations, status-transition history rows) is
-- guaranteed correct and consistent with the business rules those procedures encode.
--
-- Distribution:
--   30 Customers  (10 STANDARD / 10 PREFERRED / 10 HIGH_RISK)
--   8  Agents
--   5  Underwriters (4 active, 1 inactive)
--   5  Coverage_Types
--   60 Policies, 10 per Policy_Status (quoted, bound, active, lapsed, cancelled, renewed)
-- =====================================================================================

SET @today = CURDATE();

-- =====================================================================================
-- SECTION 1: Customers (30) - plain INSERT, Hibernate-backed table
-- =====================================================================================

INSERT INTO customer (first_name, last_name, date_of_birth, email, phone, segment_code) VALUES
('Alice',   'Nguyen',    '1985-03-14', 'alice.nguyen1@example.com',    '555-010-0001', 'STANDARD'),
('Brian',   'Johnson',   '1978-07-22', 'brian.johnson2@example.com',   '555-010-0002', 'STANDARD'),
('Carla',   'Smith',     '1990-11-02', 'carla.smith3@example.com',     '555-010-0003', 'STANDARD'),
('Derek',   'Williams',  '1982-01-30', 'derek.williams4@example.com',  '555-010-0004', 'STANDARD'),
('Elena',   'Brown',     '1975-05-18', 'elena.brown5@example.com',     '555-010-0005', 'STANDARD'),
('Felix',   'Jones',     '1988-09-09', 'felix.jones6@example.com',     '555-010-0006', 'STANDARD'),
('Grace',   'Garcia',    '1993-12-25', 'grace.garcia7@example.com',    '555-010-0007', 'STANDARD'),
('Hector',  'Martinez',  '1980-02-14', 'hector.martinez8@example.com', '555-010-0008', 'STANDARD'),
('Ivy',     'Davis',     '1991-06-06', 'ivy.davis9@example.com',       '555-010-0009', 'STANDARD'),
('Jack',    'Rodriguez', '1984-08-19', 'jack.rodriguez10@example.com', '555-010-0010', 'STANDARD'),
('Karen',   'Hernandez', '1979-04-11', 'karen.hernandez11@example.com','555-010-0011', 'PREFERRED'),
('Liam',    'Lopez',     '1986-10-23', 'liam.lopez12@example.com',     '555-010-0012', 'PREFERRED'),
('Mona',    'Gonzalez',  '1992-03-03', 'mona.gonzalez13@example.com',  '555-010-0013', 'PREFERRED'),
('Nathan',  'Wilson',    '1977-07-07', 'nathan.wilson14@example.com',  '555-010-0014', 'PREFERRED'),
('Olivia',  'Anderson',  '1989-11-29', 'olivia.anderson15@example.com','555-010-0015', 'PREFERRED'),
('Peter',   'Thomas',    '1983-05-05', 'peter.thomas16@example.com',   '555-010-0016', 'PREFERRED'),
('Quinn',   'Taylor',    '1994-02-02', 'quinn.taylor17@example.com',   '555-010-0017', 'PREFERRED'),
('Rosa',    'Moore',     '1981-09-16', 'rosa.moore18@example.com',     '555-010-0018', 'PREFERRED'),
('Samuel',  'Jackson',   '1976-12-12', 'samuel.jackson19@example.com', '555-010-0019', 'PREFERRED'),
('Tina',    'Martin',    '1990-01-21', 'tina.martin20@example.com',    '555-010-0020', 'PREFERRED'),
('Umar',    'Lee',       '1987-06-27', 'umar.lee21@example.com',       '555-010-0021', 'HIGH_RISK'),
('Vera',    'Perez',     '1973-10-10', 'vera.perez22@example.com',     '555-010-0022', 'HIGH_RISK'),
('Walter',  'Thompson',  '1995-04-04', 'walter.thompson23@example.com','555-010-0023', 'HIGH_RISK'),
('Ximena',  'White',     '1980-08-08', 'ximena.white24@example.com',   '555-010-0024', 'HIGH_RISK'),
('Yusuf',   'Harris',    '1984-03-23', 'yusuf.harris25@example.com',   '555-010-0025', 'HIGH_RISK'),
('Zara',    'Sanchez',   '1991-07-17', 'zara.sanchez26@example.com',   '555-010-0026', 'HIGH_RISK'),
('Adam',    'Clark',     '1978-11-11', 'adam.clark27@example.com',     '555-010-0027', 'HIGH_RISK'),
('Bella',   'Ramirez',   '1986-02-28', 'bella.ramirez28@example.com',  '555-010-0028', 'HIGH_RISK'),
('Caleb',   'Lewis',     '1992-05-15', 'caleb.lewis29@example.com',    '555-010-0029', 'HIGH_RISK'),
('Diana',   'Robinson',  '1983-09-09', 'diana.robinson30@example.com', '555-010-0030', 'HIGH_RISK');

-- =====================================================================================
-- SECTION 2: Agents (8) - plain INSERT, Hibernate-backed table
-- =====================================================================================

INSERT INTO agent (name, license_number, email, phone) VALUES
('Agent Olivia Hughes',  'LIC-AGT-0001', 'olivia.hughes@ipaagents.example.com',  '555-020-0001'),
('Agent Marcus Reed',    'LIC-AGT-0002', 'marcus.reed@ipaagents.example.com',    '555-020-0002'),
('Agent Priya Shah',     'LIC-AGT-0003', 'priya.shah@ipaagents.example.com',     '555-020-0003'),
('Agent Daniel Kim',     'LIC-AGT-0004', 'daniel.kim@ipaagents.example.com',     '555-020-0004'),
('Agent Fatima Noor',    'LIC-AGT-0005', 'fatima.noor@ipaagents.example.com',    '555-020-0005'),
('Agent George Patel',   'LIC-AGT-0006', 'george.patel@ipaagents.example.com',   '555-020-0006'),
('Agent Helen Ortiz',    'LIC-AGT-0007', 'helen.ortiz@ipaagents.example.com',    '555-020-0007'),
('Agent Ivan Costa',     'LIC-AGT-0008', 'ivan.costa@ipaagents.example.com',     '555-020-0008');

-- =====================================================================================
-- SECTION 3: Underwriters (5: 4 active, 1 inactive) - via stored procedure
-- =====================================================================================

CALL sp_underwriter_create('Underwriter Nancy Price',  'LIC-UW-0001', 'active',   @uw1);
CALL sp_underwriter_create('Underwriter Oscar Dunn',    'LIC-UW-0002', 'active',   @uw2);
CALL sp_underwriter_create('Underwriter Paula Chen',    'LIC-UW-0003', 'active',   @uw3);
CALL sp_underwriter_create('Underwriter Quentin Ross',  'LIC-UW-0004', 'active',   @uw4);
CALL sp_underwriter_create('Underwriter Rita Fox',      'LIC-UW-0005', 'inactive', @uw5);

-- =====================================================================================
-- SECTION 4: Coverage_Type (5) - via stored procedure
-- =====================================================================================

CALL sp_coverage_type_create('Liability',        @ct1);
CALL sp_coverage_type_create('Collision',        @ct2);
CALL sp_coverage_type_create('Comprehensive',     @ct3);
CALL sp_coverage_type_create('Medical Payments',  @ct4);
CALL sp_coverage_type_create('Uninsured Motorist',@ct5);


-- =====================================================================================
-- SECTION 5: Policies - 10 per status (60 total), built via stored-procedure CALLs.
--
-- Each policy block:
--   1. sp_policy_create                      -> policy row (status defaults to 'quoted')
--   2. sp_policy_status_history_create        -> initial quoted history row (previous_status = NULL)
--   3. sp_coverage_create (x1-2)              -> Coverage row(s)
--   4. For non-quoted target statuses, drive the status machine forward one
--      transition at a time via sp_policy_status_transition, binding (setting
--      underwriter_id + generating the Premium_Schedule/Installments) at the
--      quoted -> bound step.
--
-- Term dates are chosen so that:
--   - several 'active' policies have term_end_date within the next 30-60 days
--     (exercises the expiring-policies report and renewal-notice generation)
--   - several bound/active/lapsed/cancelled/renewed policies have at least one
--     unpaid installment with a past due_date (exercises the overdue-payments report)
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 5.1 QUOTED policies (10) - stay in 'quoted', never bound, underwriter_id stays NULL
-- -------------------------------------------------------------------------------------

CALL sp_policy_create('POL-2025-1001', 1,  1, DATE_SUB(@today, INTERVAL 10 DAY), DATE_ADD(@today, INTERVAL 355 DAY), 'monthly',     @p1001);
CALL sp_policy_status_history_create(@p1001, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1001, @ct1, 50000.00, @cov);
CALL sp_coverage_create(@p1001, @ct2, 20000.00, @cov);

CALL sp_policy_create('POL-2025-1002', 2,  2, DATE_SUB(@today, INTERVAL 8 DAY),  DATE_ADD(@today, INTERVAL 357 DAY), 'quarterly',   @p1002);
CALL sp_policy_status_history_create(@p1002, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1002, @ct1, 75000.00, @cov);

CALL sp_policy_create('POL-2025-1003', 3,  3, DATE_SUB(@today, INTERVAL 5 DAY),  DATE_ADD(@today, INTERVAL 360 DAY), 'semi_annual', @p1003);
CALL sp_policy_status_history_create(@p1003, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1003, @ct3, 30000.00, @cov);

CALL sp_policy_create('POL-2025-1004', 4,  4, DATE_SUB(@today, INTERVAL 3 DAY),  DATE_ADD(@today, INTERVAL 362 DAY), 'annual',      @p1004);
CALL sp_policy_status_history_create(@p1004, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1004, @ct4, 15000.00, @cov);

CALL sp_policy_create('POL-2025-1005', 5,  5, @today,                             DATE_ADD(@today, INTERVAL 365 DAY), 'monthly',     @p1005);
CALL sp_policy_status_history_create(@p1005, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1005, @ct5, 10000.00, @cov);

CALL sp_policy_create('POL-2025-1006', 6,  6, @today,                             DATE_ADD(@today, INTERVAL 365 DAY), 'quarterly',   @p1006);
CALL sp_policy_status_history_create(@p1006, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1006, @ct1, 60000.00, @cov);

CALL sp_policy_create('POL-2025-1007', 7,  7, @today,                             DATE_ADD(@today, INTERVAL 365 DAY), 'semi_annual', @p1007);
CALL sp_policy_status_history_create(@p1007, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1007, @ct2, 45000.00, @cov);

CALL sp_policy_create('POL-2025-1008', 8,  8, @today,                             DATE_ADD(@today, INTERVAL 365 DAY), 'annual',      @p1008);
CALL sp_policy_status_history_create(@p1008, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1008, @ct3, 25000.00, @cov);

CALL sp_policy_create('POL-2025-1009', 9,  1, @today,                             DATE_ADD(@today, INTERVAL 365 DAY), 'monthly',     @p1009);
CALL sp_policy_status_history_create(@p1009, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1009, @ct4, 18000.00, @cov);

CALL sp_policy_create('POL-2025-1010', 10, 2, @today,                             DATE_ADD(@today, INTERVAL 365 DAY), 'quarterly',   @p1010);
CALL sp_policy_status_history_create(@p1010, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p1010, @ct5, 22000.00, @cov);

-- -------------------------------------------------------------------------------------
-- 5.2 BOUND policies (10) - quoted -> bound, underwriter assigned, Premium_Schedule +
--     Installments generated (sp_policy_status_transition does not set underwriter_id,
--     so sp_policy_update assigns it as part of the bind step, matching the service-
--     layer bind contract in design.md).
-- -------------------------------------------------------------------------------------

CALL sp_policy_create('POL-2025-2001', 11, 3, DATE_SUB(@today, INTERVAL 20 DAY), DATE_ADD(@today, INTERVAL 345 DAY), 'monthly',     @p2001);
CALL sp_policy_status_history_create(@p2001, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2001, @ct1, 55000.00, @cov);
CALL sp_policy_status_transition(@p2001, 'bound', @sc);
CALL sp_policy_update(@p2001, 3, @uw1, DATE_ADD(@today, INTERVAL 345 DAY), 'monthly', 1200.00);
CALL sp_premium_schedule_create(@p2001, 'monthly', 1200.00, @ps2001);
CALL sp_premium_schedule_generate_installments(@p2001, 1200.00, 'monthly', DATE_SUB(@today, INTERVAL 20 DAY));

CALL sp_policy_create('POL-2025-2002', 12, 4, DATE_SUB(@today, INTERVAL 18 DAY), DATE_ADD(@today, INTERVAL 347 DAY), 'quarterly',   @p2002);
CALL sp_policy_status_history_create(@p2002, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2002, @ct2, 40000.00, @cov);
CALL sp_policy_status_transition(@p2002, 'bound', @sc);
CALL sp_policy_update(@p2002, 4, @uw2, DATE_ADD(@today, INTERVAL 347 DAY), 'quarterly', 2000.00);
CALL sp_premium_schedule_create(@p2002, 'quarterly', 2000.00, @ps2002);
CALL sp_premium_schedule_generate_installments(@p2002, 2000.00, 'quarterly', DATE_SUB(@today, INTERVAL 18 DAY));

CALL sp_policy_create('POL-2025-2003', 13, 5, DATE_SUB(@today, INTERVAL 15 DAY), DATE_ADD(@today, INTERVAL 350 DAY), 'semi_annual', @p2003);
CALL sp_policy_status_history_create(@p2003, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2003, @ct3, 35000.00, @cov);
CALL sp_policy_status_transition(@p2003, 'bound', @sc);
CALL sp_policy_update(@p2003, 5, @uw3, DATE_ADD(@today, INTERVAL 350 DAY), 'semi_annual', 1800.00);
CALL sp_premium_schedule_create(@p2003, 'semi_annual', 1800.00, @ps2003);
CALL sp_premium_schedule_generate_installments(@p2003, 1800.00, 'semi_annual', DATE_SUB(@today, INTERVAL 15 DAY));

CALL sp_policy_create('POL-2025-2004', 14, 6, DATE_SUB(@today, INTERVAL 12 DAY), DATE_ADD(@today, INTERVAL 353 DAY), 'annual',      @p2004);
CALL sp_policy_status_history_create(@p2004, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2004, @ct4, 20000.00, @cov);
CALL sp_policy_status_transition(@p2004, 'bound', @sc);
CALL sp_policy_update(@p2004, 6, @uw4, DATE_ADD(@today, INTERVAL 353 DAY), 'annual', 1000.00);
CALL sp_premium_schedule_create(@p2004, 'annual', 1000.00, @ps2004);
CALL sp_premium_schedule_generate_installments(@p2004, 1000.00, 'annual', DATE_SUB(@today, INTERVAL 12 DAY));

CALL sp_policy_create('POL-2025-2005', 15, 7, DATE_SUB(@today, INTERVAL 9 DAY),  DATE_ADD(@today, INTERVAL 356 DAY), 'monthly',     @p2005);
CALL sp_policy_status_history_create(@p2005, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2005, @ct5, 16000.00, @cov);
CALL sp_policy_status_transition(@p2005, 'bound', @sc);
CALL sp_policy_update(@p2005, 7, @uw1, DATE_ADD(@today, INTERVAL 356 DAY), 'monthly', 960.00);
CALL sp_premium_schedule_create(@p2005, 'monthly', 960.00, @ps2005);
CALL sp_premium_schedule_generate_installments(@p2005, 960.00, 'monthly', DATE_SUB(@today, INTERVAL 9 DAY));

CALL sp_policy_create('POL-2025-2006', 16, 8, DATE_SUB(@today, INTERVAL 7 DAY),  DATE_ADD(@today, INTERVAL 358 DAY), 'quarterly',   @p2006);
CALL sp_policy_status_history_create(@p2006, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2006, @ct1, 48000.00, @cov);
CALL sp_policy_status_transition(@p2006, 'bound', @sc);
CALL sp_policy_update(@p2006, 8, @uw2, DATE_ADD(@today, INTERVAL 358 DAY), 'quarterly', 1440.00);
CALL sp_premium_schedule_create(@p2006, 'quarterly', 1440.00, @ps2006);
CALL sp_premium_schedule_generate_installments(@p2006, 1440.00, 'quarterly', DATE_SUB(@today, INTERVAL 7 DAY));

CALL sp_policy_create('POL-2025-2007', 17, 1, DATE_SUB(@today, INTERVAL 6 DAY),  DATE_ADD(@today, INTERVAL 359 DAY), 'semi_annual', @p2007);
CALL sp_policy_status_history_create(@p2007, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2007, @ct2, 33000.00, @cov);
CALL sp_policy_status_transition(@p2007, 'bound', @sc);
CALL sp_policy_update(@p2007, 1, @uw3, DATE_ADD(@today, INTERVAL 359 DAY), 'semi_annual', 1500.00);
CALL sp_premium_schedule_create(@p2007, 'semi_annual', 1500.00, @ps2007);
CALL sp_premium_schedule_generate_installments(@p2007, 1500.00, 'semi_annual', DATE_SUB(@today, INTERVAL 6 DAY));

CALL sp_policy_create('POL-2025-2008', 18, 2, DATE_SUB(@today, INTERVAL 4 DAY),  DATE_ADD(@today, INTERVAL 361 DAY), 'annual',      @p2008);
CALL sp_policy_status_history_create(@p2008, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2008, @ct3, 27000.00, @cov);
CALL sp_policy_status_transition(@p2008, 'bound', @sc);
CALL sp_policy_update(@p2008, 2, @uw4, DATE_ADD(@today, INTERVAL 361 DAY), 'annual', 1100.00);
CALL sp_premium_schedule_create(@p2008, 'annual', 1100.00, @ps2008);
CALL sp_premium_schedule_generate_installments(@p2008, 1100.00, 'annual', DATE_SUB(@today, INTERVAL 4 DAY));

CALL sp_policy_create('POL-2025-2009', 19, 3, DATE_SUB(@today, INTERVAL 2 DAY),  DATE_ADD(@today, INTERVAL 363 DAY), 'monthly',     @p2009);
CALL sp_policy_status_history_create(@p2009, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2009, @ct4, 21000.00, @cov);
CALL sp_policy_status_transition(@p2009, 'bound', @sc);
CALL sp_policy_update(@p2009, 3, @uw1, DATE_ADD(@today, INTERVAL 363 DAY), 'monthly', 1320.00);
CALL sp_premium_schedule_create(@p2009, 'monthly', 1320.00, @ps2009);
CALL sp_premium_schedule_generate_installments(@p2009, 1320.00, 'monthly', DATE_SUB(@today, INTERVAL 2 DAY));

CALL sp_policy_create('POL-2025-2010', 20, 4, @today,                             DATE_ADD(@today, INTERVAL 365 DAY), 'quarterly',   @p2010);
CALL sp_policy_status_history_create(@p2010, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p2010, @ct5, 19000.00, @cov);
CALL sp_policy_status_transition(@p2010, 'bound', @sc);
CALL sp_policy_update(@p2010, 4, @uw2, DATE_ADD(@today, INTERVAL 365 DAY), 'quarterly', 1600.00);
CALL sp_premium_schedule_create(@p2010, 'quarterly', 1600.00, @ps2010);
CALL sp_premium_schedule_generate_installments(@p2010, 1600.00, 'quarterly', @today);

-- -------------------------------------------------------------------------------------
-- 5.3 ACTIVE policies (10) - quoted -> bound -> active. term_start_date is set further
--     in the past so the first installment(s) are already due, giving several
--     policies a genuinely overdue unpaid installment (exercises the overdue-payments
--     report) while others receive a payment that clears the first installment.
--     Policies 3005-3007 have term_end_date within the next 45 days (exercises the
--     expiring-policies report and renewal-notice generation).
-- -------------------------------------------------------------------------------------

CALL sp_policy_create('POL-2025-3001', 21, 5, DATE_SUB(@today, INTERVAL 60 DAY), DATE_ADD(@today, INTERVAL 305 DAY), 'monthly',     @p3001);
CALL sp_policy_status_history_create(@p3001, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3001, @ct1, 52000.00, @cov);
CALL sp_policy_status_transition(@p3001, 'bound', @sc);
CALL sp_policy_update(@p3001, 5, @uw1, DATE_ADD(@today, INTERVAL 305 DAY), 'monthly', 1200.00);
CALL sp_premium_schedule_create(@p3001, 'monthly', 1200.00, @ps3001);
CALL sp_premium_schedule_generate_installments(@p3001, 1200.00, 'monthly', DATE_SUB(@today, INTERVAL 60 DAY));
CALL sp_policy_status_transition(@p3001, 'active', @sc);

CALL sp_policy_create('POL-2025-3002', 22, 6, DATE_SUB(@today, INTERVAL 55 DAY), DATE_ADD(@today, INTERVAL 310 DAY), 'quarterly',   @p3002);
CALL sp_policy_status_history_create(@p3002, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3002, @ct2, 42000.00, @cov);
CALL sp_policy_status_transition(@p3002, 'bound', @sc);
CALL sp_policy_update(@p3002, 6, @uw2, DATE_ADD(@today, INTERVAL 310 DAY), 'quarterly', 2400.00);
CALL sp_premium_schedule_create(@p3002, 'quarterly', 2400.00, @ps3002);
CALL sp_premium_schedule_generate_installments(@p3002, 2400.00, 'quarterly', DATE_SUB(@today, INTERVAL 55 DAY));
CALL sp_policy_status_transition(@p3002, 'active', @sc);

CALL sp_policy_create('POL-2025-3003', 23, 7, DATE_SUB(@today, INTERVAL 50 DAY), DATE_ADD(@today, INTERVAL 315 DAY), 'semi_annual', @p3003);
CALL sp_policy_status_history_create(@p3003, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3003, @ct3, 37000.00, @cov);
CALL sp_policy_status_transition(@p3003, 'bound', @sc);
CALL sp_policy_update(@p3003, 7, @uw3, DATE_ADD(@today, INTERVAL 315 DAY), 'semi_annual', 2200.00);
CALL sp_premium_schedule_create(@p3003, 'semi_annual', 2200.00, @ps3003);
CALL sp_premium_schedule_generate_installments(@p3003, 2200.00, 'semi_annual', DATE_SUB(@today, INTERVAL 50 DAY));
CALL sp_policy_status_transition(@p3003, 'active', @sc);

CALL sp_policy_create('POL-2025-3004', 24, 8, DATE_SUB(@today, INTERVAL 45 DAY), DATE_ADD(@today, INTERVAL 320 DAY), 'annual',      @p3004);
CALL sp_policy_status_history_create(@p3004, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3004, @ct4, 24000.00, @cov);
CALL sp_policy_status_transition(@p3004, 'bound', @sc);
CALL sp_policy_update(@p3004, 8, @uw4, DATE_ADD(@today, INTERVAL 320 DAY), 'annual', 1300.00);
CALL sp_premium_schedule_create(@p3004, 'annual', 1300.00, @ps3004);
CALL sp_premium_schedule_generate_installments(@p3004, 1300.00, 'annual', DATE_SUB(@today, INTERVAL 45 DAY));
CALL sp_policy_status_transition(@p3004, 'active', @sc);

-- Expiring soon (term_end_date within 45 days) - exercises expiring-policies report
CALL sp_policy_create('POL-2025-3005', 25, 1, DATE_SUB(@today, INTERVAL 320 DAY), DATE_ADD(@today, INTERVAL 20 DAY), 'monthly',    @p3005);
CALL sp_policy_status_history_create(@p3005, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3005, @ct5, 17000.00, @cov);
CALL sp_policy_status_transition(@p3005, 'bound', @sc);
CALL sp_policy_update(@p3005, 1, @uw1, DATE_ADD(@today, INTERVAL 20 DAY), 'monthly', 1080.00);
CALL sp_premium_schedule_create(@p3005, 'monthly', 1080.00, @ps3005);
CALL sp_premium_schedule_generate_installments(@p3005, 1080.00, 'monthly', DATE_SUB(@today, INTERVAL 320 DAY));
CALL sp_policy_status_transition(@p3005, 'active', @sc);

CALL sp_policy_create('POL-2025-3006', 26, 2, DATE_SUB(@today, INTERVAL 330 DAY), DATE_ADD(@today, INTERVAL 10 DAY), 'quarterly',  @p3006);
CALL sp_policy_status_history_create(@p3006, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3006, @ct1, 46000.00, @cov);
CALL sp_policy_status_transition(@p3006, 'bound', @sc);
CALL sp_policy_update(@p3006, 2, @uw2, DATE_ADD(@today, INTERVAL 10 DAY), 'quarterly', 2000.00);
CALL sp_premium_schedule_create(@p3006, 'quarterly', 2000.00, @ps3006);
CALL sp_premium_schedule_generate_installments(@p3006, 2000.00, 'quarterly', DATE_SUB(@today, INTERVAL 330 DAY));
CALL sp_policy_status_transition(@p3006, 'active', @sc);

CALL sp_policy_create('POL-2025-3007', 27, 3, DATE_SUB(@today, INTERVAL 340 DAY), DATE_ADD(@today, INTERVAL 25 DAY), 'semi_annual',@p3007);
CALL sp_policy_status_history_create(@p3007, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3007, @ct2, 31000.00, @cov);
CALL sp_policy_status_transition(@p3007, 'bound', @sc);
CALL sp_policy_update(@p3007, 3, @uw3, DATE_ADD(@today, INTERVAL 25 DAY), 'semi_annual', 1750.00);
CALL sp_premium_schedule_create(@p3007, 'semi_annual', 1750.00, @ps3007);
CALL sp_premium_schedule_generate_installments(@p3007, 1750.00, 'semi_annual', DATE_SUB(@today, INTERVAL 340 DAY));
CALL sp_policy_status_transition(@p3007, 'active', @sc);

CALL sp_policy_create('POL-2025-3008', 28, 4, DATE_SUB(@today, INTERVAL 40 DAY), DATE_ADD(@today, INTERVAL 325 DAY), 'monthly',    @p3008);
CALL sp_policy_status_history_create(@p3008, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3008, @ct3, 23000.00, @cov);
CALL sp_policy_status_transition(@p3008, 'bound', @sc);
CALL sp_policy_update(@p3008, 4, @uw4, DATE_ADD(@today, INTERVAL 325 DAY), 'monthly', 960.00);
CALL sp_premium_schedule_create(@p3008, 'monthly', 960.00, @ps3008);
CALL sp_premium_schedule_generate_installments(@p3008, 960.00, 'monthly', DATE_SUB(@today, INTERVAL 40 DAY));
CALL sp_policy_status_transition(@p3008, 'active', @sc);

CALL sp_policy_create('POL-2025-3009', 29, 5, DATE_SUB(@today, INTERVAL 35 DAY), DATE_ADD(@today, INTERVAL 330 DAY), 'quarterly',  @p3009);
CALL sp_policy_status_history_create(@p3009, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3009, @ct4, 20000.00, @cov);
CALL sp_policy_status_transition(@p3009, 'bound', @sc);
CALL sp_policy_update(@p3009, 5, @uw1, DATE_ADD(@today, INTERVAL 330 DAY), 'quarterly', 1680.00);
CALL sp_premium_schedule_create(@p3009, 'quarterly', 1680.00, @ps3009);
CALL sp_premium_schedule_generate_installments(@p3009, 1680.00, 'quarterly', DATE_SUB(@today, INTERVAL 35 DAY));
CALL sp_policy_status_transition(@p3009, 'active', @sc);

CALL sp_policy_create('POL-2025-3010', 30, 6, DATE_SUB(@today, INTERVAL 30 DAY), DATE_ADD(@today, INTERVAL 335 DAY), 'annual',     @p3010);
CALL sp_policy_status_history_create(@p3010, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p3010, @ct5, 18000.00, @cov);
CALL sp_policy_status_transition(@p3010, 'bound', @sc);
CALL sp_policy_update(@p3010, 6, @uw2, DATE_ADD(@today, INTERVAL 335 DAY), 'annual', 1450.00);
CALL sp_premium_schedule_create(@p3010, 'annual', 1450.00, @ps3010);
CALL sp_premium_schedule_generate_installments(@p3010, 1450.00, 'annual', DATE_SUB(@today, INTERVAL 30 DAY));
CALL sp_policy_status_transition(@p3010, 'active', @sc);

-- -------------------------------------------------------------------------------------
-- 5.4 LAPSED policies (10) - quoted -> bound -> active -> lapsed
-- -------------------------------------------------------------------------------------

CALL sp_policy_create('POL-2025-4001', 1, 7, DATE_SUB(@today, INTERVAL 200 DAY), DATE_ADD(@today, INTERVAL 165 DAY), 'monthly',     @p4001);
CALL sp_policy_status_history_create(@p4001, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4001, @ct1, 51000.00, @cov);
CALL sp_policy_status_transition(@p4001, 'bound', @sc);
CALL sp_policy_update(@p4001, 7, @uw3, DATE_ADD(@today, INTERVAL 165 DAY), 'monthly', 1140.00);
CALL sp_premium_schedule_create(@p4001, 'monthly', 1140.00, @ps4001);
CALL sp_premium_schedule_generate_installments(@p4001, 1140.00, 'monthly', DATE_SUB(@today, INTERVAL 200 DAY));
CALL sp_policy_status_transition(@p4001, 'active', @sc);
CALL sp_policy_status_transition(@p4001, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4002', 2, 8, DATE_SUB(@today, INTERVAL 190 DAY), DATE_ADD(@today, INTERVAL 175 DAY), 'quarterly',   @p4002);
CALL sp_policy_status_history_create(@p4002, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4002, @ct2, 41000.00, @cov);
CALL sp_policy_status_transition(@p4002, 'bound', @sc);
CALL sp_policy_update(@p4002, 8, @uw4, DATE_ADD(@today, INTERVAL 175 DAY), 'quarterly', 2280.00);
CALL sp_premium_schedule_create(@p4002, 'quarterly', 2280.00, @ps4002);
CALL sp_premium_schedule_generate_installments(@p4002, 2280.00, 'quarterly', DATE_SUB(@today, INTERVAL 190 DAY));
CALL sp_policy_status_transition(@p4002, 'active', @sc);
CALL sp_policy_status_transition(@p4002, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4003', 3, 1, DATE_SUB(@today, INTERVAL 180 DAY), DATE_ADD(@today, INTERVAL 185 DAY), 'semi_annual', @p4003);
CALL sp_policy_status_history_create(@p4003, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4003, @ct3, 36000.00, @cov);
CALL sp_policy_status_transition(@p4003, 'bound', @sc);
CALL sp_policy_update(@p4003, 1, @uw1, DATE_ADD(@today, INTERVAL 185 DAY), 'semi_annual', 2100.00);
CALL sp_premium_schedule_create(@p4003, 'semi_annual', 2100.00, @ps4003);
CALL sp_premium_schedule_generate_installments(@p4003, 2100.00, 'semi_annual', DATE_SUB(@today, INTERVAL 180 DAY));
CALL sp_policy_status_transition(@p4003, 'active', @sc);
CALL sp_policy_status_transition(@p4003, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4004', 4, 2, DATE_SUB(@today, INTERVAL 170 DAY), DATE_ADD(@today, INTERVAL 195 DAY), 'annual',      @p4004);
CALL sp_policy_status_history_create(@p4004, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4004, @ct4, 23000.00, @cov);
CALL sp_policy_status_transition(@p4004, 'bound', @sc);
CALL sp_policy_update(@p4004, 2, @uw2, DATE_ADD(@today, INTERVAL 195 DAY), 'annual', 1250.00);
CALL sp_premium_schedule_create(@p4004, 'annual', 1250.00, @ps4004);
CALL sp_premium_schedule_generate_installments(@p4004, 1250.00, 'annual', DATE_SUB(@today, INTERVAL 170 DAY));
CALL sp_policy_status_transition(@p4004, 'active', @sc);
CALL sp_policy_status_transition(@p4004, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4005', 5, 3, DATE_SUB(@today, INTERVAL 160 DAY), DATE_ADD(@today, INTERVAL 205 DAY), 'monthly',     @p4005);
CALL sp_policy_status_history_create(@p4005, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4005, @ct5, 16500.00, @cov);
CALL sp_policy_status_transition(@p4005, 'bound', @sc);
CALL sp_policy_update(@p4005, 3, @uw3, DATE_ADD(@today, INTERVAL 205 DAY), 'monthly', 1020.00);
CALL sp_premium_schedule_create(@p4005, 'monthly', 1020.00, @ps4005);
CALL sp_premium_schedule_generate_installments(@p4005, 1020.00, 'monthly', DATE_SUB(@today, INTERVAL 160 DAY));
CALL sp_policy_status_transition(@p4005, 'active', @sc);
CALL sp_policy_status_transition(@p4005, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4006', 6, 4, DATE_SUB(@today, INTERVAL 150 DAY), DATE_ADD(@today, INTERVAL 215 DAY), 'quarterly',   @p4006);
CALL sp_policy_status_history_create(@p4006, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4006, @ct1, 47000.00, @cov);
CALL sp_policy_status_transition(@p4006, 'bound', @sc);
CALL sp_policy_update(@p4006, 4, @uw4, DATE_ADD(@today, INTERVAL 215 DAY), 'quarterly', 1920.00);
CALL sp_premium_schedule_create(@p4006, 'quarterly', 1920.00, @ps4006);
CALL sp_premium_schedule_generate_installments(@p4006, 1920.00, 'quarterly', DATE_SUB(@today, INTERVAL 150 DAY));
CALL sp_policy_status_transition(@p4006, 'active', @sc);
CALL sp_policy_status_transition(@p4006, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4007', 7, 5, DATE_SUB(@today, INTERVAL 140 DAY), DATE_ADD(@today, INTERVAL 225 DAY), 'semi_annual', @p4007);
CALL sp_policy_status_history_create(@p4007, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4007, @ct2, 32000.00, @cov);
CALL sp_policy_status_transition(@p4007, 'bound', @sc);
CALL sp_policy_update(@p4007, 5, @uw1, DATE_ADD(@today, INTERVAL 225 DAY), 'semi_annual', 1900.00);
CALL sp_premium_schedule_create(@p4007, 'semi_annual', 1900.00, @ps4007);
CALL sp_premium_schedule_generate_installments(@p4007, 1900.00, 'semi_annual', DATE_SUB(@today, INTERVAL 140 DAY));
CALL sp_policy_status_transition(@p4007, 'active', @sc);
CALL sp_policy_status_transition(@p4007, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4008', 8, 6, DATE_SUB(@today, INTERVAL 130 DAY), DATE_ADD(@today, INTERVAL 235 DAY), 'annual',      @p4008);
CALL sp_policy_status_history_create(@p4008, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4008, @ct3, 22000.00, @cov);
CALL sp_policy_status_transition(@p4008, 'bound', @sc);
CALL sp_policy_update(@p4008, 6, @uw2, DATE_ADD(@today, INTERVAL 235 DAY), 'annual', 1180.00);
CALL sp_premium_schedule_create(@p4008, 'annual', 1180.00, @ps4008);
CALL sp_premium_schedule_generate_installments(@p4008, 1180.00, 'annual', DATE_SUB(@today, INTERVAL 130 DAY));
CALL sp_policy_status_transition(@p4008, 'active', @sc);
CALL sp_policy_status_transition(@p4008, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4009', 9, 7, DATE_SUB(@today, INTERVAL 120 DAY), DATE_ADD(@today, INTERVAL 245 DAY), 'monthly',     @p4009);
CALL sp_policy_status_history_create(@p4009, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4009, @ct4, 19500.00, @cov);
CALL sp_policy_status_transition(@p4009, 'bound', @sc);
CALL sp_policy_update(@p4009, 7, @uw3, DATE_ADD(@today, INTERVAL 245 DAY), 'monthly', 900.00);
CALL sp_premium_schedule_create(@p4009, 'monthly', 900.00, @ps4009);
CALL sp_premium_schedule_generate_installments(@p4009, 900.00, 'monthly', DATE_SUB(@today, INTERVAL 120 DAY));
CALL sp_policy_status_transition(@p4009, 'active', @sc);
CALL sp_policy_status_transition(@p4009, 'lapsed', @sc);

CALL sp_policy_create('POL-2025-4010', 10, 8, DATE_SUB(@today, INTERVAL 110 DAY), DATE_ADD(@today, INTERVAL 255 DAY), 'quarterly',  @p4010);
CALL sp_policy_status_history_create(@p4010, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p4010, @ct5, 17500.00, @cov);
CALL sp_policy_status_transition(@p4010, 'bound', @sc);
CALL sp_policy_update(@p4010, 8, @uw4, DATE_ADD(@today, INTERVAL 255 DAY), 'quarterly', 1760.00);
CALL sp_premium_schedule_create(@p4010, 'quarterly', 1760.00, @ps4010);
CALL sp_premium_schedule_generate_installments(@p4010, 1760.00, 'quarterly', DATE_SUB(@today, INTERVAL 110 DAY));
CALL sp_policy_status_transition(@p4010, 'active', @sc);
CALL sp_policy_status_transition(@p4010, 'lapsed', @sc);

-- -------------------------------------------------------------------------------------
-- 5.5 CANCELLED policies (10) - mix of active -> cancelled (7) and
--     active -> lapsed -> cancelled (3), covering both permitted paths to 'cancelled'.
-- -------------------------------------------------------------------------------------

CALL sp_policy_create('POL-2025-5001', 11, 1, DATE_SUB(@today, INTERVAL 90 DAY), DATE_ADD(@today, INTERVAL 275 DAY), 'monthly',     @p5001);
CALL sp_policy_status_history_create(@p5001, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5001, @ct1, 50500.00, @cov);
CALL sp_policy_status_transition(@p5001, 'bound', @sc);
CALL sp_policy_update(@p5001, 1, @uw1, DATE_ADD(@today, INTERVAL 275 DAY), 'monthly', 1080.00);
CALL sp_premium_schedule_create(@p5001, 'monthly', 1080.00, @ps5001);
CALL sp_premium_schedule_generate_installments(@p5001, 1080.00, 'monthly', DATE_SUB(@today, INTERVAL 90 DAY));
CALL sp_policy_status_transition(@p5001, 'active', @sc);
CALL sp_policy_status_transition(@p5001, 'cancelled', @sc);

CALL sp_policy_create('POL-2025-5002', 12, 2, DATE_SUB(@today, INTERVAL 85 DAY), DATE_ADD(@today, INTERVAL 280 DAY), 'quarterly',   @p5002);
CALL sp_policy_status_history_create(@p5002, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5002, @ct2, 40500.00, @cov);
CALL sp_policy_status_transition(@p5002, 'bound', @sc);
CALL sp_policy_update(@p5002, 2, @uw2, DATE_ADD(@today, INTERVAL 280 DAY), 'quarterly', 2080.00);
CALL sp_premium_schedule_create(@p5002, 'quarterly', 2080.00, @ps5002);
CALL sp_premium_schedule_generate_installments(@p5002, 2080.00, 'quarterly', DATE_SUB(@today, INTERVAL 85 DAY));
CALL sp_policy_status_transition(@p5002, 'active', @sc);
CALL sp_policy_status_transition(@p5002, 'cancelled', @sc);

CALL sp_policy_create('POL-2025-5003', 13, 3, DATE_SUB(@today, INTERVAL 80 DAY), DATE_ADD(@today, INTERVAL 285 DAY), 'semi_annual', @p5003);
CALL sp_policy_status_history_create(@p5003, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5003, @ct3, 34500.00, @cov);
CALL sp_policy_status_transition(@p5003, 'bound', @sc);
CALL sp_policy_update(@p5003, 3, @uw3, DATE_ADD(@today, INTERVAL 285 DAY), 'semi_annual', 1950.00);
CALL sp_premium_schedule_create(@p5003, 'semi_annual', 1950.00, @ps5003);
CALL sp_premium_schedule_generate_installments(@p5003, 1950.00, 'semi_annual', DATE_SUB(@today, INTERVAL 80 DAY));
CALL sp_policy_status_transition(@p5003, 'active', @sc);
CALL sp_policy_status_transition(@p5003, 'cancelled', @sc);

CALL sp_policy_create('POL-2025-5004', 14, 4, DATE_SUB(@today, INTERVAL 75 DAY), DATE_ADD(@today, INTERVAL 290 DAY), 'annual',      @p5004);
CALL sp_policy_status_history_create(@p5004, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5004, @ct4, 22500.00, @cov);
CALL sp_policy_status_transition(@p5004, 'bound', @sc);
CALL sp_policy_update(@p5004, 4, @uw4, DATE_ADD(@today, INTERVAL 290 DAY), 'annual', 1150.00);
CALL sp_premium_schedule_create(@p5004, 'annual', 1150.00, @ps5004);
CALL sp_premium_schedule_generate_installments(@p5004, 1150.00, 'annual', DATE_SUB(@today, INTERVAL 75 DAY));
CALL sp_policy_status_transition(@p5004, 'active', @sc);
CALL sp_policy_status_transition(@p5004, 'cancelled', @sc);

CALL sp_policy_create('POL-2025-5005', 15, 5, DATE_SUB(@today, INTERVAL 70 DAY), DATE_ADD(@today, INTERVAL 295 DAY), 'monthly',     @p5005);
CALL sp_policy_status_history_create(@p5005, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5005, @ct5, 15500.00, @cov);
CALL sp_policy_status_transition(@p5005, 'bound', @sc);
CALL sp_policy_update(@p5005, 5, @uw1, DATE_ADD(@today, INTERVAL 295 DAY), 'monthly', 960.00);
CALL sp_premium_schedule_create(@p5005, 'monthly', 960.00, @ps5005);
CALL sp_premium_schedule_generate_installments(@p5005, 960.00, 'monthly', DATE_SUB(@today, INTERVAL 70 DAY));
CALL sp_policy_status_transition(@p5005, 'active', @sc);
CALL sp_policy_status_transition(@p5005, 'cancelled', @sc);

CALL sp_policy_create('POL-2025-5006', 16, 6, DATE_SUB(@today, INTERVAL 65 DAY), DATE_ADD(@today, INTERVAL 300 DAY), 'quarterly',   @p5006);
CALL sp_policy_status_history_create(@p5006, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5006, @ct1, 49000.00, @cov);
CALL sp_policy_status_transition(@p5006, 'bound', @sc);
CALL sp_policy_update(@p5006, 6, @uw2, DATE_ADD(@today, INTERVAL 300 DAY), 'quarterly', 1840.00);
CALL sp_premium_schedule_create(@p5006, 'quarterly', 1840.00, @ps5006);
CALL sp_premium_schedule_generate_installments(@p5006, 1840.00, 'quarterly', DATE_SUB(@today, INTERVAL 65 DAY));
CALL sp_policy_status_transition(@p5006, 'active', @sc);
CALL sp_policy_status_transition(@p5006, 'cancelled', @sc);

CALL sp_policy_create('POL-2025-5007', 17, 7, DATE_SUB(@today, INTERVAL 60 DAY), DATE_ADD(@today, INTERVAL 305 DAY), 'semi_annual', @p5007);
CALL sp_policy_status_history_create(@p5007, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5007, @ct2, 30500.00, @cov);
CALL sp_policy_status_transition(@p5007, 'bound', @sc);
CALL sp_policy_update(@p5007, 7, @uw3, DATE_ADD(@today, INTERVAL 305 DAY), 'semi_annual', 1700.00);
CALL sp_premium_schedule_create(@p5007, 'semi_annual', 1700.00, @ps5007);
CALL sp_premium_schedule_generate_installments(@p5007, 1700.00, 'semi_annual', DATE_SUB(@today, INTERVAL 60 DAY));
CALL sp_policy_status_transition(@p5007, 'active', @sc);
CALL sp_policy_status_transition(@p5007, 'cancelled', @sc);

-- active -> lapsed -> cancelled (3)
CALL sp_policy_create('POL-2025-5008', 18, 8, DATE_SUB(@today, INTERVAL 230 DAY), DATE_ADD(@today, INTERVAL 135 DAY), 'monthly',    @p5008);
CALL sp_policy_status_history_create(@p5008, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5008, @ct3, 21500.00, @cov);
CALL sp_policy_status_transition(@p5008, 'bound', @sc);
CALL sp_policy_update(@p5008, 8, @uw4, DATE_ADD(@today, INTERVAL 135 DAY), 'monthly', 840.00);
CALL sp_premium_schedule_create(@p5008, 'monthly', 840.00, @ps5008);
CALL sp_premium_schedule_generate_installments(@p5008, 840.00, 'monthly', DATE_SUB(@today, INTERVAL 230 DAY));
CALL sp_policy_status_transition(@p5008, 'active', @sc);
CALL sp_policy_status_transition(@p5008, 'lapsed', @sc);
CALL sp_policy_status_transition(@p5008, 'cancelled', @sc);

CALL sp_policy_create('POL-2025-5009', 19, 1, DATE_SUB(@today, INTERVAL 220 DAY), DATE_ADD(@today, INTERVAL 145 DAY), 'quarterly',  @p5009);
CALL sp_policy_status_history_create(@p5009, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5009, @ct4, 18500.00, @cov);
CALL sp_policy_status_transition(@p5009, 'bound', @sc);
CALL sp_policy_update(@p5009, 1, @uw1, DATE_ADD(@today, INTERVAL 145 DAY), 'quarterly', 1600.00);
CALL sp_premium_schedule_create(@p5009, 'quarterly', 1600.00, @ps5009);
CALL sp_premium_schedule_generate_installments(@p5009, 1600.00, 'quarterly', DATE_SUB(@today, INTERVAL 220 DAY));
CALL sp_policy_status_transition(@p5009, 'active', @sc);
CALL sp_policy_status_transition(@p5009, 'lapsed', @sc);
CALL sp_policy_status_transition(@p5009, 'cancelled', @sc);

CALL sp_policy_create('POL-2025-5010', 20, 2, DATE_SUB(@today, INTERVAL 210 DAY), DATE_ADD(@today, INTERVAL 155 DAY), 'semi_annual',@p5010);
CALL sp_policy_status_history_create(@p5010, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p5010, @ct5, 14500.00, @cov);
CALL sp_policy_status_transition(@p5010, 'bound', @sc);
CALL sp_policy_update(@p5010, 2, @uw2, DATE_ADD(@today, INTERVAL 155 DAY), 'semi_annual', 1550.00);
CALL sp_premium_schedule_create(@p5010, 'semi_annual', 1550.00, @ps5010);
CALL sp_premium_schedule_generate_installments(@p5010, 1550.00, 'semi_annual', DATE_SUB(@today, INTERVAL 210 DAY));
CALL sp_policy_status_transition(@p5010, 'active', @sc);
CALL sp_policy_status_transition(@p5010, 'lapsed', @sc);
CALL sp_policy_status_transition(@p5010, 'cancelled', @sc);

-- -------------------------------------------------------------------------------------
-- 5.6 RENEWED policies (10) - quoted -> bound -> active -> renewed
-- -------------------------------------------------------------------------------------

CALL sp_policy_create('POL-2025-6001', 21, 3, DATE_SUB(@today, INTERVAL 370 DAY), DATE_SUB(@today, INTERVAL 5 DAY), 'monthly',     @p6001);
CALL sp_policy_status_history_create(@p6001, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6001, @ct1, 53000.00, @cov);
CALL sp_policy_status_transition(@p6001, 'bound', @sc);
CALL sp_policy_update(@p6001, 3, @uw3, DATE_SUB(@today, INTERVAL 5 DAY), 'monthly', 1260.00);
CALL sp_premium_schedule_create(@p6001, 'monthly', 1260.00, @ps6001);
CALL sp_premium_schedule_generate_installments(@p6001, 1260.00, 'monthly', DATE_SUB(@today, INTERVAL 370 DAY));
CALL sp_policy_status_transition(@p6001, 'active', @sc);
CALL sp_policy_status_transition(@p6001, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6002', 22, 4, DATE_SUB(@today, INTERVAL 368 DAY), DATE_SUB(@today, INTERVAL 3 DAY), 'quarterly',   @p6002);
CALL sp_policy_status_history_create(@p6002, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6002, @ct2, 43000.00, @cov);
CALL sp_policy_status_transition(@p6002, 'bound', @sc);
CALL sp_policy_update(@p6002, 4, @uw4, DATE_SUB(@today, INTERVAL 3 DAY), 'quarterly', 2160.00);
CALL sp_premium_schedule_create(@p6002, 'quarterly', 2160.00, @ps6002);
CALL sp_premium_schedule_generate_installments(@p6002, 2160.00, 'quarterly', DATE_SUB(@today, INTERVAL 368 DAY));
CALL sp_policy_status_transition(@p6002, 'active', @sc);
CALL sp_policy_status_transition(@p6002, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6003', 23, 5, DATE_SUB(@today, INTERVAL 366 DAY), DATE_SUB(@today, INTERVAL 1 DAY), 'semi_annual', @p6003);
CALL sp_policy_status_history_create(@p6003, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6003, @ct3, 38000.00, @cov);
CALL sp_policy_status_transition(@p6003, 'bound', @sc);
CALL sp_policy_update(@p6003, 5, @uw1, DATE_SUB(@today, INTERVAL 1 DAY), 'semi_annual', 2000.00);
CALL sp_premium_schedule_create(@p6003, 'semi_annual', 2000.00, @ps6003);
CALL sp_premium_schedule_generate_installments(@p6003, 2000.00, 'semi_annual', DATE_SUB(@today, INTERVAL 366 DAY));
CALL sp_policy_status_transition(@p6003, 'active', @sc);
CALL sp_policy_status_transition(@p6003, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6004', 24, 6, DATE_SUB(@today, INTERVAL 400 DAY), DATE_SUB(@today, INTERVAL 35 DAY), 'annual',     @p6004);
CALL sp_policy_status_history_create(@p6004, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6004, @ct4, 25500.00, @cov);
CALL sp_policy_status_transition(@p6004, 'bound', @sc);
CALL sp_policy_update(@p6004, 6, @uw2, DATE_SUB(@today, INTERVAL 35 DAY), 'annual', 1350.00);
CALL sp_premium_schedule_create(@p6004, 'annual', 1350.00, @ps6004);
CALL sp_premium_schedule_generate_installments(@p6004, 1350.00, 'annual', DATE_SUB(@today, INTERVAL 400 DAY));
CALL sp_policy_status_transition(@p6004, 'active', @sc);
CALL sp_policy_status_transition(@p6004, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6005', 25, 7, DATE_SUB(@today, INTERVAL 395 DAY), DATE_SUB(@today, INTERVAL 30 DAY), 'monthly',    @p6005);
CALL sp_policy_status_history_create(@p6005, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6005, @ct5, 17800.00, @cov);
CALL sp_policy_status_transition(@p6005, 'bound', @sc);
CALL sp_policy_update(@p6005, 7, @uw3, DATE_SUB(@today, INTERVAL 30 DAY), 'monthly', 1140.00);
CALL sp_premium_schedule_create(@p6005, 'monthly', 1140.00, @ps6005);
CALL sp_premium_schedule_generate_installments(@p6005, 1140.00, 'monthly', DATE_SUB(@today, INTERVAL 395 DAY));
CALL sp_policy_status_transition(@p6005, 'active', @sc);
CALL sp_policy_status_transition(@p6005, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6006', 26, 8, DATE_SUB(@today, INTERVAL 390 DAY), DATE_SUB(@today, INTERVAL 25 DAY), 'quarterly',  @p6006);
CALL sp_policy_status_history_create(@p6006, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6006, @ct1, 48500.00, @cov);
CALL sp_policy_status_transition(@p6006, 'bound', @sc);
CALL sp_policy_update(@p6006, 8, @uw4, DATE_SUB(@today, INTERVAL 25 DAY), 'quarterly', 1960.00);
CALL sp_premium_schedule_create(@p6006, 'quarterly', 1960.00, @ps6006);
CALL sp_premium_schedule_generate_installments(@p6006, 1960.00, 'quarterly', DATE_SUB(@today, INTERVAL 390 DAY));
CALL sp_policy_status_transition(@p6006, 'active', @sc);
CALL sp_policy_status_transition(@p6006, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6007', 27, 1, DATE_SUB(@today, INTERVAL 385 DAY), DATE_SUB(@today, INTERVAL 20 DAY), 'semi_annual',@p6007);
CALL sp_policy_status_history_create(@p6007, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6007, @ct2, 33500.00, @cov);
CALL sp_policy_status_transition(@p6007, 'bound', @sc);
CALL sp_policy_update(@p6007, 1, @uw1, DATE_SUB(@today, INTERVAL 20 DAY), 'semi_annual', 1850.00);
CALL sp_premium_schedule_create(@p6007, 'semi_annual', 1850.00, @ps6007);
CALL sp_premium_schedule_generate_installments(@p6007, 1850.00, 'semi_annual', DATE_SUB(@today, INTERVAL 385 DAY));
CALL sp_policy_status_transition(@p6007, 'active', @sc);
CALL sp_policy_status_transition(@p6007, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6008', 28, 2, DATE_SUB(@today, INTERVAL 380 DAY), DATE_SUB(@today, INTERVAL 15 DAY), 'annual',     @p6008);
CALL sp_policy_status_history_create(@p6008, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6008, @ct3, 26500.00, @cov);
CALL sp_policy_status_transition(@p6008, 'bound', @sc);
CALL sp_policy_update(@p6008, 2, @uw2, DATE_SUB(@today, INTERVAL 15 DAY), 'annual', 1280.00);
CALL sp_premium_schedule_create(@p6008, 'annual', 1280.00, @ps6008);
CALL sp_premium_schedule_generate_installments(@p6008, 1280.00, 'annual', DATE_SUB(@today, INTERVAL 380 DAY));
CALL sp_policy_status_transition(@p6008, 'active', @sc);
CALL sp_policy_status_transition(@p6008, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6009', 29, 3, DATE_SUB(@today, INTERVAL 375 DAY), DATE_SUB(@today, INTERVAL 10 DAY), 'monthly',    @p6009);
CALL sp_policy_status_history_create(@p6009, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6009, @ct4, 20500.00, @cov);
CALL sp_policy_status_transition(@p6009, 'bound', @sc);
CALL sp_policy_update(@p6009, 3, @uw3, DATE_SUB(@today, INTERVAL 10 DAY), 'monthly', 1020.00);
CALL sp_premium_schedule_create(@p6009, 'monthly', 1020.00, @ps6009);
CALL sp_premium_schedule_generate_installments(@p6009, 1020.00, 'monthly', DATE_SUB(@today, INTERVAL 375 DAY));
CALL sp_policy_status_transition(@p6009, 'active', @sc);
CALL sp_policy_status_transition(@p6009, 'renewed', @sc);

CALL sp_policy_create('POL-2025-6010', 30, 4, DATE_SUB(@today, INTERVAL 372 DAY), DATE_SUB(@today, INTERVAL 7 DAY), 'quarterly',   @p6010);
CALL sp_policy_status_history_create(@p6010, NULL, 'quoted', @hist);
CALL sp_coverage_create(@p6010, @ct5, 19800.00, @cov);
CALL sp_policy_status_transition(@p6010, 'bound', @sc);
CALL sp_policy_update(@p6010, 4, @uw4, DATE_SUB(@today, INTERVAL 7 DAY), 'quarterly', 1920.00);
CALL sp_premium_schedule_create(@p6010, 'quarterly', 1920.00, @ps6010);
CALL sp_premium_schedule_generate_installments(@p6010, 1920.00, 'quarterly', DATE_SUB(@today, INTERVAL 372 DAY));
CALL sp_policy_status_transition(@p6010, 'active', @sc);
CALL sp_policy_status_transition(@p6010, 'renewed', @sc);

-- =====================================================================================
-- SECTION 6: Payments - via sp_payment_apply (oldest-unpaid-first allocation is
-- guaranteed correct by the procedure itself). Covers 20+ Policies across
-- bound/active/lapsed/cancelled/renewed statuses, with a realistic mix of partial
-- and full installment payoffs.
-- =====================================================================================

-- Bound policies (2001-2010): first installment paid in full for most, partial for a few
CALL sp_payment_apply(@p2001, 100.00,  DATE_SUB(@today, INTERVAL 15 DAY), @pay, @psc);
CALL sp_payment_apply(@p2002, 500.00,  DATE_SUB(@today, INTERVAL 13 DAY), @pay, @psc);
CALL sp_payment_apply(@p2003, 300.00,  DATE_SUB(@today, INTERVAL 10 DAY), @pay, @psc);
CALL sp_payment_apply(@p2004, 1000.00, DATE_SUB(@today, INTERVAL 8 DAY),  @pay, @psc);
CALL sp_payment_apply(@p2005, 50.00,   DATE_SUB(@today, INTERVAL 5 DAY),  @pay, @psc);
CALL sp_payment_apply(@p2006, 360.00,  DATE_SUB(@today, INTERVAL 3 DAY),  @pay, @psc);
CALL sp_payment_apply(@p2007, 250.00,  DATE_SUB(@today, INTERVAL 2 DAY),  @pay, @psc);
CALL sp_payment_apply(@p2008, 1100.00, DATE_SUB(@today, INTERVAL 1 DAY),  @pay, @psc);

-- Active policies (3001-3010): first installment(s) paid, some policies get two
-- payments to demonstrate sequential oldest-unpaid-first allocation.
CALL sp_payment_apply(@p3001, 100.00,  DATE_SUB(@today, INTERVAL 58 DAY), @pay, @psc);
CALL sp_payment_apply(@p3001, 100.00,  DATE_SUB(@today, INTERVAL 28 DAY), @pay, @psc);
CALL sp_payment_apply(@p3002, 600.00,  DATE_SUB(@today, INTERVAL 50 DAY), @pay, @psc);
CALL sp_payment_apply(@p3003, 1100.00, DATE_SUB(@today, INTERVAL 45 DAY), @pay, @psc);
CALL sp_payment_apply(@p3004, 1300.00, DATE_SUB(@today, INTERVAL 40 DAY), @pay, @psc);
-- 3005/3006/3007 (expiring soon) deliberately left with NO payment -> their first
-- installment, due near term_start_date many months ago, remains overdue+unpaid,
-- exercising the overdue-payments report.
CALL sp_payment_apply(@p3008, 80.00,   DATE_SUB(@today, INTERVAL 35 DAY), @pay, @psc);
CALL sp_payment_apply(@p3009, 420.00,  DATE_SUB(@today, INTERVAL 30 DAY), @pay, @psc);
CALL sp_payment_apply(@p3010, 1450.00, DATE_SUB(@today, INTERVAL 25 DAY), @pay, @psc);

-- Lapsed policies (4001-4010): partial payment history before lapsing (payment dates
-- predate the lapse so the sequence is chronologically plausible)
CALL sp_payment_apply(@p4001, 95.00,   DATE_SUB(@today, INTERVAL 195 DAY), @pay, @psc);
CALL sp_payment_apply(@p4002, 570.00,  DATE_SUB(@today, INTERVAL 185 DAY), @pay, @psc);
CALL sp_payment_apply(@p4003, 1050.00, DATE_SUB(@today, INTERVAL 175 DAY), @pay, @psc);
CALL sp_payment_apply(@p4004, 1250.00, DATE_SUB(@today, INTERVAL 165 DAY), @pay, @psc);

-- Cancelled policies (5001-5010): one payment made before cancellation
CALL sp_payment_apply(@p5001, 90.00,   DATE_SUB(@today, INTERVAL 85 DAY), @pay, @psc);
CALL sp_payment_apply(@p5002, 520.00,  DATE_SUB(@today, INTERVAL 80 DAY), @pay, @psc);
CALL sp_payment_apply(@p5003, 975.00,  DATE_SUB(@today, INTERVAL 75 DAY), @pay, @psc);

-- Renewed policies (6001-6010): full payment history over the renewed term
CALL sp_payment_apply(@p6001, 1260.00, DATE_SUB(@today, INTERVAL 360 DAY), @pay, @psc);
CALL sp_payment_apply(@p6002, 2160.00, DATE_SUB(@today, INTERVAL 358 DAY), @pay, @psc);
CALL sp_payment_apply(@p6003, 1000.00, DATE_SUB(@today, INTERVAL 356 DAY), @pay, @psc);
CALL sp_payment_apply(@p6004, 1350.00, DATE_SUB(@today, INTERVAL 390 DAY), @pay, @psc);

-- =====================================================================================
-- SECTION 7: Beneficiaries - via sp_beneficiary_create, covering 15+ Policies.
-- Allocation percentages per Policy never exceed 100.
-- =====================================================================================

CALL sp_beneficiary_create(@p2001, 'Nina Nguyen',    'Spouse', 60.00, @ben);
CALL sp_beneficiary_create(@p2001, 'Owen Nguyen',    'Child',  40.00, @ben);

CALL sp_beneficiary_create(@p2002, 'Paula Johnson',  'Spouse', 100.00, @ben);

CALL sp_beneficiary_create(@p2003, 'Quincy Smith',   'Child',  50.00, @ben);
CALL sp_beneficiary_create(@p2003, 'Rhea Smith',     'Child',  50.00, @ben);

CALL sp_beneficiary_create(@p3001, 'Sara Perez',     'Spouse', 70.00, @ben);
CALL sp_beneficiary_create(@p3001, 'Tomas Perez',    'Sibling',30.00, @ben);

CALL sp_beneficiary_create(@p3002, 'Uma Thompson',   'Parent', 100.00, @ben);

CALL sp_beneficiary_create(@p3003, 'Victor White',   'Spouse', 55.00, @ben);
CALL sp_beneficiary_create(@p3003, 'Wendy White',    'Child',  45.00, @ben);

CALL sp_beneficiary_create(@p3004, 'Xander Harris',  'Child',  100.00, @ben);

CALL sp_beneficiary_create(@p3005, 'Yolanda Sanchez','Spouse', 80.00, @ben);
CALL sp_beneficiary_create(@p3005, 'Zach Sanchez',   'Other',  20.00, @ben);

CALL sp_beneficiary_create(@p4001, 'Amy Clark',      'Spouse', 100.00, @ben);

CALL sp_beneficiary_create(@p4002, 'Ben Ramirez',    'Child',  60.00, @ben);
CALL sp_beneficiary_create(@p4002, 'Cara Ramirez',   'Child',  40.00, @ben);

CALL sp_beneficiary_create(@p5001, 'Diego Lewis',    'Parent', 100.00, @ben);

CALL sp_beneficiary_create(@p5002, 'Ella Robinson',  'Spouse', 75.00, @ben);
CALL sp_beneficiary_create(@p5002, 'Finn Robinson',  'Sibling',25.00, @ben);

CALL sp_beneficiary_create(@p6001, 'Gina Nguyen',    'Spouse', 100.00, @ben);

CALL sp_beneficiary_create(@p6002, 'Hank Johnson',   'Child',  50.00, @ben);
CALL sp_beneficiary_create(@p6002, 'Iris Johnson',   'Child',  50.00, @ben);

CALL sp_beneficiary_create(@p6003, 'Jorge Smith',    'Other',  100.00, @ben);

-- =====================================================================================
-- SECTION 8: Endorsements + Premium_Adjustment_History - covering 10+ Policies.
--
-- For currently-'active' policies, sp_endorsement_calculate_proration is called to
-- compute a guaranteed-correct prorated adjustment, which is then recorded via
-- sp_endorsement_create and a direct INSERT into premium_adjustment_history (no CRUD
-- procedure exists for premium_adjustment_history, so it is populated directly here,
-- consistent with the computed previous/new premium values on the Policy).
--
-- For policies that are now lapsed/cancelled/renewed, the endorsement is seeded as a
-- historical fact (it was created while the policy was still active) using a prorated
-- amount computed by hand with the same ROUND(delta * daysRemaining / totalTermDays, 2)
-- formula sp_endorsement_calculate_proration implements, since the procedure itself
-- only accepts currently-active policies.
-- =====================================================================================

-- -- Active policies: use sp_endorsement_calculate_proration directly --

-- p3001: +$5,000 coverage increase, effective 20 days ago
CALL sp_endorsement_calculate_proration(@p3001, 150.00, DATE_SUB(@today, INTERVAL 20 DAY), @proration1, @esc1);
CALL sp_endorsement_create(@p3001, 'Increase liability coverage limit by $5,000', DATE_SUB(@today, INTERVAL 20 DAY), @proration1, @end1);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p3001, @end1, 1200.00, 1200.00 + @proration1, @proration1, DATE_SUB(@today, INTERVAL 20 DAY));
CALL sp_policy_update(@p3001, 5, @uw1, DATE_ADD(@today, INTERVAL 305 DAY), 'monthly', 1200.00 + @proration1);

-- p3002: -$2,000 coverage decrease (removal), effective 15 days ago
CALL sp_endorsement_calculate_proration(@p3002, -80.00, DATE_SUB(@today, INTERVAL 15 DAY), @proration2, @esc2);
CALL sp_endorsement_create(@p3002, 'Remove collision coverage rider', DATE_SUB(@today, INTERVAL 15 DAY), @proration2, @end2);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p3002, @end2, 2400.00, 2400.00 + @proration2, @proration2, DATE_SUB(@today, INTERVAL 15 DAY));
CALL sp_policy_update(@p3002, 6, @uw2, DATE_ADD(@today, INTERVAL 310 DAY), 'quarterly', 2400.00 + @proration2);

-- p3003: +$3,000 coverage increase, effective 10 days ago
CALL sp_endorsement_calculate_proration(@p3003, 120.00, DATE_SUB(@today, INTERVAL 10 DAY), @proration3, @esc3);
CALL sp_endorsement_create(@p3003, 'Add comprehensive coverage endorsement', DATE_SUB(@today, INTERVAL 10 DAY), @proration3, @end3);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p3003, @end3, 2200.00, 2200.00 + @proration3, @proration3, DATE_SUB(@today, INTERVAL 10 DAY));
CALL sp_policy_update(@p3003, 7, @uw3, DATE_ADD(@today, INTERVAL 315 DAY), 'semi_annual', 2200.00 + @proration3);

-- p3004: +$1,500 coverage increase, effective 5 days ago
CALL sp_endorsement_calculate_proration(@p3004, 90.00, DATE_SUB(@today, INTERVAL 5 DAY), @proration4, @esc4);
CALL sp_endorsement_create(@p3004, 'Increase medical payments coverage limit', DATE_SUB(@today, INTERVAL 5 DAY), @proration4, @end4);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p3004, @end4, 1300.00, 1300.00 + @proration4, @proration4, DATE_SUB(@today, INTERVAL 5 DAY));
CALL sp_policy_update(@p3004, 8, @uw4, DATE_ADD(@today, INTERVAL 320 DAY), 'annual', 1300.00 + @proration4);

-- p3008: -$1,000 coverage decrease, effective 12 days ago
CALL sp_endorsement_calculate_proration(@p3008, -40.00, DATE_SUB(@today, INTERVAL 12 DAY), @proration5, @esc5);
CALL sp_endorsement_create(@p3008, 'Reduce comprehensive coverage limit', DATE_SUB(@today, INTERVAL 12 DAY), @proration5, @end5);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p3008, @end5, 960.00, 960.00 + @proration5, @proration5, DATE_SUB(@today, INTERVAL 12 DAY));
CALL sp_policy_update(@p3008, 4, @uw4, DATE_ADD(@today, INTERVAL 325 DAY), 'monthly', 960.00 + @proration5);

-- p3009: +$4,000 coverage increase, effective 8 days ago
CALL sp_endorsement_calculate_proration(@p3009, 200.00, DATE_SUB(@today, INTERVAL 8 DAY), @proration6, @esc6);
CALL sp_endorsement_create(@p3009, 'Add uninsured motorist coverage endorsement', DATE_SUB(@today, INTERVAL 8 DAY), @proration6, @end6);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p3009, @end6, 1680.00, 1680.00 + @proration6, @proration6, DATE_SUB(@today, INTERVAL 8 DAY));
CALL sp_policy_update(@p3009, 5, @uw1, DATE_ADD(@today, INTERVAL 330 DAY), 'quarterly', 1680.00 + @proration6);

-- -- Historical endorsements on since-lapsed/cancelled/renewed policies: seeded as --
-- -- historical facts with a hand-computed proration consistent with the formula.  --

-- p4001 (now lapsed): was active 199..1 days ago (term 200..165 days); endorsement
-- effective 150 days ago, +$2,500 coverage increase ($100.00 annualized delta)
-- daysRemaining = DATEDIFF('term_end_date', '150 days ago'), totalTermDays = DATEDIFF(term_end, term_start)
SET @p4001_days_remaining = DATEDIFF(DATE_ADD(@today, INTERVAL 165 DAY), DATE_SUB(@today, INTERVAL 150 DAY));
SET @p4001_total_term     = DATEDIFF(DATE_ADD(@today, INTERVAL 165 DAY), DATE_SUB(@today, INTERVAL 200 DAY));
SET @proration7 = ROUND(100.00 * @p4001_days_remaining / @p4001_total_term, 2);
CALL sp_endorsement_create(@p4001, 'Increase liability coverage limit by $2,500 (historical)', DATE_SUB(@today, INTERVAL 150 DAY), @proration7, @end7);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p4001, @end7, 1140.00, 1140.00 + @proration7, @proration7, DATE_SUB(@today, INTERVAL 150 DAY));

-- p5001 (now cancelled): was active 89..1 days ago (term 90..-275 i.e. 90 days ago to
-- 275 days in future); endorsement effective 70 days ago, -$1,500 ($60.00 annualized delta)
SET @p5001_days_remaining = DATEDIFF(DATE_ADD(@today, INTERVAL 275 DAY), DATE_SUB(@today, INTERVAL 70 DAY));
SET @p5001_total_term     = DATEDIFF(DATE_ADD(@today, INTERVAL 275 DAY), DATE_SUB(@today, INTERVAL 90 DAY));
SET @proration8 = ROUND(-60.00 * @p5001_days_remaining / @p5001_total_term, 2);
CALL sp_endorsement_create(@p5001, 'Reduce liability coverage limit by $1,500 (historical)', DATE_SUB(@today, INTERVAL 70 DAY), @proration8, @end8);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p5001, @end8, 1080.00, 1080.00 + @proration8, @proration8, DATE_SUB(@today, INTERVAL 70 DAY));

-- p6001 (now renewed): was active for its full term; endorsement effective 300 days
-- ago, +$3,500 ($140.00 annualized delta)
SET @p6001_days_remaining = DATEDIFF(DATE_SUB(@today, INTERVAL 5 DAY), DATE_SUB(@today, INTERVAL 300 DAY));
SET @p6001_total_term     = DATEDIFF(DATE_SUB(@today, INTERVAL 5 DAY), DATE_SUB(@today, INTERVAL 370 DAY));
SET @proration9 = ROUND(140.00 * @p6001_days_remaining / @p6001_total_term, 2);
CALL sp_endorsement_create(@p6001, 'Increase comprehensive coverage limit by $3,500 (historical)', DATE_SUB(@today, INTERVAL 300 DAY), @proration9, @end9);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p6001, @end9, 1260.00, 1260.00 + @proration9, @proration9, DATE_SUB(@today, INTERVAL 300 DAY));

-- p6002 (now renewed): endorsement effective 280 days ago, -$1,000 ($40.00 annualized delta)
SET @p6002_days_remaining = DATEDIFF(DATE_SUB(@today, INTERVAL 3 DAY), DATE_SUB(@today, INTERVAL 280 DAY));
SET @p6002_total_term     = DATEDIFF(DATE_SUB(@today, INTERVAL 3 DAY), DATE_SUB(@today, INTERVAL 368 DAY));
SET @proration10 = ROUND(-40.00 * @p6002_days_remaining / @p6002_total_term, 2);
CALL sp_endorsement_create(@p6002, 'Reduce medical payments coverage limit (historical)', DATE_SUB(@today, INTERVAL 280 DAY), @proration10, @end10);
INSERT INTO premium_adjustment_history (policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount, adjusted_at)
VALUES (@p6002, @end10, 2160.00, 2160.00 + @proration10, @proration10, DATE_SUB(@today, INTERVAL 280 DAY));

-- =====================================================================================
-- SECTION 9: Renewal notices - via sp_renewal_notice_generate, exercising the
-- renewal-notice generation endpoint against the expiring-soon active Policies.
-- =====================================================================================

CALL sp_renewal_notice_generate(@p3005, 30, @rn1, @rnsc1);
CALL sp_renewal_notice_generate(@p3006, 30, @rn2, @rnsc2);
CALL sp_renewal_notice_generate(@p3007, 30, @rn3, @rnsc3);
