#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Disposable MariaDB only: localhost:33318, database weaver_policy.
mysql_test(){ docker exec -i weaver-policy-test mariadb --default-character-set=utf8mb4 -uroot -plocal-split-test weaver_policy; }
mysql_test < src/test/resources/weaver/policy-fixture.sql
mysql_test < docs/sql/025-weaver-workspace-policy.sql
mysql_test < docs/sql/025-weaver-workspace-policy.sql
WEAVER_POLICY_TEST_URL=jdbc:mariadb://127.0.0.1:33318/weaver_policy mvn -q -o -Dmaven.repo.local=/workspace/.m2 -Dtest=WeaverPolicyIntegrationTest test
