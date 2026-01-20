# AppJob 클래스 상세 문서

## 개요

`AppJob`은 SQL 파일에서 소스/타겟 테이블 정보를 추출하는 배치 작업 클래스입니다.

이 클래스는 Spring Batch 프레임워크의 Job 개념을 모방하여 설계되었으며, **Reader → Processor → Writer** 세 단계의 파이프라인으로 구성됩니다.

## 클래스 정보

- **패키지**: `service.queryParser.job`
- **의존 클래스**:
  - `service.queryParser.reader.SqlReader`
  - `service.queryParser.processor.FileParserProcessor`
  - `service.queryParser.writer.TextWriter`
  - `service.queryParser.writer.CsvWriter`
  - `service.queryParser.writer.SourceTableCsvWriter`
  - `service.queryParser.vo.TablesInfo`

## 처리 단계

### STEP1: SqlReader
지정된 디렉토리의 모든 .sql 파일을 순회하며 파일 내용을 읽습니다.

- **입력**: SQL 파일이 저장된 디렉토리 경로
- **출력**: 각 파일의 경로와 내용(문자열)
- **특징**: 지정된 문자셋(기본: EUC-KR)으로 파일을 읽습니다.

### STEP2: FileParserProcessor
SQL 문자열을 파싱하여 소스/타겟 테이블 정보를 추출합니다.

- **입력**: SQL 문자열
- **출력**: `TablesInfo` 객체 (소스 테이블 목록, 타겟 테이블 목록)
- **특징**: MERGE, INSERT, UPDATE, DELETE 등의 DML 문을 분석합니다.

### STEP3: TextWriter
추출된 테이블 정보를 결과 파일로 저장합니다.

- **입력**: `TablesInfo` 객체
- **출력**: 결과 텍스트 파일 (원본 파일명_sql_tables.txt)
- **특징**: 지정된 문자셋(기본: UTF-8)으로 파일을 작성합니다.

### 추가 기능: CSV 출력
모든 파일 처리 완료 후 두 개의 CSV 파일을 생성합니다.

1. **summary.csv**: 프로그램별 소스/타겟 테이블 요약
2. **source_table_mapping.csv**: 소스테이블별 사용 프로그램 매핑

## 필드

### DEFAULT_INPUT_PATH
- **타입**: `static final Path`
- **값**: `D:\11. Project\11. DB\BigQuery`
- **설명**: 기본 입력 디렉토리 경로 (AppJob에서만 관리)

### DEFAULT_OUTPUT_PATH
- **타입**: `static final Path`
- **값**: `D:\11. Project\11. DB\BigQuery_out`
- **설명**: 기본 출력 디렉토리 경로 (AppJob에서만 관리)

### inputDir
- **타입**: `Path`
- **설명**: SQL 파일이 위치한 입력 디렉토리 경로입니다. 이 디렉토리의 모든 .sql 파일이 처리 대상이 됩니다.

### reader
- **타입**: `SqlReader`
- **설명**: STEP1을 담당하는 SqlReader 인스턴스입니다. 지정된 문자셋으로 SQL 파일을 읽어들입니다.

### processor
- **타입**: `FileParserProcessor`
- **설명**: STEP2를 담당하는 FileParserProcessor 인스턴스입니다. SQL 문자열에서 소스/타겟 테이블 정보를 추출합니다.

### writer
- **타입**: `TextWriter`
- **설명**: STEP3을 담당하는 TextWriter 인스턴스입니다. 추출된 테이블 정보를 결과 파일로 저장합니다.

### csvWriter
- **타입**: `CsvWriter`
- **설명**: CSV 형식으로 전체 결과를 저장하는 CsvWriter 인스턴스입니다.

### sourceTableCsvWriter
- **타입**: `SourceTableCsvWriter`
- **설명**: 소스테이블 중심으로 프로그램 매핑 정보를 저장하는 SourceTableCsvWriter 인스턴스입니다.

## 생성자

### AppJob(Path inputDir, SqlReader reader, FileParserProcessor processor, TextWriter writer, CsvWriter csvWriter, SourceTableCsvWriter sourceTableCsvWriter)

AppJob 인스턴스를 생성합니다.

각 단계(Reader, Processor, Writer)를 외부에서 주입받아 유연하게 구성할 수 있습니다. 이를 통해 다양한 입력 문자셋, 출력 디렉토리, 파싱 옵션 등을 설정할 수 있습니다.

**파라미터**:
- `inputDir` - SQL 파일이 위치한 입력 디렉토리 경로 (null 불가)
- `reader` - SQL 파일을 읽는 SqlReader 인스턴스 (null 불가)
- `processor` - SQL을 파싱하는 FileParserProcessor 인스턴스 (null 불가)
- `writer` - 결과를 출력하는 TextWriter 인스턴스 (null 불가)
- `csvWriter` - CSV로 결과를 출력하는 CsvWriter 인스턴스 (null 불가)
- `sourceTableCsvWriter` - 소스테이블 매핑 정보를 출력하는 SourceTableCsvWriter 인스턴스 (null 불가)

## 메서드

### createDefault()

기본 설정으로 AppJob 인스턴스를 생성하는 팩토리 메서드입니다.

**기본 설정**:
- **입력 디렉토리**: `D:\11. Project\11. DB\BigQuery`
- **입력 문자셋**: EUC-KR (`SqlReader.DEFAULT_CHARSET`)
- **출력 디렉토리**: `D:\11. Project\11. DB\BigQuery_out`
- **출력 문자셋**: UTF-8
- **CSV 파일**: `D:\11. Project\11. DB\BigQuery_out\summary.csv`
- **소스테이블 매핑 CSV 파일**: `D:\11. Project\11. DB\BigQuery_out\source_table_mapping.csv`
- **파싱 옵션**: 기본 설정 (`FileParserProcessor.withDefaults()`)

**반환값**: 기본 설정이 적용된 AppJob 인스턴스

특정 경로나 문자셋을 사용하려면 생성자를 직접 호출하십시오.

**사용 예시**:
```java
AppJob job = AppJob.createDefault();
job.stepRead();
```

### stepRead()

STEP1: SqlReader를 사용하여 입력 디렉토리의 모든 .sql 파일을 읽고 처리를 시작합니다.

이 메서드는 배치 파이프라인의 진입점으로, 다음 작업을 수행합니다:

1. `inputDir` 경로에 있는 모든 .sql 파일을 재귀적으로 탐색합니다.
2. 각 파일을 지정된 문자셋(기본: EUC-KR)으로 읽어들입니다.
3. 읽은 파일 경로와 내용을 `handleFile(Path, String)` 메서드로 전달합니다.
4. `handleFile` 메서드에서 STEP2(파싱)와 STEP3(출력)이 순차적으로 실행됩니다.
5. 모든 파일 처리가 완료되면 CSV 파일을 저장합니다.

**처리 흐름**:
```
stepRead()
  → SqlReader.run()
  → 각 파일마다 handleFile() 호출
  → stepParse() + stepWrite() + CSV 레코드 추가
  → 모든 파일 처리 완료 후 CSV 파일 저장
```

**참조**: `SqlReader.run(Path, SqlReader.SqlFileHandler)`, `handleFile(Path, String)`

### handleFile(Path file, String sql)

SqlReader가 읽어온 단일 SQL 파일을 처리하는 핵심 메서드입니다.

이 메서드는 STEP2(파싱)와 STEP3(출력)을 순차적으로 실행하며, SqlReader의 콜백 함수로 각 파일마다 호출됩니다.

**처리 순서**:
1. **STEP2 실행**: `stepParse(String)`를 호출하여 SQL을 파싱하고 소스/타겟 테이블 정보를 추출합니다.
2. **STEP3 실행**: `stepWrite(Path, TablesInfo)`를 호출하여 추출된 정보를 결과 파일로 저장합니다.
3. **CSV 레코드 추가**: CSV 파일에 레코드를 추가합니다.

**예외 처리**: 파일 쓰기 과정에서 IOException이 발생하면 해당 파일의 처리를 건너뛰고 에러 메시지를 표준 에러 스트림에 출력합니다. 다른 파일의 처리는 계속 진행됩니다.

**파라미터**:
- `file` - 원본 SQL 파일의 절대 경로 (예: `D:\11. Project\11. DB\BigQuery\sample.sql`)
- `sql` - 파일에서 읽어들인 SQL 문자열 전체 내용

**참조**: `stepParse(String)`, `stepWrite(Path, TablesInfo)`

### stepParse(String sql)

STEP2: FileParserProcessor를 사용하여 SQL 문자열에서 소스/타겟 테이블 정보를 추출합니다.

이 단계에서는 SQL 문자열을 분석하여 다음 정보를 추출합니다:

- **타겟 테이블**: INSERT, UPDATE, DELETE, MERGE 등의 DML 문에서 데이터가 변경되는 테이블 목록
- **소스 테이블**: FROM, JOIN 절에서 참조되는 테이블 목록

**처리 특징**:
- STEP별 구조를 인식하여 각 단계의 테이블 정보를 별도로 관리
- 백틱(\`)으로 감싸진 테이블명과 일반 테이블명 모두 처리
- 주석 내의 백틱은 제거하여 정확한 테이블명만 추출
- 중복 테이블명 제거 (Set 자료구조 활용)

**파라미터**:
- `sql` - 분석할 SQL 문자열 (여러 STEP을 포함할 수 있음)

**반환값**: `TablesInfo` 객체 (소스 테이블 Set, 타겟 테이블 Set 포함)

**참조**: `FileParserProcessor.parse(String)`, `TablesInfo`

### stepWrite(Path file, TablesInfo info)

STEP3: TextWriter를 사용하여 파싱된 테이블 정보를 결과 파일로 저장합니다.

이 단계에서는 추출된 소스/타겟 테이블 정보를 지정된 형식으로 파일에 기록합니다.

**출력 파일 형식**:
- **파일명**: `{원본파일명}_sql_tables.txt` (예: `sample.sql` → `sample_sql_tables.txt`)
- **저장 위치**: TextWriter에 지정된 출력 디렉토리 (기본: `D:\11. Project\11. DB\BigQuery_out`)
- **문자셋**: UTF-8 (기본값)

**출력 내용 구조**:
```
[Source Tables]
스키마.테이블명1
스키마.테이블명2
...

[Target Tables]
스키마.테이블명3
스키마.테이블명4
...
```

**파라미터**:
- `file` - 원본 SQL 파일 경로 (출력 파일명 생성에 사용)
- `info` - 저장할 테이블 정보 (소스 테이블 목록, 타겟 테이블 목록)

**예외**: `IOException` - 파일 쓰기 작업 중 I/O 오류가 발생한 경우

**참조**: `TextWriter.writeTables(String, TablesInfo)`, `TablesInfo`

### buildOutputFileName(Path file)

입력 파일명으로부터 출력 파일명을 생성합니다.

**파라미터**:
- `file` - 입력 SQL 파일 경로

**반환값**: 출력 파일명 (예: `sample_sql_tables.txt`)

**동작 방식**:
1. `inputDir`를 기준으로 상대 경로 계산
2. 경로 구분자를 `/`로 통일
3. `.sql` 확장자를 `_sql_tables.txt`로 변경

### main(String[] args)

프로그램의 진입점입니다. 기본 설정으로 배치 작업을 실행합니다.

이 메서드는 다음 순서로 작업을 수행합니다:

1. `createDefault()`를 호출하여 기본 설정의 AppJob 인스턴스를 생성합니다.
2. `stepRead()`를 호출하여 SQL 파일 읽기 및 처리를 시작합니다.
3. 입력 디렉토리의 모든 .sql 파일에 대해 파싱 및 출력 작업이 순차적으로 실행됩니다.
4. 모든 파일 처리 완료 후 CSV 요약 파일(summary.csv 및 source_table_mapping.csv)이 생성됩니다.

**실행 방법**:
```bash
java service.queryParser.job.AppJob
```

**파라미터**:
- `args` - 명령줄 인자 (현재 사용되지 않음)

**참조**: `createDefault()`, `stepRead()`

## 커스텀 설정 사용 방법

기본 설정 대신 특정 경로나 문자셋을 사용하려면 별도의 런처 클래스를 작성하거나 main 메서드를 수정하여 다음과 같이 사용할 수 있습니다:

```java
Path customInput = Paths.get("D:", "custom", "input");
Path customOutput = Paths.get("D:", "custom", "output");

SqlReader customReader = new SqlReader(Charset.forName("UTF-8"));
FileParserProcessor customProcessor = FileParserProcessor.withDefaults();
TextWriter customWriter = new TextWriter(customOutput, Charset.forName("UTF-8"));

Path csvPath = customOutput.resolve("summary.csv");
CsvWriter customCsvWriter = new CsvWriter(csvPath, Charset.forName("UTF-8"));

Path sourceTableCsvPath = customOutput.resolve("source_table_mapping.csv");
SourceTableCsvWriter customSourceTableCsvWriter = new SourceTableCsvWriter(sourceTableCsvPath, Charset.forName("UTF-8"));

AppJob customJob = new AppJob(customInput, customReader, customProcessor, customWriter, customCsvWriter, customSourceTableCsvWriter);
customJob.stepRead();
```

## 출력 파일

AppJob을 실행하면 다음과 같은 파일들이 생성됩니다:

### 1. 개별 텍스트 파일
각 SQL 파일마다 하나의 텍스트 파일이 생성됩니다.

- **파일명 형식**: `{원본파일명}_sql_tables.txt`
- **위치**: `D:\11. Project\11. DB\BigQuery_out`
- **인코딩**: UTF-8
- **내용**: 소스 테이블 목록과 타겟 테이블 목록

**예시**:
```
[Source Tables]
DW.`회사목록`
DW.`코드목록`

[Target Tables]
BM.`회사`
BM.`공통코드`
```

### 2. summary.csv
모든 프로그램의 테이블 정보를 요약한 CSV 파일입니다.

- **파일명**: `summary.csv`
- **위치**: `D:\11. Project\11. DB\BigQuery_out`
- **인코딩**: UTF-8 (BOM 포함)
- **형식**: `Program,Source Tables,Target Tables`

**예시**:
```csv
Program,Source Tables,Target Tables
bq_dw_red_care_sales_01.sql,DW.`회사목록`; DW.`코드목록`,BM.`회사`; BM.`공통코드`
```

### 3. source_table_mapping.csv
소스테이블별로 어떤 프로그램들이 사용하는지 매핑한 CSV 파일입니다.

- **파일명**: `source_table_mapping.csv`
- **위치**: `D:\11. Project\11. DB\BigQuery_out`
- **인코딩**: UTF-8 (BOM 포함)
- **형식**: `Program,Source Table,Target Tables`

**예시**:
```csv
Program,Source Table,Target Tables
bq_dw_red_care_sales_01.sql,DW.`회사목록`,BM.`회사`; BM.`공통코드`
bq_dw_red_care_sales_02.sql,DW.`회사목록`,BM.`회사`
```

## 처리 흐름도

```
main()
  ↓
createDefault()
  ↓
stepRead()
  ↓
SqlReader.run(inputDir, handler)
  ↓
각 .sql 파일마다:
  ↓
handleFile(file, sql)
  ↓
  ├─ stepParse(sql) → TablesInfo
  ├─ stepWrite(file, info) → 텍스트 파일 생성
  ├─ csvWriter.addRecord(fileName, info)
  └─ sourceTableCsvWriter.addRecord(fileName, info)
  ↓
모든 파일 처리 완료 후:
  ↓
  ├─ csvWriter.write() → summary.csv 생성
  └─ sourceTableCsvWriter.write() → source_table_mapping.csv 생성
```

## 콘솔 출력 예시

```
========================================
Starting SQL file processing...
Input directory: D:\11. Project\11. DB\BigQuery
========================================
========================================
CSV file saved successfully.
Total records: 3
========================================
========================================
Source table mapping CSV file saved successfully.
Total source tables: 5
========================================
```

## 관련 클래스

- **SqlReader**: SQL 파일 읽기 담당
- **FileParserProcessor**: SQL 파싱 및 테이블 추출 담당
- **TextWriter**: 개별 텍스트 파일 쓰기 담당
- **CsvWriter**: 프로그램별 CSV 파일 쓰기 담당
- **SourceTableCsvWriter**: 소스테이블별 CSV 파일 쓰기 담당
- **TablesInfo**: 테이블 정보 저장 VO 객체

## 참고 문서

- [AppStepJob.md](./AppStepJob.md) - STEP별 테이블 추출
- [SqlReader.md](./SqlReader.md) - SQL 파일 읽기 상세
- [source-table-csv-writer.md](./source-table-csv-writer.md) - 소스테이블 매핑 CSV 상세
- [AppJob-vs-SpringBatch.md](./AppJob-vs-SpringBatch.md) - Spring Batch와의 비교

