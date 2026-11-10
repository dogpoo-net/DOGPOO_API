# WEAVER API

Java 25 LTS / Spring Boot 4.1.2 migration of the uploaded DOGPOO_API source.
Java 25 is the latest LTS target, not the latest short-lived Java release.

## Build and run

Install JDK 25 and Maven 3.9+, then run `mvn verify` and
`java -jar target/weaver-api-1.0.0-SNAPSHOT.jar`.
Supply DB_URL, DB_USERNAME, DB_PASSWORD and JWT_SECRET through the process environment.
Use the existing deployment's JWT secret (including its Base64 representation) to
preserve existing tokens. Do not commit credentials. The original resource files
contain credentials and are intentionally excluded from this repository.

The dispatcher remains at `/dogpoo/*`. If the original WAR had an additional
context path, set API_CONTEXT_PATH to that same path. Default port is 8080.
Scheduling is enabled by default, as in the original XML configuration. For local
smoke checks use `--weaver.scheduling.enabled=false` to prevent scheduled DB writes
and external requests. No schema initialization or migration runs automatically.

Optional integration environment variables:
SMTP_HOST, SMTP_FROM, SMTP_USERNAME, SMTP_PASSWORD,
KAKAO_WEBHOOK_TOKEN, CHATGPT_API_KEY, BATGERS_API_KEY, EXIM_API_KEY,
TELEGRAM_BOT_PATH (the existing Telegram URL path, including its token).
Configure the relevant integration before using its endpoints.

## Changes and compatibility

* Executable Boot JAR replaces the external-container WAR; Jakarta servlet/mail APIs.
* SecurityFilterChain replaces WebSecurityConfigurerAdapter. Original public URL
  patterns are matched against the application-relative URI, including `/dogpoo`.
* Authentication manager initialization avoids the legacy service/configuration cycle.
* JWT filter is registered only in the security chain. Malformed JWT parsing no
  longer escapes as a server error; malformed Bearer prefixes cannot cause substring errors.
* Legacy Jackson 1 and Log4j 1 dependencies removed; Boot-managed logging and JDBC pool.
* Credentials moved out of source; mapper XML files and DB schema unchanged.
* Existing JUnit 4 integration sources preserved under src/legacy-test/java for
  migration, not executed by the new test suite. They require old Spring XML contexts
  and real integrations; do not run them against production without reviewing writes.

`docs/legacy-endpoints.json` records all original controller mapping annotations.
17 controllers / 72 method mappings were compared with the archive and are unchanged.
This is a routing inventory, not proof of full behavioral equivalence.

## Validation and remaining work

Validated on JDK 25: Maven package, 2 passing automated compatibility tests,
application startup with scheduling disabled, live HTTP GET /dogpoo/api/checkGet
and POST /dogpoo/api/checkPost (200 and original Korean JSON messages), and invalid
JWT on a protected URL (401). Smoke checks use an unreachable local DB and do not
claim DB functionality. Original dev/local DB hosts could not be resolved from the
cloud machine; live TCP connection was refused. Configure network access to a test
DB before validating all database-backed routes, authentication and scheduled jobs.

Remaining legacy risks requiring dedicated compatibility tests: broad anonymous URL patterns,
third-party callbacks and integrations, ownership/authorization checks, legacy
JJWT 0.9.1 (retained with JAXB for existing token compatibility), and business logic
validation. This migration is not a completed security audit or production sign-off.

## Mapper SQL hardening

All XML and annotation SQL values now use JDBC bindings. LIKE searches bind the
keyword inside CONCAT while preserving existing `%`/`_` wildcard semantics.
Sorting accepts threadNo, threadTitle, threadViews, threadLikeCnt, threadOwner,
openDate, answerCnt and threadOpenDate; unknown/missing keys fall back to openDate.
Counter columns accept only likeCnt/dislikeCnt and adjustments only `+ 1`/`- 1`;
unsupported values fail before SQL execution. Legacy history pagination retains
its original offset and `page + 8` count, computed by MyBatis before binding.

Five mapper security tests load the actual XML/annotation statements without a
DB and check parameterization, ordering, counter rejection, pagination arithmetic
and absence of raw substitution in mapper sources. Together with the two existing
compatibility tests, seven tests pass. Live MariaDB execution remains unverified;
this change addresses SQL injection, not object ownership or endpoint access policy.
