# AppJob vs Spring Batch 스텝 구조 비교

## 개요

`AppJob` 클래스는 Spring Batch 프레임워크의 Job 개념을 모방하여 설계되었지만, 실제 구현에는 중요한 차이점들이 있습니다. 이 문서는 두 접근 방식의 구조적 차이와 설계 철학을 비교합니다.

## 1. 스텝 개념의 차이

### Spring Batch의 스텝 구조

Spring Batch에서 **Step**은 **독립적인 실행 단위**입니다.

```java
@Bean
public Job importUserJob(JobRepository jobRepository, Step step1, Step step2, Step step3) {
    return new JobBuilder("importUserJob", jobRepository)
        .start(step1)      // STEP1: 파일 읽기
        .next(step2)       // STEP2: 데이터 처리
        .next(step3)       // STEP3: 데이터 쓰기
        .build();
}

@Bean
public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("step1", jobRepository)
        .<Person, Person>chunk(10, transactionManager)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
}
```

**특징**:
- 각 Step은 독립적으로 실행되는 별도의 실행 단위
- Step 간의 흐름 제어 (start → next → next)
- 각 Step은 독립적인 트랜잭션 범위를 가짐
- Step 실행 결과에 따라 다음 Step 실행 여부 결정 가능

### AppJob의 스텝 구조

AppJob에서 **STEP**은 **단일 파일 처리 파이프라인의 단계**입니다.

```java
public void stepRead() {
    reader.run(inputDir, this::handleFile);  // 모든 파일 순회
}

private void handleFile(Path file, String sql) {
    TablesInfo info = stepParse(sql);    // STEP2: 파싱
    stepWrite(file, info);                // STEP3: 쓰기
}
```

**특징**:
- STEP1, 2, 3이 하나의 파일 처리 파이프라인으로 연결됨
- 각 파일마다 전체 파이프라인(STEP1→2→3)이 반복 실행됨
- Step이 독립적인 실행 단위가 아니라 파이프라인의 단계
- Step 간 흐름 제어가 없고 순차 실행만 가능

## 2. 실행 모델 비교

### Spring Batch: Step 기반 실행

```
Job
├── Step1 (모든 데이터 읽기)
│   ├── Reader: 1000개 레코드 읽기
│   ├── Processor: 1000개 레코드 처리
│   └── Writer: 1000개 레코드 쓰기
├── Step2 (데이터 변환)
│   ├── Reader: 1000개 레코드 읽기
│   ├── Processor: 1000개 레코드 변환
│   └── Writer: 1000개 레코드 쓰기
└── Step3 (데이터 검증)
    ├── Reader: 1000개 레코드 읽기
    ├── Processor: 1000개 레코드 검증
    └── Writer: 1000개 레코드 쓰기
```

**실행 순서**:
1. Step1이 **모든 데이터**를 처리 완료
2. Step2가 **모든 데이터**를 처리 완료
3. Step3이 **모든 데이터**를 처리 완료

### AppJob: 파일 기반 파이프라인 실행

```
AppJob
├── 파일1.sql
│   ├── Reader (파일1 읽기)
│   ├── Process (파일1 파싱)
│   └── Write (파일1 쓰기)
├── 파일2.sql
│   ├── Reader (파일2 읽기)
│   ├── Process (파일2 파싱)
│   └── Write (파일2 쓰기)
└── 파일3.sql
    ├── Reader (파일3 읽기)
    ├── Process (파일3 파싱)
    └── Write (파일3 쓰기)
```

**실행 순서**:
1. 파일1에 대해 Reader→Process→Write 완료
2. 파일2에 대해 Reader→Process→Write 완료
3. 파일3에 대해 Reader→Process→Write 완료

## 3. 주요 차이점 상세 분석

### 3.1 스텝의 의미

| 구분 | Spring Batch | AppJob |
|------|-------------|--------|
| **Step의 의미** | 독립적인 작업 단위 | 파이프라인의 처리 단계 |
| **Step 실행 범위** | 전체 데이터셋 | 단일 파일 |
| **Step 간 관계** | 순차적/조건부 실행 | 파이프라인 체인 |

### 3.2 데이터 처리 방식

**Spring Batch - Chunk 기반 처리**:
```java
.<Person, Person>chunk(100)  // 100개씩 묶어서 처리
    .reader(reader())         // 100개 읽기
    .processor(processor())   // 100개 처리
    .writer(writer())         // 100개 쓰기
```
- 대량 데이터를 청크(chunk) 단위로 나누어 처리
- 메모리 효율적
- 트랜잭션 단위 제어 가능

**AppJob - 파일 단위 처리**:
```java
private void processFile(Path file, String sql) {
    TablesInfo info = process(sql);  // 전체 파일 파싱
    write(file, info);                // 결과 쓰기
}
```
- 파일 전체를 메모리에 로드하여 처리
- 파일이 작을 때 적합
- 파일이 크면 메모리 문제 발생 가능

### 3.3 트랜잭션 관리

**Spring Batch**:
```java
@Bean
public Step step1(JobRepository jobRepository, 
                  PlatformTransactionManager transactionManager) {
    return new StepBuilder("step1", jobRepository)
        .<Person, Person>chunk(10, transactionManager)  // 10개마다 커밋
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
}
```
- Chunk 단위로 트랜잭션 관리
- 실패 시 청크 단위로 롤백
- 재시작 지원 (중단된 위치부터 재개)

**AppJob**:
```java
private void processFile(Path file, String sql) {
    try {
        TablesInfo info = process(sql);
        write(file, info);
    } catch (IOException ex) {
        System.err.println("파일 처리 실패: " + file);
        // 다음 파일 계속 처리
    }
}
```
- 명시적 트랜잭션 관리 없음
- 파일 단위로 성공/실패 처리
- 실패한 파일은 스킵하고 다음 파일 계속 처리

### 3.4 에러 처리 및 재시작

**Spring Batch**:
```java
.faultTolerant()
    .skip(Exception.class)
    .skipLimit(10)           // 10개까지 스킵 허용
    .retry(Exception.class)
    .retryLimit(3)           // 3번까지 재시도
```
- 정교한 에러 처리 전략
- Skip, Retry, Restart 지원
- JobRepository에 실행 상태 저장
- 중단된 Job을 재시작 가능

**AppJob**:
```java
catch (IOException ex) {
    System.err.println("파일 처리 실패: " + file + " - " + ex.getMessage());
    // 단순히 에러 출력 후 다음 파일 처리
}
```
- 단순 예외 처리
- 실패한 파일은 로그만 남기고 스킵
- 재시작 기능 없음
- 실행 상태 추적 없음

### 3.5 흐름 제어

**Spring Batch - 조건부 흐름 제어**:
```java
@Bean
public Job conditionalJob(JobRepository jobRepository) {
    return new JobBuilder("conditionalJob", jobRepository)
        .start(step1())
        .on("COMPLETED").to(step2())        // 성공 시 step2
        .from(step1()).on("FAILED").to(step3())  // 실패 시 step3
        .end()
        .build();
}
```
- Step 실행 결과에 따라 다음 Step 결정
- 조건부 분기, 병렬 실행 지원
- 복잡한 워크플로우 구성 가능

**AppJob - 고정된 파이프라인**:
```java
private void processFile(Path file, String sql) {
    TablesInfo info = process(sql);    // 항상 Process 실행
    write(file, info);                  // 항상 Write 실행
}
```
- 고정된 순서로만 실행 (Reader→Process→Write)
- 조건부 분기 불가
- 단순하고 예측 가능한 흐름

## 4. Spring Batch의 실제 Reader-Processor-Writer 패턴

### Spring Batch의 Chunk-Oriented Processing

```java
@Bean
public Step chunkStep(JobRepository jobRepository, 
                      PlatformTransactionManager transactionManager) {
    return new StepBuilder("chunkStep", jobRepository)
        .<InputType, OutputType>chunk(100, transactionManager)
        .reader(itemReader())      // 한 번에 1개씩 읽기
        .processor(itemProcessor()) // 읽은 아이템 처리
        .writer(itemWriter())       // 100개 모아서 쓰기
        .build();
}
```

**처리 흐름**:
```
Step 실행
  ↓
Chunk 1 (100개)
  ├── Reader.read() → Item 1
  ├── Processor.process(Item 1) → Processed Item 1
  ├── Reader.read() → Item 2
  ├── Processor.process(Item 2) → Processed Item 2
  ├── ... (98번 반복)
  ├── Reader.read() → Item 100
  ├── Processor.process(Item 100) → Processed Item 100
  └── Writer.write(List<Processed Items 1-100>)
  ↓
Commit Transaction
  ↓
Chunk 2 (100개)
  ├── ... (동일 과정 반복)
```

### AppJob의 파일 처리 흐름

```java
public void execute() {
    reader.run(inputDir, this::processFile);
}

private void processFile(Path file, String sql) {
    TablesInfo info = process(sql);
    write(file, info);
}
```

**처리 흐름**:
```
execute() 실행
  ↓
파일1.sql
  ├── Reader: 파일 전체 내용 읽기
  ├── Process: 전체 내용 파싱
  └── Write: 결과 파일 쓰기
  ↓
파일2.sql
  ├── Reader: 파일 전체 내용 읽기
  ├── Process: 전체 내용 파싱
  └── Write: 결과 파일 쓰기
```

## 5. 각 접근 방식의 장단점

### Spring Batch의 장점

✅ **엔터프라이즈급 기능**
- 트랜잭션 관리, 재시작, 재시도, 스킵 등 강력한 기능
- 대량 데이터 처리에 최적화
- 실행 상태 추적 및 모니터링

✅ **확장성**
- 멀티스레드, 파티셔닝, 원격 청킹 지원
- 대용량 데이터 처리 가능

✅ **유연성**
- 조건부 흐름, 병렬 실행, 복잡한 워크플로우 구성 가능

❌ **단점**
- 학습 곡선이 높음
- 설정이 복잡함
- 간단한 작업에는 오버스펙

### AppJob의 장점

✅ **단순성**
- 이해하기 쉬운 구조
- 최소한의 의존성 (Spring Framework 불필요)
- 빠른 개발 가능

✅ **가독성**
- 명확한 파이프라인 구조
- 디버깅이 쉬움

✅ **경량**
- 작은 프로젝트에 적합
- 배포가 간단함

❌ **단점**
- 대용량 파일 처리 시 메모리 문제
- 트랜잭션 관리 부재
- 재시작, 재시도 기능 없음
- 병렬 처리 어려움

## 6. 사용 시나리오 비교

### Spring Batch가 적합한 경우

1. **대량 데이터 처리**
   - 수백만 건의 레코드 처리
   - 데이터베이스 마이그레이션
   - 대용량 파일 처리 (GB 단위)

2. **복잡한 비즈니스 로직**
   - 조건부 실행 흐름이 필요한 경우
   - 여러 단계의 데이터 변환이 필요한 경우
   - 병렬 처리가 필요한 경우

3. **엔터프라이즈 요구사항**
   - 트랜잭션 관리가 중요한 경우
   - 재시작/재시도가 필요한 경우
   - 실행 이력 추적이 필요한 경우

### AppJob이 적합한 경우

1. **소규모 배치 작업**
   - 수십~수백 개의 작은 파일 처리
   - 단순한 데이터 추출 작업
   - 파일 크기가 작은 경우 (MB 단위)

2. **단순한 처리 흐름**
   - 고정된 순서의 처리 단계
   - 조건부 분기가 불필요한 경우
   - 파일 단위로 독립적인 처리

3. **빠른 개발 필요**
   - 프로토타입 개발
   - 일회성 스크립트
   - Spring 의존성을 피하고 싶은 경우

## 7. 구조적 개선 제안

AppJob을 Spring Batch에 더 가깝게 만들려면 다음과 같은 개선이 필요합니다:

### 7.1 Step을 독립적인 실행 단위로 분리

```java
public interface Step {
    void execute();
}

public class ReadStep implements Step {
    public void execute() {
        // 모든 파일 읽기
    }
}

public class ParseStep implements Step {
    public void execute() {
        // 모든 파일 파싱
    }
}

public class WriteStep implements Step {
    public void execute() {
        // 모든 결과 쓰기
    }
}

public class AppJob {
    private List<Step> steps;
    
    public void run() {
        for (Step step : steps) {
            step.execute();  // Step 단위로 실행
        }
    }
}
```

### 7.2 청크 기반 처리 도입

```java
public class ChunkBasedProcessor {
    private int chunkSize = 10;
    
    public void process(List<Path> files) {
        for (int i = 0; i < files.size(); i += chunkSize) {
            List<Path> chunk = files.subList(i, 
                Math.min(i + chunkSize, files.size()));
            processChunk(chunk);  // 10개씩 묶어서 처리
        }
    }
}
```

### 7.3 상태 관리 및 재시작 지원

```java
public class JobRepository {
    public void saveJobExecution(JobExecution execution) {
        // 실행 상태 저장
    }
    
    public JobExecution getLastExecution(String jobName) {
        // 마지막 실행 상태 조회
    }
}

public class AppJob {
    public void restart() {
        JobExecution lastExecution = repository.getLastExecution("appJob");
        int lastProcessedIndex = lastExecution.getLastProcessedIndex();
        // 중단된 위치부터 재개
    }
}
```

## 8. 결론

### 핵심 차이점 요약

| 측면 | Spring Batch | AppJob |
|------|-------------|--------|
| **Step 개념** | 독립적인 실행 단위 | 파이프라인의 단계 |
| **실행 모델** | Step별 전체 데이터 처리 | 파일별 전체 단계 처리 |
| **데이터 처리** | Chunk 기반 (메모리 효율적) | 파일 전체 로드 |
| **트랜잭션** | Chunk 단위 트랜잭션 | 트랜잭션 관리 없음 |
| **에러 처리** | Skip, Retry, Restart | 단순 에러 로깅 |
| **흐름 제어** | 조건부, 병렬 실행 가능 | 고정된 순차 실행 |
| **복잡도** | 높음 (학습 곡선 존재) | 낮음 (직관적) |
| **확장성** | 매우 높음 | 제한적 |
| **사용 사례** | 대규모 엔터프라이즈 배치 | 소규모 파일 처리 |

### 최종 평가

**AppJob**은 Spring Batch의 **Reader-Processor-Writer 패턴**을 차용했지만, **Step의 개념과 실행 모델**은 완전히 다릅니다.

- **Spring Batch**: Step이 전체 데이터셋을 처리하는 독립적인 작업 단위
- **AppJob**: Step이 단일 파일을 처리하는 파이프라인의 단계

AppJob은 "Spring Batch를 모방한" 설계라기보다는, **파일 처리에 특화된 간단한 파이프라인 프레임워크**로 보는 것이 더 정확합니다. 소규모 파일 처리 작업에는 충분히 효과적이지만, 대규모 배치 처리나 복잡한 워크플로우가 필요한 경우에는 실제 Spring Batch 사용을 고려해야 합니다.

