# Insurance Policy Administration System

A standalone, MEDIUM-tier **legacy-style** insurance policy administration backend. It manages the
full policy lifecycle (quote → bind → active → endorsement → payment → lapse/cancel/renew) for
customers, agents, and underwriters, exposed as a JSON REST API.

> **This project is a modernization test fixture.** It deliberately contains a mix of persistence
> technologies and several *intentional* legacy "smells" so that modernization tooling and reviewers
> have realistic patterns to detect and refactor. The smells are marked in code and listed in the
> [Intentional Legacy Smells](#intentional-legacy-smells) section below — they are **by design**, not
> accidental defects.

---

## Tech Stack

| Concern              | Technology                                                    | Version        |
|----------------------|---------------------------------------------------------------|----------------|
| Language / runtime   | Java (JDK)                                                     | 8              |
| Web framework        | Spring MVC (XML + annotations, **not** Spring Boot)           | 5.3.31         |
| ORM (subset of DAOs) | Hibernate ORM                                                 | 5.6.15.Final   |
| JDBC / stored procs  | Spring JDBC (`JdbcTemplate` / `SimpleJdbcCall`)               | 5.3.31         |
| Cross-manager tx     | Spring `ChainedTransactionManager` (spring-data-commons)      | 2.7.18         |
| Database             | MySQL                                                          | 8.x            |
| Connection pool      | Apache Commons DBCP2                                           | 2.9.0          |
| JSON                 | Jackson (databind + jsr310)                                   | 2.15.2         |
| Logging              | SLF4J + Logback                                               | 1.7.36 / 1.2.12|
| Packaging            | WAR on servlet container                                      | —              |
| Servlet container    | Apache Tomcat                                                 | 9.x            |
| Testing              | JUnit 4, Mockito, jqwik (property-based)                      | 4.13.2 / 4.11.0 / 1.7.4 |

Build output is a WAR (`<packaging>war</packaging>`, `<finalName>insurance-policy-admin</finalName>`).

---

## Architecture

Classic layered design: **Controller → Service → DAO → Database**.

```
HTTP (JSON)
  │
  ▼
Controller            @RestController-style controllers under com.insurancepolicyadmin.controller
  │                   DispatcherServlet mapped to /api/*
  ▼
Service               Interfaces + *Impl under com.insurancepolicyadmin.service(.impl)
  │                   All business rules, validation, orchestration, @Transactional boundaries
  ▼
DAO (two families)    Technology-neutral interfaces under com.insurancepolicyadmin.dao
  │                   ├─ Hibernate impls  (com.insurancepolicyadmin.dao.hibernate)
  │                   └─ Spring-JDBC impls (com.insurancepolicyadmin.dao.jdbc) → stored procedures
  ▼
MySQL 8               17 tables + 100 stored procedures
```

### Deliberate mixed-persistence split

The DAO layer is intentionally split across **two different persistence technologies** sharing a
single `DataSource`:

- **Hibernate DAOs** (`SessionFactory` / `Session` / JPA `CriteriaBuilder`) back the
  **Customer, Agent, and Beneficiary** aggregates. These are the only JPA-mapped entities
  (`model.entity`).
- **Spring-JDBC + stored-procedure DAOs** back **everything else** — Policy lifecycle, Endorsement,
  Premium/Installment, Payment, Underwriter, and all Reporting. These call the **100 MySQL stored
  procedures** via `SimpleJdbcCall` / `CallableStatement`. Their domain objects are plain
  (non-JPA) classes under `model.plain`.

Because a single service method can touch *both* families (e.g. issuing a policy also validates a
Hibernate-managed Customer/Agent), transactions that span both DAO families are wrapped in a
**`ChainedTransactionManager`** that composes the `HibernateTransactionManager` and the
`DataSourceTransactionManager`. Single-family methods use their own dedicated transaction manager.

> This mixed model is one of the fixture's defining characteristics — a realistic "half-migrated"
> legacy codebase where part of the data access was moved to an ORM and part was left on stored
> procedures.

---

## Intentional Legacy Smells

Each item below is **deliberate** and marked in the source. A modernization tool/reviewer should
expect to find and (optionally) refactor these:

| Smell                                      | Location                                                                 | Marker / notes |
|--------------------------------------------|--------------------------------------------------------------------------|----------------|
| Business logic embedded in a controller    | `PaymentController` — overpayment amount check before delegating         | `// INTENTIONAL LAYERING VIOLATION` above the check |
| Business logic embedded in a controller    | `ReportController` — renewal lookahead window (1–365) range validation   | `// INTENTIONAL LAYERING VIOLATION` above the check |
| Tenant/segment branching utility           | `CustomerSegmentRouter` — branches allocation strictness & payment grace by `Customer.segmentCode` (`STANDARD` / `PREFERRED` / `HIGH_RISK`) | `// INTENTIONAL TENANT BRANCHING` above the branching logic |
| Static utility class                       | `ValidationUtils` — static `isValidEmail` / `isValidPhone` / `isNotFutureDate` | called from Customer/Beneficiary/Payment services |
| Checked-exception collapsing               | `ReportServiceImpl.exportBookOfBusinessReport` — wraps `IOException`, `ParseException`, `javax.mail.MessagingException` in one try block, each rethrown as unchecked `ReportExportException` | three distinct checked exceptions flattened |
| Mutable JavaBean DTO                        | `PolicySearchCriteria` — public getters/setters, no validation, no `final` fields, no defensive copying | plain mutable bean |

---

## Prerequisites

- **Java 8 JDK** (`JAVA_HOME` pointing at a JDK 8 install)
- **Apache Maven** 3.6+
- **MySQL 8.x** server
- **Apache Tomcat 9.x**

---

## Setup

### 1. Create the database and application user

The stored procedures use `DELIMITER`, so each `.sql` file **must be run through the `mysql`
client** (do not split or paste statement-by-statement through a tool that strips `DELIMITER`).

```sql
-- as a MySQL admin (e.g. root)
CREATE DATABASE insurance_policy_admin
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'ipa_app'@'%' IDENTIFIED BY 'IpaApp_2026!';
GRANT ALL PRIVILEGES ON insurance_policy_admin.* TO 'ipa_app'@'%';
FLUSH PRIVILEGES;
```

**Authentication plugin note (MySQL 8):** MySQL 8 defaults new users to `caching_sha2_password`,
which fails over a non-SSL JDBC connection unless the driver is allowed to retrieve the public key.
You have two options:

- **Option A (recommended for this fixture):** keep `allowPublicKeyRetrieval=true` in the JDBC URL
  (already set — see step 3). This lets `caching_sha2_password` work over the non-SSL connection.
- **Option B:** switch the app user to the legacy plugin:

  ```sql
  ALTER USER 'ipa_app'@'%' IDENTIFIED WITH mysql_native_password BY 'IpaApp_2026!';
  FLUSH PRIVILEGES;
  ```

### 2. Load schema, procedures, and seed data (strict order)

Run these in order. **Procedures must be loaded after the schema and before the data.** Load all six
`procedures_*.sql` files (order among the six does not matter, but all must precede `data.sql`):

```bash
cd insurance-policy-admin

# 1) schema (17 tables)
mysql -u ipa_app -p insurance_policy_admin < src/main/resources/db/schema.sql

# 2) stored procedures (100 procedures across 6 files)
mysql -u ipa_app -p insurance_policy_admin < src/main/resources/db/procedures_customer_agent_underwriter.sql
mysql -u ipa_app -p insurance_policy_admin < src/main/resources/db/procedures_policy_coverage.sql
mysql -u ipa_app -p insurance_policy_admin < src/main/resources/db/procedures_endorsement_premium.sql
mysql -u ipa_app -p insurance_policy_admin < src/main/resources/db/procedures_payment.sql
mysql -u ipa_app -p insurance_policy_admin < src/main/resources/db/procedures_status_history.sql
mysql -u ipa_app -p insurance_policy_admin < src/main/resources/db/procedures_reporting.sql

# 3) seed data
mysql -u ipa_app -p insurance_policy_admin < src/main/resources/db/data.sql
```

Verify the procedure count is 100:

```sql
SELECT COUNT(*) FROM information_schema.routines
WHERE routine_schema = 'insurance_policy_admin' AND routine_type = 'PROCEDURE';
-- expect: 100
```

### 3. Configure `jdbc.properties`

Edit `src/main/resources/jdbc.properties`:

```properties
jdbc.driverClassName=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/insurance_policy_admin?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
jdbc.username=ipa_app
jdbc.password=IpaApp_2026!
```

Keep `useSSL=false&allowPublicKeyRetrieval=true` for a `caching_sha2_password` user over a non-SSL
connection (Option A above). If you chose `mysql_native_password` (Option B), the URL still works
unchanged.

### 4. Build

```bash
mvn clean package
# → target/insurance-policy-admin.war
```

(Use `mvn clean package -DskipTests` to skip the test phase.)

### 5. Deploy to Tomcat 9

```bash
cp target/insurance-policy-admin.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh          # or startup.bat on Windows
```

Tomcat expands the WAR to context path `/insurance-policy-admin`, and the DispatcherServlet is mapped
to `/api/*`, so the API base URL is:

```
http://<host>:<port>/insurance-policy-admin/api
```

> **Port note:** the base URL port is whatever Tomcat's HTTP connector is configured to use. The
> stock Tomcat 9 default is **8080**, giving
> `http://localhost:8080/insurance-policy-admin/api`. This fixture was verified end-to-end on a
> connector configured for **8081** (`http://<host>:8081/insurance-policy-admin/api`). Adjust the
> port in the examples below to match your `conf/server.xml`.

---

## REST API Reference

All paths below are relative to the API base URL
(`http://<host>:<port>/insurance-policy-admin/api`). Examples assume base
`http://localhost:8080/insurance-policy-admin/api`.

### Customers

| Method | Path                     | Description                                   |
|--------|--------------------------|-----------------------------------------------|
| POST   | `/customers`             | Create a customer (validates email/phone)     |
| GET    | `/customers/{id}`        | Get a customer by id                          |
| GET    | `/customers/search`      | Search customers (requires ≥1 criterion)      |

### Agents

| Method | Path                | Description                               |
|--------|---------------------|-------------------------------------------|
| POST   | `/agents`           | Create an agent (unique license number)   |
| GET    | `/agents/{id}`      | Get an agent by id                        |
| PUT    | `/agents/{id}`      | Update an agent                           |
| DELETE | `/agents/{id}`      | Delete an agent                           |

### Underwriters

| Method | Path                     | Description                 |
|--------|--------------------------|-----------------------------|
| POST   | `/underwriters`          | Create an underwriter       |
| GET    | `/underwriters/{id}`     | Get an underwriter by id    |

### Policies

| Method | Path                                   | Description                                              |
|--------|----------------------------------------|----------------------------------------------------------|
| POST   | `/policies`                            | Issue a policy (1–50 coverages; starts in `quoted`)      |
| GET    | `/policies`                            | Search policies (AND of optional query params)           |
| GET    | `/policies/{id}`                       | Get a policy by id                                       |
| GET    | `/policies/by-number/{policyNumber}`   | Get a policy by its policy number                        |
| POST   | `/policies/{id}/bind`                  | Bind a quoted policy (active underwriter + schedule gen) |
| POST   | `/policies/{id}/status-transitions`    | Transition policy status (validated transition matrix)   |
| GET    | `/policies/{id}/status-history`        | Status history (ascending by timestamp)                  |

### Beneficiaries

| Method | Path                               | Description                                      |
|--------|------------------------------------|--------------------------------------------------|
| POST   | `/policies/{id}/beneficiaries`     | Add a beneficiary (allocation sum ≤ 100%)        |
| GET    | `/policies/{id}/beneficiaries`     | List beneficiaries for a policy                  |
| PUT    | `/beneficiaries/{beneficiaryId}`   | Update a beneficiary                             |
| DELETE | `/beneficiaries/{beneficiaryId}`   | Delete a beneficiary                             |

### Endorsements

| Method | Path                               | Description                                                   |
|--------|------------------------------------|---------------------------------------------------------------|
| POST   | `/policies/{id}/endorsements`      | Apply an endorsement to an `active` policy (prorated premium) |

### Payments

| Method | Path                           | Description                                               |
|--------|--------------------------------|-----------------------------------------------------------|
| POST   | `/policies/{id}/payments`      | Record a payment (oldest-unpaid-first allocation)         |
| GET    | `/policies/{id}/payments`      | Payment history (descending by payment date)              |

### Reports

| Method | Path                                          | Description                                        |
|--------|-----------------------------------------------|----------------------------------------------------|
| POST   | `/renewal-notices`                            | Generate renewal notices (1–365 day lookahead)     |
| GET    | `/reports/expiring-policies`                  | Expiring policies within N days (`?days=`)         |
| GET    | `/reports/overdue-payments`                   | Overdue installments report                        |
| GET    | `/agents/{id}/book-of-business`               | Agent book of business                             |
| GET    | `/agents/{id}/book-of-business/export`        | Export agent book of business                      |

### Example requests

**Create a customer**

```bash
curl -i -X POST http://localhost:8080/insurance-policy-admin/api/customers \
  -H "Content-Type: application/json" \
  -d '{
        "firstName": "Ada",
        "lastName": "Lovelace",
        "email": "ada.lovelace@example.com",
        "phone": "5551234567",
        "dateOfBirth": "1980-12-10",
        "segmentCode": "PREFERRED"
      }'
# → 201 Created, body includes the generated customerId
```

**Issue a policy**

```bash
curl -i -X POST http://localhost:8080/insurance-policy-admin/api/policies \
  -H "Content-Type: application/json" \
  -d '{
        "customerId": 1,
        "agentId": 1,
        "effectiveDate": "2026-01-01",
        "expirationDate": "2026-12-31",
        "billingFrequency": 12,
        "coverages": [
          { "coverageTypeId": 1, "coverageLimit": 100000.00, "premium": 1200.00 }
        ]
      }'
# → 201 Created, policy returned in "quoted" status with a generated policyNumber
```

**Record a payment**

```bash
curl -i -X POST http://localhost:8080/insurance-policy-admin/api/policies/1/payments \
  -H "Content-Type: application/json" \
  -d '{
        "amount": 100.00,
        "paymentDate": "2026-02-01",
        "method": "CARD"
      }'
# → 201 Created, allocation applied oldest-unpaid-first; overpayment → 409
```

---

## Error response format

All errors (both the inline controller checks and service-layer exceptions) are returned through the
same `@ControllerAdvice` global handler as a JSON `ErrorResponse`:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Human-readable description of the problem",
  "fieldErrors": [
    { "field": "email", "message": "must be a valid email address" }
  ],
  "timestamp": "2026-01-01T12:00:00"
}
```

Typical HTTP statuses:

- `400 Bad Request` — validation failures (`VALIDATION_ERROR`), bad input ranges
- `404 Not Found` — unknown policy / customer / agent / beneficiary / underwriter id
- `409 Conflict` — business-rule conflicts (invalid status transition, overpayment, duplicate
  customer/license, allocation exceeds 100%, non-endorsable policy)
- `500 Internal Server Error` — unexpected failures (`INTERNAL_ERROR` catch-all)

`fieldErrors` is populated for field-level validation failures and is empty/omitted otherwise.

---

## Known limitation

**Endorsing a policy whose remaining installments already have `payment_allocation` rows fails.**

When an endorsement regenerates the not-yet-due installments, the stored procedure
`sp_installment_regenerate_remaining` deletes and recreates those installment rows. If any of those
remaining installments already have `payment_allocation` rows pointing at them, the delete is blocked
by an `ON DELETE RESTRICT` foreign key on `payment_allocation`, and the endorsement transaction rolls
back.

This means endorsements apply cleanly to **freshly bound / active policies that have not yet had
payments allocated to future installments**, but can fail on policies where payments have already
been allocated against installments scheduled to be regenerated. This is a known data-model edge case
of the fixture (the restrictive FK is intentional), not a transient error — it is documented here so
reviewers recognize it as an expected boundary of the current schema design.
