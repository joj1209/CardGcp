# CsvConverter 클래스

## 📋 개요
CsvConverter는 Control-M 작업 정의 CSV 파일을 읽어서 수직 구조(행 기반)를 수평 구조(열 기반)로 피벗 변환하는 유틸리티 클래스입니다.

## 🎯 주요 기능

### 1. CSV 피벗 변환
- **입력**: 수직 구조 CSV (항목명이 행에 분산)
- **출력**: 수평 구조 CSV (항목명이 열로 변환)
- Control-M 작업 정의를 한 행에 모든 속성이 표시되도록 변환

### 2. 입출력 경로 설정
- 명령줄 아규먼트로 입출력 경로 지정 가능
- 기본값: `csv/CmJob.csv` → `output_result.csv`

### 3. Excel 호환성
- UTF-8 BOM 추가로 Excel에서 한글 깨짐 방지
- TIMEFROM/TIMEUNTIL 필드는 텍스트 형식으로 저장 (0000 → 0 변환 방지)

---

## 📊 데이터 구조 변환

### 입력 CSV 구조 (CmJob.csv)
```csv
APPLICATION_NM5,GROUP_NM4,JOBNAME_NM3,CTRM_항목명,CTRM_DATA
APP1,GRP1,JOB001,CMDLINE,/usr/bin/script.sh
APP1,GRP1,JOB001,DESCRIPTION,데이터 처리 작업
APP1,GRP1,JOB001,TIMEFROM,0800
APP1,GRP1,JOB001,TIMEUNTIL,1800
APP2,GRP2,JOB002,CMDLINE,/usr/bin/backup.sh
APP2,GRP2,JOB002,DESCRIPTION,백업 작업
```

### 출력 CSV 구조 (output_result.csv)
```csv
APPLICATION_NM5,GROUP_NM4,JOBNAME_NM3,CMDLINE,DAYSCAL,DESCRIPTION,INCOND,MONTH,TIMEFROM,TIMEUNTIL
APP1,GRP1,JOB001,/usr/bin/script.sh,,데이터 처리 작업,,,"0800","1800"
APP2,GRP2,JOB002,/usr/bin/backup.sh,,백업 작업,,,""
```

---

## 🔧 주요 메서드

### 1. `main(String[] args)`
**설명**: 프로그램 진입점, 아규먼트 처리 및 변환 실행

**아규먼트**:
- `args[0]`: 입력 CSV 파일 경로 (선택)
- `args[1]`: 출력 CSV 파일 경로 (선택)

**사용 예시**:
```bash
# 기본값 사용
java service.CsvConverter

# 입력 경로만 지정
java service.CsvConverter data/input.csv

# 입출력 경로 모두 지정
java service.CsvConverter data/input.csv data/output.csv
```

**처리 흐름**:
1. 아규먼트 파싱 (없으면 기본값 사용)
2. 사용법 출력 (아규먼트 없을 때)
3. CsvConverter 객체 생성
4. convert() 메서드 호출

---

### 2. `convert()`
**설명**: CSV 변환 전체 프로세스 오케스트레이션

**처리 단계**:
1. **readCsv()**: 입력 CSV 파일 읽기
2. **pivotData()**: 데이터 피벗 변환
3. **writeCsv()**: 결과를 CSV 파일로 저장
4. 완료 메시지 및 통계 출력

**예외 처리**:
- IOException 등 예외 발생 시 에러 메시지 출력
- 스택 트레이스 출력으로 디버깅 지원

---

### 3. `readCsv(String filePath)`
**설명**: CSV 파일을 읽어서 2차원 배열로 반환

**파라미터**:
- `filePath`: 읽을 CSV 파일 경로

**반환값**:
- `List<String[]>`: 각 행이 String 배열로 구성된 리스트

**처리 로직**:
1. UTF-8 인코딩으로 파일 열기
2. 첫 번째 행(헤더) 스킵
3. 각 행을 `parseCsvLine()` 메서드로 파싱
4. 최소 5개 컬럼(APPLICATION_NM5, GROUP_NM4, JOBNAME_NM3, CTRM_항목명, CTRM_DATA) 확인
5. 유효한 행만 리스트에 추가

**예외**:
- `IOException`: 파일 읽기 실패 시 발생

---

### 4. `parseCsvLine(String line)`
**설명**: CSV 행을 파싱하여 컬럼 배열로 분리 (따옴표 내 쉼표 처리)

**파라미터**:
- `line`: 파싱할 CSV 행 문자열

**반환값**:
- `String[]`: 파싱된 컬럼 배열

**처리 로직**:
1. 문자 단위로 순회
2. 따옴표(") 만나면 `inQuotes` 플래그 토글
3. 따옴표 밖의 쉼표(,)를 구분자로 인식
4. 따옴표 안의 쉼표는 데이터로 처리
5. 각 컬럼은 trim() 처리

**예시**:
```
입력: APP1,GRP1,"JOB,001",CMDLINE,/usr/bin/script.sh
출력: ["APP1", "GRP1", "JOB,001", "CMDLINE", "/usr/bin/script.sh"]
```

---

### 5. `pivotData(List<String[]> rawData)`
**설명**: 수직 구조 데이터를 수평 구조로 피벗 변환

**파라미터**:
- `rawData`: 원본 CSV 데이터 (2차원 배열)

**반환값**:
- `List<Map<String, String>>`: 피벗된 데이터 (각 행이 Map)

**처리 로직**:

#### Step 1: 복합 키 생성
```java
String compositeKey = application + "|" + group + "|" + jobName;
```
- APPLICATION_NM5, GROUP_NM4, JOBNAME_NM3를 조합하여 고유 키 생성
- 같은 작업의 여러 속성을 하나의 행으로 그룹화

#### Step 2: 데이터 그룹화
```java
pivotMap.putIfAbsent(compositeKey, new LinkedHashMap<>());
Map<String, String> rowMap = pivotMap.get(compositeKey);
```
- 복합 키별로 Map 생성 (없으면 새로 생성)
- LinkedHashMap 사용으로 삽입 순서 유지

#### Step 3: 기본 컬럼 저장
```java
rowMap.put("APPLICATION_NM5", application);
rowMap.put("GROUP_NM4", group);
rowMap.put("JOBNAME_NM3", jobName);
```

#### Step 4: 동적 컬럼 생성
```java
rowMap.put(ctrmKey, ctrmData);
```
- CTRM_항목명을 컬럼명으로, CTRM_DATA를 값으로 저장
- 예: `CMDLINE` → `/usr/bin/script.sh`
- 예: `TIMEFROM` → `0800`

**데이터 변환 예시**:

**입력** (수직 구조):
```
APP1, GRP1, JOB001, CMDLINE, /usr/bin/script.sh
APP1, GRP1, JOB001, DESCRIPTION, 데이터 처리
APP1, GRP1, JOB001, TIMEFROM, 0800
```

**출력** (수평 구조):
```
{
  "APPLICATION_NM5": "APP1",
  "GROUP_NM4": "GRP1",
  "JOBNAME_NM3": "JOB001",
  "CMDLINE": "/usr/bin/script.sh",
  "DESCRIPTION": "데이터 처리",
  "TIMEFROM": "0800"
}
```

---

### 6. `writeCsv(String filePath, List<Map<String, String>> data)`
**설명**: 피벗된 데이터를 CSV 파일로 저장

**파라미터**:
- `filePath`: 출력 CSV 파일 경로
- `data`: 피벗된 데이터

**처리 로직**:

#### Step 1: 컬럼 정의 (10개 고정)
```java
allColumns = [
  "APPLICATION_NM5",  // 애플리케이션명
  "GROUP_NM4",        // 그룹명
  "JOBNAME_NM3",      // 작업명
  "CMDLINE",          // 실행 명령
  "DAYSCAL",          // 실행 일정
  "DESCRIPTION",      // 작업 설명
  "INCOND",           // 실행 조건
  "MONTH",            // 실행 월
  "TIMEFROM",         // 시작 시간
  "TIMEUNTIL"         // 종료 시간
]
```

#### Step 2: BOM 추가
```java
bw.write('\ufeff');
```
- UTF-8 BOM (Byte Order Mark) 추가
- Excel에서 UTF-8 파일을 올바르게 인식하도록 지원
- 한글 깨짐 방지

#### Step 3: 헤더 작성
```java
bw.write(String.join(",", allColumns));
```

#### Step 4: 데이터 작성
각 행을 순회하며:

1. **값 가져오기**: 컬럼명으로 값 조회 (없으면 빈 문자열)
2. **TIMEFROM/TIMEUNTIL 특별 처리**:
   ```java
   if (("TIMEFROM".equals(column) || "TIMEUNTIL".equals(column)) && !value.isEmpty()) {
       value = "=\"" + value + "\"";
   }
   ```
   - Excel에서 `0000` → `0`으로 자동 변환되는 것을 방지
   - `="0000"` 형식으로 저장하여 텍스트로 인식

3. **특수 문자 처리**:
   ```java
   if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
       value = "\"" + value.replace("\"", "\"\"") + "\"";
   }
   ```
   - 쉼표, 따옴표, 개행 포함 시 따옴표로 감싸기
   - 따옴표는 두 번 반복(`""`)으로 이스케이프

#### Step 5: 통계 출력
- 출력 파일 절대 경로
- 총 컬럼 수

**예외**:
- `IOException`: 파일 쓰기 실패 시 발생

---

## 🏗️ 클래스 구조

### 필드
```java
private static final String DEFAULT_INPUT_CSV = "csv/CmJob.csv";
private static final String DEFAULT_OUTPUT_CSV = "output_result.csv";
private final String inputCsv;
private final String outputCsv;
```

### 생성자
```java
public CsvConverter(String inputCsv, String outputCsv)
```
- 입출력 경로를 받아 객체 생성

---

## 📌 사용 시나리오

### 시나리오 1: 기본 사용
```bash
java service.CsvConverter
```
- 입력: `csv/CmJob.csv`
- 출력: `output_result.csv`

### 시나리오 2: 커스텀 경로
```bash
java service.CsvConverter data/cm_jobs.csv reports/result.csv
```
- 입력: `data/cm_jobs.csv`
- 출력: `reports/result.csv`

### 시나리오 3: 프로그래밍 방식
```java
CsvConverter converter = new CsvConverter("input.csv", "output.csv");
converter.convert();
```

---

## 🔍 데이터 처리 흐름

```
[입력 CSV 파일]
      ↓
readCsv() - UTF-8 읽기, 헤더 스킵, 행 파싱
      ↓
[2차원 배열: List<String[]>]
      ↓
pivotData() - 복합 키 생성, 그룹화, 피벗
      ↓
[Map 리스트: List<Map<String, String>>]
      ↓
writeCsv() - BOM 추가, 헤더 작성, 데이터 작성, 특수 처리
      ↓
[출력 CSV 파일]
```

---

## ⚙️ 주요 기술 특징

### 1. CSV 파싱
- **따옴표 처리**: 따옴표 안의 쉼표를 데이터로 인식
- **상태 기반 파싱**: `inQuotes` 플래그로 따옴표 내외 구분

### 2. 데이터 피벗
- **LinkedHashMap 사용**: 삽입 순서 유지
- **복합 키 전략**: 여러 컬럼 조합으로 고유 키 생성
- **동적 컬럼 생성**: CTRM_항목명을 컬럼으로 변환

### 3. Excel 호환성
- **UTF-8 BOM**: Excel의 한글 인코딩 문제 해결
- **텍스트 형식 강제**: `="0000"` 형식으로 숫자 자동 변환 방지
- **특수 문자 이스케이프**: CSV 표준에 맞는 처리

### 4. 견고성
- **예외 처리**: try-catch로 오류 처리
- **리소스 관리**: try-with-resources로 자동 닫기
- **데이터 검증**: 최소 컬럼 수 확인

---

## 🎓 설계 원칙

### 1. 단일 책임 원칙 (SRP)
- `readCsv()`: CSV 읽기만 담당
- `pivotData()`: 피벗 변환만 담당
- `writeCsv()`: CSV 쓰기만 담당

### 2. 의존성 역전 원칙 (DIP)
- 표준 Java API만 사용 (외부 라이브러리 의존성 없음)
- JDK 8 호환

### 3. 개방-폐쇄 원칙 (OCP)
- 고정된 10개 컬럼 구조
- 필요 시 컬럼 추가 가능한 구조

---

## 📊 출력 결과 예시

### 콘솔 출력
```
사용법: java service.CsvConverter [입력CSV경로] [출력CSV경로]
기본값 사용: 입력=csv/CmJob.csv, 출력=output_result.csv

출력 파일: D:\11. Project\02. Java\backend\CardGcp\output_result.csv
총 컬럼 수: 10
CSV 변환 완료: output_result.csv
총 50개의 행이 생성되었습니다.
```

### CSV 파일 내용
```csv
APPLICATION_NM5,GROUP_NM4,JOBNAME_NM3,CMDLINE,DAYSCAL,DESCRIPTION,INCOND,MONTH,TIMEFROM,TIMEUNTIL
APP1,GRP1,JOB001,/usr/bin/script.sh,,데이터 처리 작업,,,"0800","1800"
APP2,GRP2,JOB002,/usr/bin/backup.sh,,백업 작업,,,"2200","2359"
```

---

## 🐛 알려진 제약사항

1. **고정 컬럼 구조**: 10개 컬럼으로 고정 (확장 시 코드 수정 필요)
2. **메모리 사용**: 전체 파일을 메모리에 로드 (대용량 파일 주의)
3. **CSV 표준**: RFC 4180 완전 준수는 아님 (기본적인 파싱만 지원)
4. **TIMEFROM/TIMEUNTIL 특별 처리**: Excel 전용 (다른 도구에서는 `="0000"` 형식 그대로 표시)

---

## 🔧 확장 가능성

### 1. 동적 컬럼 지원
현재는 10개 컬럼 고정이지만, 동적으로 컬럼을 수집하도록 확장 가능:
```java
Set<String> allKeys = new LinkedHashSet<>();
for (Map<String, String> row : data) {
    allKeys.addAll(row.keySet());
}
```

### 2. 대용량 파일 처리
스트리밍 방식으로 변경하여 메모리 사용 최적화 가능

### 3. 다양한 인코딩 지원
생성자에 Charset 파라미터 추가

### 4. CSV 라이브러리 사용
Apache Commons CSV, OpenCSV 등으로 교체하여 표준 준수도 향상

---

## 📝 요약

CsvConverter는 Control-M 작업 정의를 수직 구조에서 수평 구조로 변환하는 특화된 유틸리티입니다. 
- **간단한 사용법**: 명령줄 아규먼트만으로 실행 가능
- **Excel 호환성**: UTF-8 BOM 및 텍스트 형식 강제로 Excel에서 바로 사용 가능
- **견고한 파싱**: 따옴표 내 쉼표 등 특수 상황 처리
- **명확한 구조**: 메서드별 단일 책임으로 유지보수 용이

---

## 📚 관련 문서
- [Control-M 작업 정의](https://www.bmc.com/guides/control-m.html)
- [CSV RFC 4180 표준](https://tools.ietf.org/html/rfc4180)
- [Java NIO 파일 처리](https://docs.oracle.com/javase/8/docs/api/java/nio/file/package-summary.html)

