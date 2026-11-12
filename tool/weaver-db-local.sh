#!/bin/bash
# Run only on DOGPOO; credentials stay in the server process, not in exported files.
set -eu
weaver_db_user=''
weaver_db_pass=''
while IFS='=' read -r key value; do
 case "$key" in
  DB_USERNAME|DB_PASSWORD)
   value=${value%$'\r'}
   if [[ "$value" == \"*\" || "$value" == \'*\' ]]; then value=${value:1:${#value}-2}; fi
   if [[ "$key" == DB_USERNAME ]]; then weaver_db_user=$value; else weaver_db_pass=$value; fi
   ;;
 esac
done < /home/ocaptain/api/api.env
[[ -n "$weaver_db_user" && -n "$weaver_db_pass" ]]
MYSQL_PWD="$weaver_db_pass" mysql --batch --raw --skip-column-names --host=127.0.0.1 --user="$weaver_db_user" DOGPOO_LIVE
