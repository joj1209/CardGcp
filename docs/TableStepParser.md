# TableStepParser 클래스

## 개요
SQL 스크립트를 STEP별로 분할하여 각 STEP의 소스/타겟 테이블을 추출하는 클래스입니다.

## 주요 기능

### 1. STEP별 테이블 추출
SQL 스크립트를 STEP 단위로 분할하고 각 STEP의 소스/타겟 테이블을 추출합니다.

### 2. STEP 패턴 지원
다음과 같은 STEP 주석 패턴을 지원합니다:
- `/* STEP001 */`
- `/* STEP002 */`
- `-- STEP001`
- `-- STEP002`

## 클래스 구조

### 생성자
```java
// 기본 생성자
public TableStepParser()

// TableParser 주입 생성자
public TableStepParser(TableParser tableParser)
```

### 주요 메소드

#### 1. extractTablesByStep()
STEP별로 테이블을 추출합니다.

```java
public Map<String, TablesInfo> extractTablesByStep(String sql)
```

**반환값**: Map<STEP명, TablesInfo>
- Key: "STEP001", "STEP002" 등
- Value: 해당 STEP의 테이블 정보

**예시:**
```java
TableStepParser parser = new TableStepParser();
Map<String, TablesInfo> stepTables = parser.extractTablesByStep(sql);

// STEP001의 테이블 정보
TablesInfo step001 = stepTables.get("STEP001");
System.out.println("Sources: " + step001.getSources());
System.out.println("Targets: " + step001.getTargets());
```

#### 2. extractAllTables()
STEP 구분 없이 전체 SQL의 테이블을 추출합니다.

```java
public TablesInfo extractAllTables(String sql)
```

**예시:**
```java
TablesInfo allTables = parser.extractAllTables(sql);
System.out.println("All Sources: " + allTables.getSources());
System.out.println("All Targets: " + allTables.getTargets());
```

#### 3. extractTablesForStep()
특정 STEP의 테이블 정보만 추출합니다.

```java
public TablesInfo extractTablesForStep(String sql, String stepName)
```

**예시:**
```java
TablesInfo step005 = parser.extractTablesForStep(sql, "STEP005");
if (step005 != null) {
    System.out.println("Sources: " + step005.getSources());
    System.out.println("Targets: " + step005.getTargets());
}
```

#### 4. formatStepTables()
STEP별 테이블 정보를 보기 좋게 포맷팅합니다.

```java
public String formatStepTables(Map<String, TablesInfo> stepTables)
```

**출력 형식:**
```
============================================================
 STEP001
============================================================

[Source Tables]
DW.`기지국01`

[Target Tables]
DM.`요약01`

============================================================
 STEP002
============================================================
...
```

#### 5. countSteps()
SQL 스크립트의 STEP 개수를 반환합니다.

```java
public int countSteps(String sql)
```

**예시:**
```java
int count = parser.countSteps(sql);
System.out.println("Total STEPs: " + count); // 출력: Total STEPs: 7
```

## 사용 예시

### 기본 사용법

```java
import service.queryParser.parser.TableStepParser;
import service.queryParser.vo.TablesInfo;

import java.util.Map;

// SQL 파일 읽기
String sql = Files.readString(Paths.get("sample.sql"));

        // 파서 생성
        TableStepParser parser = new TableStepParser();

        // STEP별 테이블 추출
        Map<String, TablesInfo> stepTables = parser.extractTablesByStep(sql);

        // 결과 출력
        String formatted = parser.formatStepTables(stepTables);
System.out.

        println(formatted);
```

### 특정 STEP 조회
```java
// STEP005만 조회
TablesInfo step005 = parser.extractTablesForStep(sql, "STEP005");
System.out.println("STEP005 Sources: " + step005.getSources());
System.out.println("STEP005 Targets: " + step005.getTargets());
```

### 전체 테이블 조회
```java
// STEP 구분 없이 전체 테이블 조회
TablesInfo allTables = parser.extractAllTables(sql);
System.out.println("All Sources: " + allTables.getSources());
System.out.println("All Targets: " + allTables.getTargets());
```

## 입력 예시

```sql
-- 변수선언
DECLARE some_var STRING;

/* STEP001 */
BEGIN
    INSERT INTO DM.`요약01`
    SELECT * FROM DW.`기지국01` AS N1
END;

/* STEP002 */
BEGIN
    SELECT *
    FROM DW.`기지국02` AS N1
    LEFT JOIN DW.`부서02` AS N2
      ON N1.ENT_ID = N2.ENT_ID
END;

/* STEP003 */
BEGIN
    DELETE FROM DW.`부서06`
END;
```

## 출력 예시

```
Total STEPs: 3

============================================================
 STEP001
============================================================

[Source Tables]
DW.`기지국01`

[Target Tables]
DM.`요약01`

============================================================
 STEP002
============================================================

[Source Tables]
DW.`기지국02`
DW.`부서02`

[Target Tables]
(No target tables)

============================================================
 STEP003
============================================================

[Source Tables]
DW.`부서06`

[Target Tables]
DW.`부서06`
```

## 내부 구조

### StepInfo (내부 클래스)
STEP 정보를 담는 VO 클래스입니다.

```java
private static class StepInfo {
    final String stepName;  // "STEP001"
    final String sql;       // STEP의 SQL 코드
}
```

### StepMatch (내부 클래스)
STEP 패턴 매칭 정보를 담는 클래스입니다.

```java
private static class StepMatch {
    final String stepName;  // "STEP001"
    final int start;        // 매칭 시작 위치
    final int end;          // 매칭 종료 위치
}
```

## 처리 흐름

1. **STEP 패턴 매칭**
   - 정규식으로 모든 STEP 주석 위치 찾기
   - `/* STEP001 */` 또는 `-- STEP001` 패턴

2. **SQL 분할**
   - STEP 주석 사이의 SQL 코드 추출
   - 각 STEP의 SQL을 StepInfo로 저장

3. **테이블 추출**
   - 각 STEP의 SQL을 TableParser로 파싱
   - 소스/타겟 테이블 추출

4. **결과 저장**
   - Map<STEP명, TablesInfo> 형태로 반환

## TableParser와의 연동

TableStepParser는 내부적으로 TableParser를 사용합니다:

```java
public class TableStepParser {
    private final TableParser tableParser;
    
    public Map<String, TablesInfo> extractTablesByStep(String sql) {
        // ...
        for (StepInfo step : steps) {
            TablesInfo tables = tableParser.extractTables(step.sql);
            // ...
        }
        // ...
    }
}
```

**장점:**
- 단일 책임: STEP 분할 로직만 담당
- 재사용성: TableParser의 모든 기능 활용
- 유지보수: TableParser 개선 시 자동 반영

## 주의사항

1. **STEP 번호 형식**
   - STEP 뒤에 숫자만 사용 가능 (STEP001, STEP002 등)
   - 문자가 섞이면 인식 안됨 (STEP_A, STEP-1 등)

2. **STEP 순서**
   - SQL 파일에 나타나는 순서대로 추출
   - STEP 번호 순서와 무관

3. **중복 STEP**
   - 동일한 STEP 번호가 여러 개 있으면 마지막 것만 저장됨

4. **CTE 별칭 제외**
   - WITH 절의 별칭은 자동으로 제외됨
   - TableParser의 기능 활용

## 활용 사례

### 1. 배치 작업 분석
```java
// 배치 SQL 파일의 STEP별 의존성 분석
Map<String, TablesInfo> steps = parser.extractTablesByStep(batchSql);
for (String step : steps.keySet()) {
    TablesInfo info = steps.get(step);
    System.out.println(step + " depends on: " + info.getSources());
    System.out.println(step + " modifies: " + info.getTargets());
}
```

### 2. 영향도 분석
```java
// 특정 테이블이 사용되는 STEP 찾기
String targetTable = "DW.`기지국01`";
Map<String, TablesInfo> steps = parser.extractTablesByStep(sql);

for (Map.Entry<String, TablesInfo> entry : steps.entrySet()) {
    if (entry.getValue().getSources().contains(targetTable)) {
        System.out.println(targetTable + " is used in " + entry.getKey());
    }
}
```

### 3. 테스트 데이터 준비
```java
// 각 STEP에 필요한 소스 테이블 목록 추출
Map<String, TablesInfo> steps = parser.extractTablesByStep(sql);
for (Map.Entry<String, TablesInfo> entry : steps.entrySet()) {
    System.out.println("For " + entry.getKey() + ", prepare:");
    entry.getValue().getSources().forEach(System.out::println);
}
```

## 참고
- TableParser: 실제 테이블 추출 로직
- TablesInfo: 테이블 정보 VO
- AppStepJob: STEP별 파일 출력 (writer 사용)

