#!/bin/bash
# Requires Docker, Java 25, Maven. Uses no production credentials/data.
set -euo pipefail
cd "$(dirname "$0")/.."
container=weaver-content-split-test
# Container must already run MariaDB 10.11 on localhost:33317 with the disposable password.
mysql_test(){ docker exec -i "$container" mariadb --default-character-set=utf8mb4 -uroot -plocal-split-test weaver_split; }
mysql_test < src/test/resources/weaver/content-split-fixture.sql
mysql_test < docs/sql/023-weaver-item-content-split.sql
mysql_test < docs/sql/023-weaver-item-content-split.sql
WEAVER_SPLIT_TEST_URL=jdbc:mariadb://127.0.0.1:33317/weaver_split mvn -q -o -Dmaven.repo.local=/workspace/.m2 -Dtest=WeaverItemContentIntegrationTest test
# Explicit rollback then re-migrate to prove deploy recovery preserves content.
mysql_test < docs/sql/023-weaver-item-content-rollback.sql
mysql_test < docs/sql/023-weaver-item-content-split.sql
WEAVER_SPLIT_TEST_URL=jdbc:mariadb://127.0.0.1:33317/weaver_split mvn -q -o -Dmaven.repo.local=/workspace/.m2 -Dtest=WeaverItemContentIntegrationTest test
