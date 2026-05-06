#!/bin/bash

for id in 999 888 777 666 555; do
    redis-cli PUBLISH order:event "ORDER_COMPLETE:$id" > /dev/null &
    echo "[Publisher] 이벤트 발행: ORDER_COMPLETE:$id"
done

wait
echo "전송 완료"
