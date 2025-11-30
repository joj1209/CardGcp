# TextStepWriter 클래스 문서

## 개요

`TextStepWriter`는 STEP별로 추출된 테이블 정보를 포맷팅하여 텍스트 파일로 저장하는 Writer 클래스입니다.

`TextWriter`가 파일 전체의 테이블 정보를 출력하는 것과 달리, `TextStepWriter`는 각 STEP별로 구분하여 출력합니다.

## 주요 특징

- **STEP별 포맷팅**: 각 STEP을 구분선으로 명확하게 분리
- **가독성 중심**: 단계별로 소스/타겟 테이블을 명확하게 표시
- **유연한 인코딩**: 다양한 문자셋 지원

## 상수

### DEFAULT_OUTPUT_DIR
- **타입**: `Path` (static final)
- **값**: `D:\11. Project\11. DB\BigQuery_out`
- **설명**: STEP별 테이블 정보를 저장할 기본 출력 디렉토리

## 필드

### outputDir
- **타입**: `Path` (private final)
- **설명**: 결과 파일을 저장할 출력 디렉토리 경로

### charset
- **타입**: `Charset` (private final)
- **설명**: 파일 쓰기에 사용할 문자셋 (기본: UTF-8)

## 생성자

### TextStepWriter(Path outputDir, Charset charset)

출력 디렉토리와 문자셋을 지정하여 인스턴스를 생성합니다.

**파라미터**:
- `outputDir` - 결과 파일을 저장할 디렉토리 경로
- `charset` - 파일 쓰기에 사용할 문자셋

**사용 예시**:
```java
Path outputDir = Paths.get("D:", "output");
Charset charset = Charset.forName("UTF-8");
TextStepWriter writer = new TextStepWriter(outputDir, charset);
```

## 메서드

### writeStepTables(Path inputDir, Path file, Map<String, TablesInfo> stepTables)

입력 디렉토리 기준 상대 경로를 유지하면서 STEP별 테이블 정보를 파일로 저장합니다.

**동작 방식**:
1. 파일명 정보 출력 로그
2. 상대 경로 기반 출력 파일명 생성 (`buildOutputName()`)
3. STEP 개수 출력
4. `writeStepTables(String, Map)` 호출

**파라미터**:
- `inputDir` - SQL 파일의 입력 디렉토리 (상대 경로 계산용)
- `file` - 원본 SQL 파일의 절대 경로
- `stepTables` - STEP별 테이블 정보 Map

**반환값**: 생성된 출력 파일의 Path

**예외**:
- `IOException` - 파일 쓰기 중 오류 발생

**출력 로그**:
```
[StepWriter] Processing step table info: batch.sql
[StepWriter] Generated output filename: batch_step_tables.txt
[StepWriter] Total steps: 3
```

### writeStepTables(String relativeFile, Map<String, TablesInfo> stepTables)

STEP별 테이블 정보를 포맷팅하여 지정된 상대 경로에 저장합니다.

**동작 방식**:
1. `formatStepTables()`로 내용 포맷팅
2. `write()`로 파일 쓰기

**파라미터**:
- `relativeFile` - 출력 디렉토리 기준 상대 파일명
- `stepTables` - STEP별 테이블 정보 Map

**반환값**: 생성된 출력 파일의 Path

**예외**:
- `IOException` - 파일 쓰기 중 오류 발생

### formatStepTables(Map<String, TablesInfo> stepTables)

STEP별 테이블 정보를 읽기 쉬운 텍스트 형식으로 포맷팅합니다.

**출력 형식**:
```
============================================================
STEP001
============================================================

[Source Tables]
스키마.테이블1
스키마.테이블2

[Target Tables]
스키마.테이블3

============================================================
STEP002
============================================================

[Source Tables]
스키마.테이블4

[Target Tables]
스키마.테이블5
스키마.테이블6

```

**특징**:
- 각 STEP을 60개의 `=` 문자로 구분
- STEP명 표시
- 소스 테이블 목록 (`[Source Tables]`)
- 타겟 테이블 목록 (`[Target Tables]`)
- 테이블이 없을 경우 `(No source tables)` 또는 `(No target tables)` 표시

**파라미터**:
- `stepTables` - STEP별 테이블 정보 Map (Key: STEP명, Value: TablesInfo)

**반환값**: 포맷팅된 문자열

**예시**:
```java
Map<String, TablesInfo> stepTables = new LinkedHashMap<>();
stepTables.put("STEP001", info1);
stepTables.put("STEP002", info2);

String formatted = writer.formatStepTables(stepTables);
```

### write(String relativeFile, String content)

지정된 내용을 파일로 저장하는 내부 메서드입니다.

**동작 방식**:
1. 출력 디렉토리와 상대 파일명을 결합하여 절대 경로 생성
2. 부모 디렉토리 생성 (존재하지 않는 경우)
3. 지정된 문자셋으로 내용을 바이트로 변환
4. 파일 쓰기

**파라미터**:
- `relativeFile` - 출력 디렉토리 기준 상대 파일명
- `content` - 파일에 쓸 내용

**반환값**: 생성된 파일의 Path

**예외**:
- `IOException` - 디렉토리 생성 또는 파일 쓰기 중 오류 발생

**출력 로그**:
```
[StepWriter] Creating output directory: D:\11. Project\11. DB\BigQuery_out
[StepWriter] Writing file: batch_step_tables.txt
[StepWriter] File write completed: D:\11. Project\11. DB\BigQuery_out\batch_step_tables.txt
```

### buildOutputName(Path inputDir, Path file)

입력 파일의 상대 경로를 기반으로 출력 파일명을 생성합니다.

**동작 방식**:
1. `inputDir`을 기준으로 `file`의 상대 경로 계산
2. 경로 구분자를 `/`로 통일
3. `.sql` 확장자를 `_step_tables.txt`로 변경

**파라미터**:
- `inputDir` - 입력 디렉토리 경로
- `file` - 원본 SQL 파일의 절대 경로

**반환값**: 출력 파일명 (상대 경로)

**예시**:
```
입력 디렉토리: D:\11. Project\11. DB\BigQuery
입력 파일: D:\11. Project\11. DB\BigQuery\batch\job1.sql
출력 파일명: batch/job1_step_tables.txt
```

## 사용 예시

### 기본 사용

```java
TextStepWriter writer = new TextStepWriter(
    Paths.get("D:", "output"),
    Charset.forName("UTF-8")
);

Map<String, TablesInfo> stepTables = new LinkedHashMap<>();
// ... stepTables 구성

Path inputDir = Paths.get("D:", "input");
Path file = Paths.get("D:", "input", "batch.sql");

writer.writeStepTables(inputDir, file, stepTables);
```

### AppStepJob에서의 사용

```java
public class AppStepJob {
    private final TextStepWriter writer;
    
    private void write(Path file, Map<String, TablesInfo> stepTables) throws IOException {
        writer.writeStepTables(inputDir, file, stepTables);
    }
}
```

## 출력 예시

### 입력: 3개의 STEP이 있는 SQL 파일

```sql
-- STEP001
INSERT INTO BM.공통코드 SELECT * FROM DW.코드목록;

-- STEP002
MERGE INTO BM.회사 AS T USING DW.회사목록 AS S ON T.id = S.id;

-- STEP003
INSERT INTO BM.사용자 SELECT * FROM DW.원천사용자집계;
```

### 출력: batch_step_tables.txt

```
============================================================
STEP001
============================================================

[Source Tables]
DW.코드목록

[Target Tables]
BM.공통코드

============================================================
STEP002
============================================================

[Source Tables]
DW.회사목록

[Target Tables]
BM.회사

============================================================
STEP003
============================================================

[Source Tables]
DW.원천사용자집계

[Target Tables]
BM.사용자

```

## TextWriter vs TextStepWriter 비교

| 측면 | TextWriter | TextStepWriter |
|------|-----------|---------------|
| **출력 형식** | 전체 소스/타겟 | STEP별 소스/타겟 |
| **파일명 규칙** | `*_out.txt` | `*_step_tables.txt` |
| **입력 타입** | `TablesInfo` | `Map<String, TablesInfo>` |
| **구분선** | 없음 | STEP별 구분선 |
| **사용 Job** | AppJob | AppStepJob |

### 출력 비교

**TextWriter 출력**:
```
[Source Tables]
DW.코드목록
DW.회사목록
DW.원천사용자집계

[Target Tables]
BM.공통코드
BM.회사
BM.사용자
```

**TextStepWriter 출력**:
```
============================================================
STEP001
============================================================
[Source Tables]
DW.코드목록
[Target Tables]
BM.공통코드

============================================================
STEP002
============================================================
[Source Tables]
DW.회사목록
[Target Tables]
BM.회사

============================================================
STEP003
============================================================
[Source Tables]
DW.원천사용자집계
[Target Tables]
BM.사용자
```

## 로그 메시지

모든 로그 메시지는 `[StepWriter]` 접두사를 사용하여 다른 컴포넌트와 구분됩니다.

### 처리 시작
```
[StepWriter] Processing step table info: batch.sql
```

### 파일명 생성
```
[StepWriter] Generated output filename: batch_step_tables.txt
```

### STEP 개수
```
[StepWriter] Total steps: 3
```

### 디렉토리 생성
```
[StepWriter] Creating output directory: D:\11. Project\11. DB\BigQuery_out
```

### 파일 쓰기 시작
```
[StepWriter] Writing file: batch_step_tables.txt
```

### 파일 쓰기 완료
```
[StepWriter] File write completed: D:\11. Project\11. DB\BigQuery_out\batch_step_tables.txt
```

## 에러 처리

파일 쓰기 중 발생하는 `IOException`은 호출자(`AppStepJob.processFile()`)에서 처리됩니다:

```java
catch (IOException ex) {
    System.err.println("Step file processing failed: " + file + " - " + ex.getMessage());
}
```

## 개선 제안

### 1. HTML 출력 지원

```java
public Path writeStepTablesAsHtml(String relativeFile, Map<String, TablesInfo> stepTables) {
    String html = formatStepTablesAsHtml(stepTables);
    return write(relativeFile.replace(".txt", ".html"), html);
}
```

### 2. CSV 출력 지원

```java
public Path writeStepTablesAsCsv(String relativeFile, Map<String, TablesInfo> stepTables) {
    // STEP,테이블타입,스키마,테이블명 형식
}
```

### 3. JSON 출력 지원

```java
public Path writeStepTablesAsJson(String relativeFile, Map<String, TablesInfo> stepTables) {
    // JSON 형식으로 출력
}
```

### 4. STEP 통계 추가

```java
private String formatStepTables(Map<String, TablesInfo> stepTables) {
    // 각 STEP의 테이블 개수 통계 추가
    sb.append("Total: ").append(totalSources).append(" sources, ")
      .append(totalTargets).append(" targets\n");
}
```

## 관련 클래스

- `AppStepJob` - 이 Writer를 사용하는 Job
- `TextWriter` - 파일 전체 테이블 정보 출력 Writer
- `TablesInfo` - 테이블 정보 저장 VO
- `SqlReader` - SQL 파일 읽기
- `FileParserProcessor` - SQL 파싱

## 설계 철학

1. **명확한 구분**: STEP별로 명확하게 구분된 출력
2. **가독성**: 사람이 읽기 쉬운 포맷
3. **일관성**: TextWriter와 유사한 구조 유지
4. **확장성**: 다양한 출력 형식 추가 가능한 구조

