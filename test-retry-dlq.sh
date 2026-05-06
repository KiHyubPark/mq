#!/bin/bash

# DLQ + 재시도 실습용 스크립트
# - FAIL_ 메시지 1건만 적재 -> [RETRY] 2개 + [DLQ] 1개 블록 (총 3개)
# - 정상 메시지는 test-orders.sh 가 이미 시연하므로 여기서는 제외
#
# 검증:
#   redis-cli LRANGE order:dlq 0 -1   # 실패 메시지 확인
#   앱 콘솔 로그에서 [RETRY] / [DLQ] 흐름 확인

QUEUE="order:retry"
DLQ="order:dlq"

# 매 실행마다 깔끔한 결과를 보기 위해 retry/dlq 큐 초기화
deleted=$(redis-cli DEL "$QUEUE" "$DLQ")
echo "[Cleanup] DEL $QUEUE $DLQ -> 삭제된 키 수: $deleted"

# 실패 메시지 1건 -> [RETRY] 2개 + [DLQ] 1개 블록 (총 3개)
#   FAIL_* 는 RetryDlqConsumer 가 항상 실패로 간주하므로
#   MAX_RETRY=3 한도까지 재시도된 뒤 DLQ 로 격리됨
redis-cli RPUSH "$QUEUE" "FAIL_ORDER-9" > /dev/null
echo "[Producer] 실패 메시지 적재: FAIL_ORDER-9"

echo "전송 완료. 수 초 후 'redis-cli LRANGE order:dlq 0 -1' 로 DLQ 확인하세요."
