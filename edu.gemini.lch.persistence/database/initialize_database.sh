#!/bin/sh

set -x

datauser=$1
database=$2

echo "Initializing databases $database for user $datauser "

find . -name "*.sql" -maxdepth 1 | env LC_ALL=C sort -n | xargs -I {} psql -f {} -d $database -U $datauser -h 127.0.0.1 | grep "NOTICE\|ERROR"

exit 0
