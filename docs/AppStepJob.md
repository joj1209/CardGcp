# AppStepJob 클래스 문서

## 개요

`AppStepJob`은 SQL 파일에서 **스텝별로** 소스/타겟 테이블 정보를 추출하는 배치 작업 클래스입니다.

기본 `AppJob`이 파일 전체의 테이블을 추출하는 것과 달리, `AppStepJob`은 `-- STEP001`, `/* STEP002 */` 등의 마커로 구분된 각 단계별로 테이블 정보를 추출합니다.

## 주요 특징

- **스텝별 처리**: SQL 파일 내의 STEP 마커를 인식하여 단계별로 분석
- **기존 컴포넌트 재사용**: `SqlReader`, `FileParserProcessor` 재사용
- **AppJob 구조 준용**: Reader → Processor → Writer 파이프라인 유지
- **스텝 자동 감지**: STEP 주석 패턴 자동 인식

## 처리 단계

### Reader (SqlReader 재사용)
지정된 디렉토리의 모든 .sql 파일을 읽습니다.

### Processor (Step 분리 + FileParserProcessor 재사용)
1. SQL 파일에서 STEP 마커를 찾아 구분
2. 각 STEP별로 SQL을 분리
3. 각 STEP의 SQL을 `FileParserProcessor`로 파싱
4. STEP별 `TablesInfo`를 Map으로 구성

### Writer (TextStepWriter - 신규)
STEP별 테이블 정보를 포맷팅하여 파일로 저장합니다.

## 필드

### inputDir
- **타입**: `Path`
- **설명**: SQL 파일이 위치한 입력 디렉토리 경로

### reader
- **타입**: `SqlReader`
- **설명**: SQL 파일을 읽는 Reader (AppJob과 동일)

### processor
- **타입**: `FileParserProcessor`
- **설명**: SQL을 파싱하는 Processor (AppJob과 동일)

### writer
- **타입**: `TextStepWriter`
- **설명**: STEP별 테이블 정보를 출력하는 Writer (신규)

## 생성자

### AppStepJob(Path inputDir, SqlReader reader, FileParserProcessor processor, TextStepWriter writer)

각 단계(Reader, Processor, Writer)를 외부에서 주입받아 구성합니다.

**파라미터**:
- `inputDir` - SQL 파일 입력 디렉토리
- `reader` - SqlReader 인스턴스
- `processor` - FileParserProcessor 인스턴스
- `writer` - TextStepWriter 인스턴스

## 메서드

### createJob()

기본 설정으로 `AppStepJob` 인스턴스를 생성하는 정적 팩토리 메서드입니다.

**기본 설정**:
- 입력 디렉토리: `D:\11. Project\11. DB\BigQuery`
- 입력 문자셋: UTF-8
- 출력 디렉토리: `D:\11. Project\11. DB\BigQuery_out`
- 출력 문자셋: UTF-8

**반환값**: 기본 설정이 적용된 `AppStepJob` 인스턴스

### execute()

Job을 실행하여 입력 디렉토리의 모든 .sql 파일을 읽고 스텝별로 처리를 시작합니다.

**처리 흐름**:
```
execute()
  → SqlReader.run()
  → 각 파일마다 processFile() 호출
  → processSteps() + write()
```

### processFile(Path file, String sql)

단일 SQL 파일을 읽어 스텝별로 처리하는 메서드입니다.

**처리 순서**:
1. `processSteps()`를 호출하여 스텝별 테이블 정보 추출
2. `write()`를 호출하여 결과 파일 저장

**파라미터**:
- `file` - SQL 파일 경로
- `sql` - SQL 파일 내용

**예외 처리**: 파일 처리 실패 시 에러 로그 출력 후 다음 파일 계속 처리

### processSteps(String sql)

SQL 문자열에서 STEP 마커를 찾아 각 단계별로 테이블 정보를 추출합니다.

**동작 방식**:

1. **STEP 마커 패턴 인식**:
   ```sql
   -- STEP001
   /* STEP002 */
   ```

2. **SQL 분리**:
   각 STEP 마커 사이의 SQL을 추출

3. **각 STEP 파싱**:
   `FileParserProcessor.parse()`로 소스/타겟 테이블 추출

4. **결과 수집**:
   `Map<String, TablesInfo>` 형태로 반환
   - Key: "STEP001", "STEP002", ...
   - Value: 해당 STEP의 `TablesInfo`

**파라미터**:
- `sql` - 전체 SQL 문자열

**반환값**: 
- `Map<String, TablesInfo>` - STEP별 테이블 정보
- STEP 마커가 없으면 전체를 "STEP000"으로 처리

**출력 로그**:
```
[StepProcessor] STEP001 - Sources: 3, Targets: 1
[StepProcessor] STEP002 - Sources: 5, Targets: 2
```

### write(Path file, Map<String, TablesInfo> stepTables)

STEP별 테이블 정보를 파일로 저장합니다.

**파라미터**:
- `file` - 원본 SQL 파일 경로
- `stepTables` - STEP별 테이블 정보 Map

**예외**:
- `IOException` - 파일 쓰기 실패

### main(String[] args)

프로그램의 진입점입니다.

**실행 순서**:
1. `createJob()`으로 기본 설정의 인스턴스 생성
2. `execute()`로 배치 작업 실행

## STEP 마커 패턴

### 지원하는 패턴

```sql
-- STEP001
-- STEP1
-- step001
-- step1

/* STEP001 */
/* STEP1 */
/* step001 */
/* step1 */
```

### 패턴 특징

- **대소문자 무관**: STEP, Step, step 모두 인식
- **숫자 형식 자동 변환**: STEP1 → STEP001, STEP12 → STEP012
- **주석 형식 지원**: 라인 주석(`--`), 블록 주석(`/* */`) 모두 지원

## 사용 예시

### 입력 SQL 파일 예시

```sql
-- STEP001
/* 첫 번째 단계: 공통 코드 적재 */
INSERT INTO BM.`공통코드`
SELECT * FROM DW.`코드목록`;

-- STEP002
/* 두 번째 단계: 회사 정보 적재 */
MERGE INTO BM.`회사` AS T
USING DW.`회사목록` AS S
ON T.id = S.id
WHEN MATCHED THEN UPDATE SET T.name = S.name;

-- STEP003
/* 세 번째 단계: 사용자 집계 */
INSERT INTO BM.`사용자`
SELECT user_id, user_name
FROM DW.`원천사용자집계`
WHERE active = 'Y';
```

### 출력 파일 예시

```
============================================================
STEP001
============================================================

[Source Tables]
DW.`코드목록`

[Target Tables]
BM.`공통코드`

============================================================
STEP002
============================================================

[Source Tables]
DW.`회사목록`

[Target Tables]
BM.`회사`

============================================================
STEP003
============================================================

[Source Tables]
DW.`원천사용자집계`

[Target Tables]
BM.`사용자`

```

### 기본 실행

```java
AppStepJob job = AppStepJob.createJob();
job.execute();
```

### 커스텀 설정 실행

```java
Path inputDir = Paths.get("D:", "custom", "input");
SqlReader reader = new SqlReader(Charset.forName("EUC-KR"));
FileParserProcessor processor = FileParserProcessor.withDefaults();
TextStepWriter writer = new TextStepWriter(
    Paths.get("D:", "custom", "output"), 
    Charset.forName("UTF-8")
);

AppStepJob job = new AppStepJob(inputDir, reader, processor, writer);
job.execute();
```

## AppJob vs AppStepJob 비교

| 측면 | AppJob | AppStepJob |
|------|--------|-----------|
| **출력 단위** | 파일 전체 | STEP별 |
| **Writer** | TextWriter | TextStepWriter |
| **출력 형식** | 전체 소스/타겟 테이블 | STEP별 소스/타겟 테이블 |
| **STEP 인식** | 없음 | STEP 마커 자동 인식 |
| **사용 목적** | 전체 테이블 목록 확인 | 단계별 데이터 흐름 분석 |

### 처리 흐름 비교

**AppJob**:
```
파일.sql
  → 전체 파싱
  → [Source Tables] 전체 목록
     [Target Tables] 전체 목록
```

**AppStepJob**:
```
파일.sql
  → STEP001 파싱
     [Source Tables] STEP001 목록
     [Target Tables] STEP001 목록
  → STEP002 파싱
     [Source Tables] STEP002 목록
     [Target Tables] STEP002 목록
  → STEP003 파싱
     ...
```

## 출력 파일명 규칙

- **AppJob**: `원본파일명_out.txt`
- **AppStepJob**: `원본파일명_step_tables.txt`

**예시**:
- 입력: `batch_job.sql`
- AppJob 출력: `batch_job_out.txt`
- AppStepJob 출력: `batch_job_step_tables.txt`

## 에러 처리

### 파일 처리 실패

```java
catch (IOException ex) {
    System.err.println("Step file processing failed: " + file + " - " + ex.getMessage());
}
```

- 개별 파일 처리 실패 시 해당 파일 스킵
- 다음 파일 처리 계속 진행
- 에러 로그 출력

### STEP 마커 없는 경우

```java
if (stepTables.isEmpty()) {
    TablesInfo info = processor.parse(sql);
    stepTables.put("STEP000", info);
    System.out.println("[StepProcessor] No step markers found, processing as STEP000");
}
```

- STEP 마커가 없으면 전체를 "STEP000"으로 처리
- 정상적으로 파싱 진행

## 진행 상황 로그

### StepProcessor 로그

```
[StepProcessor] STEP001 - Sources: 3, Targets: 1
[StepProcessor] STEP002 - Sources: 5, Targets: 2
[StepProcessor] STEP003 - Sources: 2, Targets: 1
```

### StepWriter 로그

```
[StepWriter] Processing step table info: batch_job.sql
[StepWriter] Generated output filename: batch_job_step_tables.txt
[StepWriter] Total steps: 3
[StepWriter] Creating output directory: D:\11. Project\11. DB\BigQuery_out
[StepWriter] Writing file: batch_job_step_tables.txt
[StepWriter] File write completed: D:\11. Project\11. DB\BigQuery_out\batch_job_step_tables.txt
```

## 활용 사례

### 1. 배치 작업 의존성 분석

STEP별로 어떤 테이블을 읽고 쓰는지 확인하여 작업 순서와 의존성을 파악할 수 있습니다.

### 2. 데이터 리니지 추적

각 단계에서 데이터가 어떻게 흐르는지 추적할 수 있습니다.

### 3. 영향도 분석

특정 테이블 변경 시 어떤 STEP들이 영향을 받는지 확인할 수 있습니다.

### 4. 문서화

배치 작업의 각 단계를 자동으로 문서화할 수 있습니다.

## 제한사항

1. **STEP 순서**: 파일에 작성된 순서대로 처리 (병렬 처리 미지원)
2. **중첩 STEP**: 중첩된 STEP 구조는 지원하지 않음
3. **동적 SQL**: 동적으로 생성되는 SQL은 분석 불가

## 개선 제안

### 1. STEP 의존성 그래프 생성

```java
public Map<String, Set<String>> buildDependencyGraph(Map<String, TablesInfo> stepTables) {
    // STEP001의 타겟이 STEP002의 소스인 경우 의존성 연결
}
```

### 2. 병렬 실행 가능 STEP 탐지

```java
public Set<String> findParallelizableSteps(Map<String, TablesInfo> stepTables) {
    // 서로 독립적인 STEP들을 찾아 병렬 처리 가능 여부 판단
}
```

### 3. STEP 통계 정보

```java
public StepStatistics calculateStatistics(Map<String, TablesInfo> stepTables) {
    // 각 STEP의 테이블 개수, 스키마 분포 등 통계 정보 생성
}
```

## 관련 클래스

- `AppJob` - 파일 전체 테이블 추출 Job
- `SqlReader` - SQL 파일 읽기 (재사용)
- `FileParserProcessor` - SQL 파싱 (재사용)
- `TextStepWriter` - STEP별 결과 출력 (신규)
- `TablesInfo` - 테이블 정보 저장 VO

## 설계 철학

1. **재사용**: 기존 Reader, Processor 재사용으로 코드 중복 최소화
2. **일관성**: AppJob의 구조를 준용하여 학습 용이
3. **확장성**: Map 구조로 유연한 STEP 관리
4. **가독성**: STEP별 구분으로 명확한 데이터 흐름 파악

