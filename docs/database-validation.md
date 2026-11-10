# Real database validation

The VPN path reaches MariaDB 10.11.5. Original dev credentials select DOGPOO_DEV;
original local/live credentials both select DOGPOO_LIVE. Authentication and SELECT 1
were checked for all three profiles. Functional tests use DOGPOO_DEV only.

## Results

177 server-side PREPARE checks passed, zero failures. All four opt-in DB test
cases passed, and the eleven DB-independent tests plus Maven packaging passed
separately. These totals are test cases, not endpoint coverage counts.

## Safety and reproducibility

Several tables use MyISAM, so rollback is not an isolation mechanism. The tests
never execute mapped DML on persistent tables. SQL-wide coverage uses server
PREPARE/DEALLOCATE (not EXECUTE). Read tests use read-only connections. Write tests
copy SHOW CREATE TABLE definitions into session-scoped temporary tables shadowing
the original table names, which disappear on connection close. Temporary user
fixtures use a one-connection pool; scheduled jobs are disabled. No production
user passwords or user rows are used. No DB schema changes were made.

The tests are opt-in. After establishing an authorized local VPN TCP forward,
set DB_VALIDATION_PORT to its local port and DB_VALIDATION_CONFIG to an external
Java XML properties file containing database.url, database.username and
database.password for DOGPOO_DEV. A sibling application.properties supplies the
original jwt.secret. Never commit these files. Run:

```sh
mvn -Dtest=DatabaseMapperValidationTest,DatabaseApplicationValidationTest test
```

The ordinary `mvn verify` suite skips these opt-in tests without the variables.
Run it separately to verify the eleven DB-independent compatibility/security tests.

## Coverage

* Server-side preparation of all registered XML and annotation SQL, including both
  PUBG and ETC statistics branches. This validates syntax and referenced schema,
  not every possible runtime value or every DML outcome.
* Real release-note, notice and game-history mapper reads, including result mapping.
* An injected username string returns no user.
* Temporary comment-table insert, read, update and soft delete with Unicode and
  SQL metacharacters. The real table structure and date function are used.
* Real embedded-server POSTs to both Convatar home list endpoints, validating the
  existing code/msg/DATA envelope.
* Original JWT key token generation and verification.
* Real authentication service and BCrypt against a temporary user table: correct
  password accepted, wrong password rejected.

## Findings

Two pre-existing unused mapper methods were removed after confirming no callers:
getLastChatSeq had invalid WHERE GROUP BY syntax and referenced nonexistent userSeq;
updateUserLevel referenced userLv, absent from both development and live schemas.
No endpoint uses these methods and no schema change was introduced to support them.

The original JWT key works with the retained library despite not being strict
Base64. A future library/key migration still needs explicit token compatibility tests.

## Limits

This is not an end-to-end test of all 72 endpoints. Email, OAuth, image upload,
Telegram, Kakao/OpenAI callbacks, complete business write workflows and scheduled
jobs require integration fixtures and controlled external destinations. They were
not triggered against real users. The existing mapping-inventory test covers all
72 mappings, but mapping equality alone does not prove behavioral equivalence.
