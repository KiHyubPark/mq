#!/bin/bash

# DLQ + 재시도 실습용 스크립트
# - FAIL_ 메시지 1건 적재
# - RetryDlqConsumer 가 FAIL_* 페이로드를 항상 실패로 간주
# - MAX_RETRY=3 한도까지 재시도 후 DLQ 로 격리
# - 예상 출력: [RETRY] 블록 2개 + [DLQ] 블록 1개 (총 3블록)
# - 정상 메시지 시연은 test-orders.sh 참고
#
# 검증:
#   redis-cli LRANGE order:dlq 0 -1   # DLQ 에 격리된 메시지 확인

QUEUE="order:retry"
DLQ="order:dlq"

# 이전 실행 결과를 초기화 (키가 없어도 정상 — 삭제 수 0 은 오류 아님)
deleted=$(redis-cli DEL "$QUEUE" "$DLQ")
echo "[Cleanup] $QUEUE / $DLQ 초기화"

# FAIL_ 접두사가 붙은 메시지는 RetryDlqConsumer 가 강제 실패로 처리
redis-cli RPUSH "$QUEUE" "FAIL_ORDER-9" > /dev/null
echo "[Producer] 실패 메시지 적재: FAIL_ORDER-9"

echo "전송 완료. 'redis-cli LRANGE order:dlq 0 -1' 로 DLQ 확인"
