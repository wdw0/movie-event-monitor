#!/usr/bin/env sh
set -eu
CLI_BOOTSTRAP_SERVERS=${KAFKA_CLI_BOOTSTRAP_SERVERS:-broker1:19092,broker2:19092,broker3:19092}
KAFKA_CLI=${KAFKA_CLI:-docker compose exec -T broker1 /opt/kafka/bin/kafka-topics.sh}
# shellcheck disable=SC2086
$KAFKA_CLI --bootstrap-server "$CLI_BOOTSTRAP_SERVERS" --create --if-not-exists --topic movie-events --partitions 3 --replication-factor 3
# shellcheck disable=SC2086
$KAFKA_CLI --bootstrap-server "$CLI_BOOTSTRAP_SERVERS" --create --if-not-exists --topic movie-derived-events --partitions 3 --replication-factor 3
# shellcheck disable=SC2086
$KAFKA_CLI --bootstrap-server "$CLI_BOOTSTRAP_SERVERS" --describe --topic movie-events
# shellcheck disable=SC2086
$KAFKA_CLI --bootstrap-server "$CLI_BOOTSTRAP_SERVERS" --describe --topic movie-derived-events
