# CSV 비교 시스템 (CsvCompareApp)

## 개요
외부 라이브러리 없이 순수 Java로 구현된 CSV 파일 비교 시스템입니다. 두 개의 CSV 파일을 읽어서 키(Key) 기반으로 행을 매칭하고, 차이점을 분석하여 결과를 CSV 파일로 출력합니다.

## 패키지 구조

```
service.csvCompare/
├── job/
│   └── CsvCompareApp.java          # 메인 진입점
├── io/
│   ├── CsvReader.java              # CSV 파일 읽기
│   └── CsvWriter.java              # CSV 파일 쓰기
├── process/
│   ├── KeyStrategy.java            # 키 생성 전략 인터페이스
│   ├── CompositeKeyStrategy.java   # 복합 키 구현
│   └── CsvComparator.java          # 테이블 비교 로직
└── model/
    ├── CsvTable.java               # CSV 테이블 모델
    ├── DataRow.java                # 데이터 행 모델
    ├── OutputRow.java              # 출력 행 모델
    └── ResultType.java             # 비교 결과 타입
```

---

## 주요 클래스 상세 설명

### 1. CsvCompareApp (메인 진입점)

**위치**: `service.csvCompare.job.CsvCompareApp`

#### 주요 기능
- CSV 파일 2개를 입력받아 비교 수행
- 키(Key) 컬럼 지정 (선택사항)
- 비교 결과를 CSV 파일로 출력

#### 실행 방법
```bash
java service.csvCompare.job.CsvCompareApp <file1.csv> <file2.csv> [key1,key2,...]
```

**파라미터**:
- `file1.csv`: 첫 번째 CSV 파일 경로
- `file2.csv`: 두 번째 CSV 파일 경로
- `key1,key2,...`: (선택) 비교에 사용할 키 컬럼명 (콤마로 구분)

**예시**:
```bash
# 첫 번째 헤더를 키로 사용 (자동)
java service.csvCompare.job.CsvCompareApp data1.csv data2.csv

# ID 컬럼을 키로 사용
java service.csvCompare.job.CsvCompareApp data1.csv data2.csv ID

# ID와 DATE 복합키로 사용
java service.csvCompare.job.CsvCompareApp data1.csv data2.csv ID,DATE
```

#### 주요 로직

1. **파일 검증**
   - 입력 파일 존재 여부 확인
   - 파일 읽기 권한 확인

2. **키 전략 결정**
   - 사용자가 키를 지정한 경우: 지정된 키 사용
   - 키 미지정: 첫 번째 헤더 컬럼을 키로 사용
   - 양쪽 파일에 모두 키 컬럼이 존재하는지 검증

3. **CSV 파일 읽기**
   - CsvReader를 통해 두 파일을 CsvTable 객체로 변환

4. **헤더 통합**
   - 두 파일의 헤더를 합집합으로 통합 (file1 순서 우선)

5. **비교 수행**
   - CsvComparator를 통해 행별 비교 수행
   - 결과: OutputRow 리스트

6. **결과 출력**
   - 출력 파일명: `result_<원본파일명>`
   - 출력 위치: 첫 번째 파일과 동일한 디렉토리
   - 추가 컬럼: `RESULT`, `DIFF_DETAIL`

#### 출력 결과 예시

| ID | NAME | AGE | RESULT | DIFF_DETAIL |
|----|------|-----|--------|-------------|
| 1  | John | 25  | MATCHED | |
| 2  | Jane | 30  | MISMATCHED | AGE: '28' -> '30' |
| 3  | Bob  | 35  | ONLY_IN_FILE1 | |
| 4  | Alice| 40  | ONLY_IN_FILE2 | |

---

### 2. CsvReader (CSV 파일 읽기)

**위치**: `service.csvCompare.io.CsvReader`

#### 주요 기능
- RFC 4180 표준을 준수하는 CSV 파싱
- UTF-8 인코딩 지원
- 따옴표, 이스케이프, 개행 처리

#### 주요 메서드

##### `read(Path path): CsvTable`
CSV 파일을 읽어 CsvTable 객체로 변환

**처리 과정**:
1. 파일을 UTF-8로 읽기
2. CSV 텍스트 파싱
3. 첫 줄을 헤더로 인식
4. 나머지 줄을 DataRow로 변환
5. CsvTable 생성 및 반환

##### `parseCsv(String s): List<List<String>>`
CSV 텍스트를 2차원 리스트로 파싱

**지원 기능**:
- 따옴표(") 필드 인용 처리
- 이중 따옴표("") 이스케이프 처리
- 콤마(,) 구분자 처리
- CRLF, LF 개행 처리
- 필드 내 개행 문자 지원

**예시**:
```csv
ID,NAME,DESCRIPTION
1,John,"Hello, World"
2,Jane,"Line1
Line2"
3,Bob,"Quote: ""test"""
```

---

### 3. CsvWriter (CSV 파일 쓰기)

**위치**: `service.csvCompare.io.CsvWriter`

#### 주요 기능
- RFC 4180 표준 CSV 생성
- 필드 인용 및 이스케이프
- UTF-8 인코딩

#### 주요 메서드

##### `write(Path outPath, List<String> headers, List<OutputRow> rows): void`
헤더와 OutputRow 리스트를 CSV 파일로 작성

**처리 과정**:
1. 출력 디렉토리 생성 (필요시)
2. 헤더 행 작성
3. 각 OutputRow를 필드 리스트로 변환하여 작성
4. CRLF(\r\n) 개행 사용

##### `writeRow(BufferedWriter bw, List<String> fields): void`
한 행을 CSV 형식으로 작성

##### `writeField(BufferedWriter bw, String s): void`
필드 값을 인용/이스케이프 처리하여 작성

**인용 규칙**:
- 콤마(,) 포함 시 인용
- 따옴표(") 포함 시 인용
- 개행(\n, \r) 포함 시 인용
- 따옴표는 이중으로 이스케이프 ("" -> """")

---

### 4. CsvComparator (테이블 비교 로직)

**위치**: `service.csvCompare.process.CsvComparator`

#### 주요 기능
- 두 CSV 테이블을 키 기반으로 비교
- 헤더 합집합 생성
- 차이점 상세 분석

#### 주요 메서드

##### `unifyHeaders(List<String> h1, List<String> h2): List<String>`
두 파일의 헤더를 합집합으로 통합

**규칙**:
- file1의 헤더 순서 우선
- file2에만 있는 컬럼은 뒤에 추가
- 중복 제거 (LinkedHashSet 사용)

##### `compareTables(CsvTable t1, CsvTable t2, KeyStrategy keyStrategy, List<String> unifiedHeaders): List<OutputRow>`
두 테이블을 비교하여 OutputRow 리스트 반환

**처리 과정**:
1. 각 테이블을 키 기반 Map으로 변환
2. 모든 키의 합집합 생성
3. 각 키에 대해:
   - 양쪽 파일에 모두 존재: 값 비교
     - 모든 컬럼이 일치: `MATCHED`
     - 차이 발견: `MISMATCHED` + 차이점 상세 정보
   - file1에만 존재: `ONLY_IN_FILE1`
   - file2에만 존재: `ONLY_IN_FILE2`

##### `toKeyedMap(CsvTable t, KeyStrategy keyStrategy, String tag): Map<String, DataRow>`
CsvTable을 키-행 매핑으로 변환

**중복 검증**:
- 키 중복 발견 시 IllegalStateException 발생
- 어느 파일에서 발생했는지 태그로 표시

##### `buildDiffDetail(List<String> cols, DataRow r1, DataRow r2): String`
차이가 있는 컬럼의 상세 정보 생성

**형식**: `컬럼명: '값1' -> '값2'; 컬럼명2: '값3' -> '값4'`

**예시**: `AGE: '28' -> '30'; CITY: 'Seoul' -> 'Busan'`

---

### 5. KeyStrategy (키 생성 전략)

**위치**: `service.csvCompare.process.KeyStrategy`

#### 인터페이스 정의
```java
public interface KeyStrategy {
    String buildKey(Map<String, String> rowValues);
}
```

#### 목적
- 행을 고유하게 식별하기 위한 키 생성 방법 정의
- 전략 패턴(Strategy Pattern) 적용
- 다양한 키 생성 방식 확장 가능

---

### 6. CompositeKeyStrategy (복합 키 구현)

**위치**: `service.csvCompare.process.CompositeKeyStrategy`

#### 주요 기능
- 여러 컬럼을 조합하여 복합 키 생성
- Unit Separator(0x1D) 문자로 값 연결
- null 안전 처리

#### 생성자
```java
public CompositeKeyStrategy(List<String> keyColumns)
```

**검증**:
- 키 컬럼 리스트가 null이거나 비어있으면 예외 발생

#### 주요 메서드

##### `buildKey(Map<String, String> rowValues): String`
지정된 컬럼들의 값을 연결하여 키 생성

**예시**:
```java
keyColumns = ["ID", "DATE"]
rowValues = {ID: "1001", DATE: "2025-12-07", NAME: "John"}
결과: "1001\u001D2025-12-07"
```

**특징**:
- SEP 문자(0x1D): ASCII Unit Separator 사용하여 값 충돌 최소화
- null 값은 빈 문자열("")로 처리
- Stream API 활용

---

### 7. CsvTable (테이블 모델)

**위치**: `service.csvCompare.model.CsvTable`

#### 구조
```java
public class CsvTable {
    private final List<String> headers;
    private final List<DataRow> rows;
}
```

#### 주요 기능
- CSV 파일의 논리적 표현
- 헤더와 데이터 행 분리 관리
- Immutable 컬렉션 반환 (보호)

#### 메서드
- `getHeaders()`: 헤더 리스트 반환 (읽기 전용)
- `getRows()`: DataRow 리스트 반환 (읽기 전용)

---

### 8. DataRow (데이터 행 모델)

**위치**: `service.csvCompare.model.DataRow`

#### 구조
```java
public class DataRow {
    private final Map<String, String> values;
}
```

#### 주요 기능
- 한 행의 데이터를 컬럼명-값 매핑으로 관리
- LinkedHashMap 사용으로 삽입 순서 보존
- Immutable 맵 반환 (보호)

#### 메서드
- `getValues()`: 전체 값 맵 반환 (읽기 전용)
- `getValue(String column)`: 특정 컬럼의 값 반환

---

### 9. OutputRow (출력 행 모델)

**위치**: `service.csvCompare.model.OutputRow`

#### 구조
```java
public class OutputRow {
    private final Map<String, String> valuesInUnifiedHeaderOrder;
    private final ResultType resultType;
    private final String diffDetail;
}
```

#### 주요 기능
- 비교 결과를 포함한 출력용 행 표현
- 통합 헤더 순서로 값 저장
- 결과 타입과 차이점 상세 정보 포함

#### 메서드

##### `toOutputFields(): List<String>`
CSV 출력을 위한 필드 리스트 생성

**순서**:
1. 통합 헤더 순서대로 값들
2. RESULT (결과 타입)
3. DIFF_DETAIL (차이점 상세)

---

### 10. ResultType (비교 결과 타입)

**위치**: `service.csvCompare.model.ResultType`

#### Enum 정의
```java
public enum ResultType {
    MATCHED,          // 완전 일치
    MISMATCHED,       // 값 불일치
    ONLY_IN_FILE1,    // file1에만 존재
    ONLY_IN_FILE2     // file2에만 존재
}
```

#### 각 타입의 의미

| 타입 | 의미 | 설명 |
|------|------|------|
| `MATCHED` | 완전 일치 | 키가 양쪽 파일에 존재하고 모든 컬럼 값이 동일 |
| `MISMATCHED` | 값 불일치 | 키가 양쪽 파일에 존재하지만 일부 컬럼 값이 다름 |
| `ONLY_IN_FILE1` | file1에만 존재 | 해당 키가 첫 번째 파일에만 존재 |
| `ONLY_IN_FILE2` | file2에만 존재 | 해당 키가 두 번째 파일에만 존재 |

---

## 실행 흐름도

```
[사용자 실행]
    ↓
[CsvCompareApp.main()]
    ↓
[인자 검증 및 키 전략 결정]
    ↓
[CsvReader.read(file1)] → [CsvTable t1]
    ↓
[CsvReader.read(file2)] → [CsvTable t2]
    ↓
[CsvComparator.unifyHeaders()] → [통합 헤더]
    ↓
[CsvComparator.compareTables()]
    ├─ [각 행을 키로 매핑]
    ├─ [키 합집합 생성]
    └─ [각 키별로 비교]
        ├─ 양쪽 존재 → 값 비교
        ├─ file1만 → ONLY_IN_FILE1
        └─ file2만 → ONLY_IN_FILE2
    ↓
[OutputRow 리스트 생성]
    ↓
[CsvWriter.write(결과파일)]
    ↓
[완료 메시지 출력]
```

---

## 특징 및 장점

### 1. 외부 라이브러리 의존성 없음
- 순수 JDK만으로 동작
- 네트워크 격리 환경에서도 사용 가능
- 별도 설치나 설정 불필요

### 2. RFC 4180 표준 준수
- 표준 CSV 형식 완벽 지원
- Excel 등 다양한 도구와 호환

### 3. 유연한 키 전략
- Strategy Pattern으로 확장 가능
- 단일 키, 복합 키 모두 지원
- 커스텀 키 전략 추가 가능

### 4. 상세한 차이점 분석
- 어느 컬럼이 다른지 명확히 표시
- 변경 전후 값 비교
- 추가/삭제된 행 식별

### 5. Immutable 설계
- 데이터 보호 및 안정성
- Thread-safe 가능성
- 예기치 않은 변경 방지

### 6. 명확한 오류 처리
- 파일 읽기 오류
- 키 중복 감지
- 키 컬럼 존재 검증
- 의미 있는 오류 메시지

---

## 사용 예시

### 예시 1: 단일 키 비교

**file1.csv**:
```csv
ID,NAME,AGE
1,John,25
2,Jane,28
3,Bob,35
```

**file2.csv**:
```csv
ID,NAME,AGE
1,John,25
2,Jane,30
4,Alice,40
```

**실행**:
```bash
java service.csvCompare.job.CsvCompareApp file1.csv file2.csv ID
```

**result_file1.csv**:
```csv
ID,NAME,AGE,RESULT,DIFF_DETAIL
1,John,25,MATCHED,
2,Jane,30,MISMATCHED,AGE: '28' -> '30'
3,Bob,35,ONLY_IN_FILE1,
4,Alice,40,ONLY_IN_FILE2,
```

### 예시 2: 복합 키 비교

**sales1.csv**:
```csv
DATE,STORE_ID,PRODUCT,AMOUNT
2025-12-01,S001,Apple,100
2025-12-01,S002,Banana,200
2025-12-02,S001,Apple,150
```

**sales2.csv**:
```csv
DATE,STORE_ID,PRODUCT,AMOUNT
2025-12-01,S001,Apple,100
2025-12-01,S002,Banana,250
2025-12-03,S001,Orange,300
```

**실행**:
```bash
java service.csvCompare.job.CsvCompareApp sales1.csv sales2.csv DATE,STORE_ID,PRODUCT
```

**result_sales1.csv**:
```csv
DATE,STORE_ID,PRODUCT,AMOUNT,RESULT,DIFF_DETAIL
2025-12-01,S001,Apple,100,MATCHED,
2025-12-01,S002,Banana,250,MISMATCHED,AMOUNT: '200' -> '250'
2025-12-02,S001,Apple,150,ONLY_IN_FILE1,
2025-12-03,S001,Orange,300,ONLY_IN_FILE2,
```

---

## 제한사항

1. **메모리 제약**: 전체 파일을 메모리에 로드하므로 대용량 파일 처리 시 주의 필요
2. **인코딩**: UTF-8만 지원
3. **데이터 타입**: 모든 값을 문자열로 처리 (숫자 비교 시 "1"과 "01"은 다름)
4. **공백 처리**: 값 비교 시 trim() 적용으로 앞뒤 공백 무시

---

## 확장 가능성

### 1. 새로운 키 전략 추가
```java
public class RowNumberKeyStrategy implements KeyStrategy {
    @Override
    public String buildKey(Map<String, String> rowValues) {
        return String.valueOf(rowIndex++);
    }
}
```

### 2. 커스텀 비교 로직
```java
public class NumericComparator extends CsvComparator {
    @Override
    protected boolean isEqual(String a, String b, String columnName) {
        if (isNumericColumn(columnName)) {
            return Double.parseDouble(a) == Double.parseDouble(b);
        }
        return super.isEqual(a, b, columnName);
    }
}
```

### 3. 다양한 출력 형식
- JSON 출력
- HTML 리포트
- Excel 파일 생성

---

## 관련 문서

- [AppJob 시스템](./AppJob.md)
- [TableParser 시스템](./table-pattern-system.md)
- [로거 가이드](./logger-guide.md)

---

## 버전 정보

- 최초 작성: 2025-12-07
- JDK 버전: 8 이상
- 외부 의존성: 없음

