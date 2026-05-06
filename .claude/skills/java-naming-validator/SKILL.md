---
name: java-naming-validator
description: Java 코드에서 의미 없는 단일 문자 변수명, 축약어 변수명을 탐지하고 의도가 드러나는 이름으로 교체 제안 및 수정. 루프 인덱스(i, j, k)와 스트림 람다처럼 관례적으로 허용된 케이스는 제외.
origin: local
---

# Java Naming Validator

Java 코드에서 의미 없는 변수명을 탐지하고 수정하는 스킬.

## When to Use

- 코드 리뷰 후 변수명 품질 개선이 필요할 때
- 새로 작성한 Java 파일의 네이밍을 검증할 때
- 레거시 코드의 가독성을 높이고 싶을 때

## 허용 예외 (수정 불필요)

아래 케이스는 관례적으로 허용되므로 지적하지 않는다:

| 케이스 | 예시 | 이유 |
|--------|------|------|
| for 루프 인덱스 | `i`, `j`, `k` | 업계 표준 관례 |
| 스트림 람다 파라미터 | `.map(e -> ...)`, `.filter(s -> ...)` | 짧은 스코프, 타입에서 의미 파악 가능 |
| 수학 공식 변수 | `x`, `y`, `z` | 도메인 관례 |
| try-catch 예외 | `e` in `catch (Exception e)` | 단일 라인 재던지기 한정 |

## 탐지 대상 — 나쁜 패턴

### 1. 단일 문자 일반 변수

```java
// BAD
long n = seq.incrementAndGet();
String s = message.trim();
int c = list.size();
Object o = factory.create();

// GOOD
long messageSeq = seq.incrementAndGet();
String trimmedMessage = message.trim();
int itemCount = list.size();
Object createdBean = factory.create();
```

### 2. 의미 없는 약어

```java
// BAD
String msg = buildMessage();
int cnt = items.size();
String tmp = process(value);
Object obj = factory.get();
int num = getTotal();
String str = serialize(data);
int val = compute();
String res = fetch();
String ret = format(input);

// GOOD
String errorMessage = buildMessage();
int itemCount = items.size();
String processedValue = process(value);
Object createdInstance = factory.get();
int totalCount = getTotal();
String serializedData = serialize(data);
int computedScore = compute();
String fetchedResponse = fetch();
String formattedOutput = format(input);
```

### 3. 타입 반복형 약어

```java
// BAD — 타입명을 그대로 줄인 것
RedisTemplate rt = ...;
MessageListenerAdapter mla = ...;
StringRedisTemplate srt = ...;

// GOOD — 역할/목적 기반
RedisTemplate redisTemplate = ...;
MessageListenerAdapter stockAdapter = ...;
StringRedisTemplate stringRedisTemplate = ...;
```

## 탐지 절차

### Phase 1 — SCAN

대상 경로의 모든 `.java` 파일을 수집한다:

```bash
find {path} -name "*.java" | sort
```

각 파일에서 아래 패턴을 grep한다:

```bash
# 단일 문자 변수 선언 (i, j, k, x, y, z, e 제외)
grep -n "\b[a-wA-W]\s*=" {file}

# 흔한 의미 없는 약어
grep -nE "\b(msg|cnt|tmp|obj|num|str|val|res|ret|buf|ptr|ref|mgr|svc|util|cfg|conf|ctx)\b\s*(=|;)" {file}
```

### Phase 2 — CLASSIFY

각 발견에 대해 판단한다:

1. **허용 예외**에 해당하는가? → SKIP
2. 변수의 **실제 역할/도메인 의미**가 무엇인가?
3. 주변 코드(메서드명, 클래스명, 사용처)에서 의미를 추론한다

### Phase 3 — SUGGEST

각 문제 변수에 대해 구체적인 대체 이름을 제안한다.

이름 결정 원칙:
- **도메인 의미** 우선 (`messageSeq` > `counter`)
- **사용 목적** 반영 (`retryCount` > `count`)
- **타입 반복 금지** (`stringValue` → 타입은 선언에 있음)
- 길이보다 **명확성** 우선 (2-3 단어 조합도 허용)

### Phase 4 — FIX

승인된 변경사항을 적용한다:

- 변수 선언부와 모든 사용처를 함께 수정
- 메서드 시그니처 파라미터 포함
- 한 변수씩 수정 후 컴파일 가능 상태 유지

## 변수명 결정 가이드

| 변수가 나타내는 것 | 추천 네이밍 패턴 |
|-------------------|----------------|
| 카운터 / 순번 | `*Count`, `*Seq`, `*Index` |
| 플래그 / 조건 | `is*`, `has*`, `should*` |
| 메시지 / 텍스트 | `*Message`, `*Payload`, `*Content` |
| 결과값 | `*Result`, `*Response`, `*Output` |
| 임시 중간값 | 임시가 아닌 실제 역할로 명명 |
| 크기 / 개수 | `*Size`, `*Count`, `*Length` |
| 설정값 | `*Config`, `*Setting`, `*Threshold` |

## Output Format

```
파일: {파일 경로}

  [LINE {n}] {원래 코드}
  → 제안: {수정 코드}
  → 이유: {왜 이 이름이 더 좋은가}

수정 건수: {n}건
```
