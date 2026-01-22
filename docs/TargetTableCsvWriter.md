# TargetTableCsvWriter 클래스 설명

## 개요
`TargetTableCsvWriter`는 타겟 테이블을 기준으로 프로그램과 소스 테이블 간의 매핑 관계를 CSV 파일로 출력하는 클래스입니다.

## 패키지 경로
```
service.queryParser.writer.TargetTableCsvWriter
```

## 주요 기능
1. **타겟 테이블 중심의 매핑**: 각 타겟 테이블이 어떤 프로그램에서 사용되고, 어떤 소스 테이블들로부터 데이터를 받는지를 추적
2. **중복 제거**: TreeMap과 TreeSet을 사용하여 자동으로 정렬되고 중복이 제거된 데이터 관리
3. **CSV 출력**: UTF-8 BOM 지원 및 CSV 이스케이프 처리를 통한 안전한 파일 출력

## 클래스 구조

### 필드
```java
private final Path outputPath;              // CSV 파일 출력 경로
private final Charset charset;              // 파일 인코딩 (UTF-8 권장)
private final Map<String, TableMapping> tableMappings;  // 타겟 테이블별 매핑 정보
```

### 생성자
```java
public TargetTableCsvWriter(Path outputPath, Charset charset)
```
- **outputPath**: CSV 파일이 저장될 경로
- **charset**: 파일 인코딩 (일반적으로 UTF-8 사용)

## 주요 메서드

### 1. addRecord()
```java
public void addRecord(String fileName, TablesInfo tablesInfo)
```
**목적**: SQL 파일 하나에서 추출된 테이블 정보를 누적

**처리 과정**:
1. TablesInfo에서 소스 테이블 집합과 타겟 테이블 집합을 가져옴
2. 각 타겟 테이블마다:
   - 타겟 테이블이 tableMappings에 없으면 새로 생성
   - 해당 타겟 테이블의 TableMapping에 프로그램명과 소스 테이블들을 추가

**매핑 관계**:
- 키: 타겟 테이블명
- 값: TableMapping (프로그램 → 소스 테이블들)

### 2. write()
```java
public void write() throws IOException
```
**목적**: 누적된 데이터를 CSV 파일로 저장

**처리 과정**:
1. 출력 디렉토리가 없으면 생성
2. BufferedWriter로 파일 열기
3. UTF-8 인코딩이면 BOM(\ufeff) 추가 (Excel 한글 깨짐 방지)
4. CSV 헤더 작성: "Program,Target Table,Source Tables"
5. 각 타겟 테이블별로 매핑 정보 출력
6. 버퍼 플러시 및 파일 닫기

**CSV 형식**:
```
Program,Target Table,Source Tables
"program1.sql","DM.일별매출","DW.매출; DW.상품"
"program2.sql","DM.일별매출","DW.고객; DW.주문"
```

### 3. writeMapping()
```java
private void writeMapping(BufferedWriter writer, TableMapping mapping) throws IOException
```
**목적**: 하나의 타겟 테이블에 대한 모든 프로그램 매핑 출력

**처리 과정**:
1. 타겟 테이블명 가져오기
2. 프로그램별로 순회:
   - 프로그램명
   - 타겟 테이블명
   - 해당 프로그램의 소스 테이블들을 "; "로 연결
3. CSV 이스케이프 처리 후 한 줄로 출력

### 4. joinTables()
```java
private String joinTables(Set<String> tables)
```
**목적**: 여러 테이블명을 하나의 문자열로 결합

**동작**:
- 입력: {"DW.매출", "DW.상품", "DW.고객"}
- 출력: "DW.매출; DW.상품; DW.고객"
- 구분자: "; " (세미콜론 + 공백)

**특징**:
- null이나 빈 Set이면 빈 문자열 반환
- StringBuilder 사용으로 효율적인 문자열 연결

### 5. escapeCsv()
```java
private String escapeCsv(String value)
```
**목적**: CSV 형식에 맞게 값을 이스케이프 처리

**처리 규칙**:
1. null이면 빈 문자열 반환
2. 특수 문자(쉼표, 따옴표, 개행 등) 포함 여부 확인
3. 포함되어 있으면:
   - 내부의 따옴표(")를 두 개("")로 변환
   - 전체를 따옴표로 감싸기
4. 포함되지 않으면 원본 그대로 반환

**예시**:
- `"DM.매출"` → `"DM.매출"` (그대로)
- `"DM.매출,2024"` → `"\"DM.매출,2024\""`

### 6. getTableCount()
```java
public int getTableCount()
```
**목적**: 전체 타겟 테이블 개수 반환

**용도**: 처리 완료 후 통계 정보 출력

## 내부 클래스: TableMapping

### 목적
하나의 타겟 테이블에 대한 모든 프로그램 매핑 정보를 관리

### 필드
```java
private final String targetTable;                      // 타겟 테이블명
private final Map<String, Set<String>> programMappings; // 프로그램 → 소스 테이블들
```

### 주요 메서드

#### addProgram()
```java
public void addProgram(String program, Set<String> sourceTables)
```
**처리 과정**:
1. 프로그램이 programMappings에 없으면 새 TreeSet 생성
2. 소스 테이블들을 해당 프로그램의 Set에 추가
3. TreeSet 사용으로 자동 정렬 및 중복 제거

## 사용 예시

### 1. 객체 생성
```java
Path outputPath = Paths.get("D:", "11. Project", "11. DB", "BigQuery_out", "target_table_mapping.csv");
TargetTableCsvWriter writer = new TargetTableCsvWriter(outputPath, StandardCharsets.UTF_8);
```

### 2. 데이터 추가
```java
TablesInfo info = processor.parse(sqlContent);
writer.addRecord("bq_dw_red_care_sales_01.sql", info);
```

### 3. CSV 파일 저장
```java
writer.write();
System.out.println("Total target tables: " + writer.getTableCount());
```

## AppJob에서의 통합

### createDefault() 메서드
```java
Path targetTableCsvPath = DEFAULT_OUTPUT_PATH.resolve("target_table_mapping.csv");
TargetTableCsvWriter targetTableCsvWriter = new TargetTableCsvWriter(targetTableCsvPath, Charset.forName("UTF-8"));
```

### handleFile() 메서드
```java
String fileName = file.getFileName().toString();
targetTableCsvWriter.addRecord(fileName, info);
```

### stepRead() 메서드
```java
try {
    targetTableCsvWriter.write();
    System.out.println("========================================");
    System.out.println("Target table mapping CSV file saved successfully.");
    System.out.println("Total target tables: " + targetTableCsvWriter.getTableCount());
    System.out.println("========================================");
} catch (IOException ex) {
    System.err.println("Failed to save target table mapping CSV file: " + ex.getMessage());
}
```

## 출력 파일 형식

### 파일명
`target_table_mapping.csv`

### 헤더
```
Program,Target Table,Source Tables
```

### 데이터 예시
```csv
Program,Target Table,Source Tables
"bq_dw_red_care_sales_01.sql","BM.`공통코드`","DW.`코드목록`"
"bq_dw_red_care_sales_01.sql","BM.`사용자`","DW.`원천사용자집계`"
"bq_dw_red_care_sales_01.sql","BM.`회사`","DW.`공통함수`; DW.`회사목록`"
"bq_dw_red_care_sales_02.sql","BM.`회사`","DW.`사무실`; DW.`회사목록`"
"bq_dw_red_care_sales_01.sql","DM.일별카드발급현황","DW.카드목록"
```

## SourceTableCsvWriter와의 차이점

### SourceTableCsvWriter
- **기준**: 소스 테이블
- **질문**: "이 소스 테이블이 어떤 프로그램에서 사용되고, 어떤 타겟 테이블에 데이터를 제공하는가?"
- **CSV 헤더**: `Program,Source Table,Target Tables`
- **사용 사례**: 소스 테이블의 영향 범위 분석, 소스 테이블 변경 시 영향도 파악

### TargetTableCsvWriter
- **기준**: 타겟 테이블
- **질문**: "이 타겟 테이블이 어떤 프로그램에서 만들어지고, 어떤 소스 테이블로부터 데이터를 받는가?"
- **CSV 헤더**: `Program,Target Table,Source Tables`
- **사용 사례**: 타겟 테이블의 데이터 출처 추적, 타겟 테이블 생성 로직 파악

## 데이터 구조

### 내부 저장 구조
```
tableMappings (TreeMap<String, TableMapping>)
├─ "BM.공통코드" → TableMapping
│  └─ programMappings (TreeMap<String, Set<String>>)
│     └─ "program1.sql" → ["DW.코드목록"]
├─ "BM.회사" → TableMapping
│  └─ programMappings
│     ├─ "program1.sql" → ["DW.회사목록", "DW.공통함수"]
│     └─ "program2.sql" → ["DW.회사목록", "DW.사무실"]
└─ "DM.일별카드발급현황" → TableMapping
   └─ programMappings
      └─ "program1.sql" → ["DW.카드목록"]
```

## 주요 특징

### 1. 자동 정렬
- TreeMap과 TreeSet 사용으로 테이블명과 프로그램명이 알파벳순으로 자동 정렬

### 2. 중복 제거
- Set 자료구조 사용으로 동일 테이블명 자동 중복 제거

### 3. UTF-8 BOM 지원
- Excel에서 한글이 깨지지 않도록 UTF-8 BOM 추가

### 4. CSV 표준 준수
- RFC 4180 CSV 표준에 따른 이스케이프 처리

### 5. 확장 가능한 구조
- 새로운 프로그램이나 테이블 추가가 용이한 구조

## 성능 고려사항

### 메모리 사용
- 모든 매핑 정보를 메모리에 유지
- 대용량 프로젝트의 경우 메모리 사용량 증가 가능

### 시간 복잡도
- addRecord(): O(log n) - TreeMap/TreeSet 삽입
- write(): O(n × m) - n개 테이블, 각 m개 프로그램

### 개선 방안
- 필요시 스트리밍 방식으로 변경 가능
- 배치 처리로 메모리 사용량 최적화 가능

## 관련 클래스
- `SourceTableCsvWriter`: 소스 테이블 기준 매핑 CSV 작성
- `CsvWriter`: 전체 프로그램별 테이블 요약 CSV 작성
- `CsvStepWriter`: 스텝별 테이블 정보 CSV 작성
- `TablesInfo`: 테이블 정보를 담는 VO 클래스
- `AppJob`: 전체 파싱 프로세스를 관리하는 Job 클래스

