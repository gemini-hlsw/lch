#!/bin/sh

set -x

datauser=$1
database=$2

echo "Initializing databases $database for user $datauser "

find . -name "*.sql" -maxdepth 1 | env LC_ALL=C sort -n | xargs -I {} psql -f {} -d $database -U $datauser | grep "NOTICE\|ERROR"

exit 0
