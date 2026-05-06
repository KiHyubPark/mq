#!/bin/bash

for i in $(seq 1 10); do
    id=$(printf "%02d" $((RANDOM % 99 + 1)))
    redis-cli RPUSH order:queue "ORDER-$id" > /dev/null
    echo "[Producer] 큐에 추가: ORDER-$id"
done
