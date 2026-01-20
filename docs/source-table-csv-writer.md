# SourceTableCsvWriter

## 개요

`SourceTableCsvWriter`는 소스테이블 중심으로 프로그램 매핑 정보를 CSV 형식으로 저장하는 클래스입니다.

기존 `CsvWriter`가 프로그램(파일) 중심으로 소스/타겟 테이블을 기록하는 반면, `SourceTableCsvWriter`는 **소스테이블 중심**으로 어떤 프로그램들이 해당 테이블을 사용하는지 역방향 매핑을 생성합니다.

## 주요 기능

### 1. 소스테이블 중심 매핑
- 각 소스테이블이 어떤 프로그램(파일)에서 사용되는지 추적
- 하나의 소스테이블이 여러 프로그램에서 사용될 경우 N개의 행으로 표현
- 각 프로그램별로 해당 소스테이블과 연관된 타겟테이블 목록 포함

### 2. CSV 출력 형식

```csv
Program,Source Table,Target Tables
bq_dw_red_care_sales_01.sql,DW.`회사목록`,BM.`회사`; BM.`공통코드`
bq_dw_red_care_sales_02.sql,DW.`회사목록`,BM.`회사`
bq_dw_red_care_sales_01.sql,DW.`코드목록`,BM.`공통코드`
bq_dw_red_care_sales_01.sql,DW.`사무실`,BM.`회사`
```

### 3. 정렬 기능
- 소스테이블명 기준으로 오름차순 정렬 (TreeMap 사용)
- 프로그램명 기준으로 오름차순 정렬 (TreeMap 사용)
- 타겟테이블명 기준으로 오름차순 정렬 (TreeSet 사용)

### 4. 한글 지원
- UTF-8 BOM 추가로 엑셀에서 한글이 깨지지 않도록 처리
- 쉼표, 따옴표, 줄바꿈이 포함된 경우 자동 이스케이프 처리

## 클래스 구조

```java
public class SourceTableCsvWriter {
    private final Path outputPath;
    private final Charset charset;
    private final Map<String, TableMapping> tableMappings;
    
    // 주요 메소드
    public void addRecord(String fileName, TablesInfo tablesInfo)
    public void write() throws IOException
    public int getTableCount()
    
    // 내부 클래스
    private static class TableMapping {
        private final String sourceTable;
        private final Map<String, Set<String>> programMappings;
    }
}
```

## 사용 방법

### 1. 인스턴스 생성

```java
Path outputPath = Paths.get("D:", "11. Project", "11. DB", "BigQuery_out", "source_table_mapping.csv");
Charset charset = Charset.forName("UTF-8");
SourceTableCsvWriter writer = new SourceTableCsvWriter(outputPath, charset);
```

### 2. 레코드 추가

```java
// 파일별로 테이블 정보 추가
String fileName = "bq_dw_red_care_sales_01.sql";
TablesInfo tablesInfo = processor.parse(sqlContent);
writer.addRecord(fileName, tablesInfo);
```

### 3. CSV 파일 저장

```java
writer.write();
System.out.println("Total source tables: " + writer.getTableCount());
```

## AppJob과의 통합

`AppJob` 클래스는 두 개의 CSV Writer를 사용합니다:

1. **CsvWriter**: 프로그램 중심 CSV (summary.csv)
   - 파일명, 소스테이블들, 타겟테이블들

2. **SourceTableCsvWriter**: 소스테이블 중심 CSV (source_table_mapping.csv)
   - 프로그램, 소스테이블, 타겟테이블들

```java
public static AppJob createDefault() {
    // ... 기타 초기화 ...
    Path csvPath = DEFAULT_OUTPUT_PATH.resolve("summary.csv");
    CsvWriter csvWriter = new CsvWriter(csvPath, Charset.forName("UTF-8"));
    
    Path sourceTableCsvPath = DEFAULT_OUTPUT_PATH.resolve("source_table_mapping.csv");
    SourceTableCsvWriter sourceTableCsvWriter = new SourceTableCsvWriter(sourceTableCsvPath, Charset.forName("UTF-8"));
    
    return new AppJob(DEFAULT_INPUT_PATH, reader, processor, writer, csvWriter, sourceTableCsvWriter);
}
```

## 출력 예시

### 입력 (SQL 파일들)

**bq_dw_red_care_sales_01.sql**
```sql
INSERT INTO BM.`회사`
SELECT * FROM DW.`회사목록`, DW.`사무실`
WHERE ...;

INSERT INTO BM.`공통코드`
SELECT * FROM DW.`코드목록`
WHERE ...;
```

**bq_dw_red_care_sales_02.sql**
```sql
INSERT INTO BM.`회사`
SELECT * FROM DW.`회사목록`
WHERE ...;
```

### 출력 (source_table_mapping.csv)

| Program | Source Table | Target Tables |
|---------|-------------|---------------|
| bq_dw_red_care_sales_01.sql | DW.`회사목록` | BM.`공통코드`; BM.`회사` |
| bq_dw_red_care_sales_01.sql | DW.`코드목록` | BM.`공통코드` |
| bq_dw_red_care_sales_01.sql | DW.`사무실` | BM.`회사` |
| bq_dw_red_care_sales_02.sql | DW.`회사목록` | BM.`회사` |

## 데이터 흐름

```
SQL 파일들
    ↓
SqlReader (STEP1)
    ↓
FileParserProcessor (STEP2)
    ↓
TablesInfo 객체
    ↓
SourceTableCsvWriter.addRecord()
    ↓
내부 Map에 소스테이블별로 프로그램 매핑 누적
    ↓
모든 파일 처리 완료 후
    ↓
SourceTableCsvWriter.write()
    ↓
source_table_mapping.csv 생성
```

## 장점

### 1. 영향도 분석 용이
- 특정 소스테이블을 수정하면 어떤 프로그램들이 영향받는지 즉시 확인 가능
- 테이블 삭제 전 사용처 파악 용이

### 2. 의존성 추적
- 소스테이블 → 프로그램 → 타겟테이블의 연결 관계 명확화
- 데이터 흐름 역추적 가능

### 3. 리팩토링 지원
- 테이블 통합/분리 시 영향 범위 분석
- 테이블명 변경 시 수정 대상 프로그램 목록 확보

### 4. 문서화
- 소스테이블 사용 현황을 자동으로 문서화
- 엑셀에서 바로 열어 필터링 및 분석 가능

## 내부 동작 원리

### TableMapping 클래스

```java
private static class TableMapping {
    private final String sourceTable;
    private final Map<String, Set<String>> programMappings;  // 프로그램 -> 타겟테이블들
    
    public void addProgram(String program, Set<String> targetTables) {
        programMappings.putIfAbsent(program, new TreeSet<>());
        if (targetTables != null) {
            programMappings.get(program).addAll(targetTables);
        }
    }
}
```

### 매핑 누적 과정

1. `addRecord()` 호출 시 각 소스테이블에 대해:
   - `tableMappings` Map에 소스테이블이 없으면 새로 생성
   - 해당 소스테이블의 `TableMapping` 객체에 프로그램과 타겟테이블들 추가

2. `write()` 호출 시:
   - 소스테이블 기준으로 정렬 (TreeMap)
   - 각 소스테이블별로 프로그램 기준 정렬 (TreeMap)
   - 각 (소스테이블, 프로그램) 조합마다 CSV 행 생성
   - 타겟테이블들은 세미콜론(;)으로 구분하여 하나의 셀에 표시

## 확장 가능성

현재는 소스테이블 → 프로그램 → 타겟테이블 매핑만 제공하지만, 향후 다음과 같은 확장이 가능합니다:

1. **타겟테이블 중심 매핑**: 타겟테이블 → 프로그램 → 소스테이블들
2. **컬럼 레벨 매핑**: 소스 컬럼 → 프로그램 → 타겟 컬럼
3. **통계 정보 추가**: 참조 횟수, 최초/최종 사용 일자 등
4. **그래프 형식 출력**: Graphviz DOT 형식으로 데이터 흐름도 생성

## 참고

- 패키지: `service.queryParser.writer`
- 관련 클래스: `CsvWriter`, `AppJob`, `TablesInfo`
- 출력 파일: `source_table_mapping.csv`

