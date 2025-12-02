# 테이블 추출 패턴 시스템

## 개요
SQL 쿼리에서 소스/타겟 테이블을 패턴 기반으로 추출하는 시스템입니다.
패턴 정의와 파싱 로직을 분리하여 유지보수성과 확장성을 극대화했습니다.

## 시스템 구조

### 1. 패턴 정의 클래스 (file.pattern)

#### 1.1 TableNamePattern
테이블명 패턴 정의 및 유효성 검증을 담당합니다.

**지원 테이블명 형식:**
- \`table\` - 백틱으로 감싼 테이블명
- schema.\`table\` - 스키마.백틱 테이블
- schema.table - 스키마.테이블
- table - 단순 테이블명

**주요 메소드:**
```java
// 테이블명 정규식 패턴
public static final String TABLE_NAME_REGEX

// 유효한 테이블명 검증
public static boolean isValidTableName(String tableName)

// 테이블명 정리
public static String cleanTableName(String name)

// 패턴 빌드
public static Pattern buildPattern(String keyword)
```

**제외 키워드:**
```
SELECT, FROM, WHERE, INSERT, UPDATE, DELETE, JOIN, LEFT, RIGHT,
INNER, OUTER, ON, AND, OR, NOT, IN, EXISTS, BETWEEN, LIKE,
ORDER, GROUP, BY, HAVING, LIMIT, OFFSET, UNION, ALL, DISTINCT,
AS, INTO, VALUES, SET, CASCADE, RESTRICT, PUBLIC, PRIVATE,
GRANT, REVOKE, COMMIT, ROLLBACK, SAVEPOINT, TRANSACTION, BEGIN,
END, IF, THEN, ELSE, CASE, WHEN, NULL, TRUE, FALSE, USING
```

#### 1.2 TableSourcePattern
소스 테이블 추출 패턴을 정의합니다.

**패턴 상수:**
```java
public static final String FROM_PATTERN = "FROM";
public static final String LEFT_JOIN_PATTERN = "LEFT\\s+(?:OUTER\\s+)?JOIN";
public static final String INNER_JOIN_PATTERN = "INNER\\s+JOIN";
public static final String RIGHT_JOIN_PATTERN = "RIGHT\\s+(?:OUTER\\s+)?JOIN";
public static final String JOIN_PATTERN = "JOIN";
public static final String USING_PATTERN = "USING";
public static final String FROM_CLAUSE_RANGE_PATTERN = "...";
```

**지원 패턴:**
1. FROM 절
2. LEFT JOIN / LEFT OUTER JOIN
3. INNER JOIN
4. RIGHT JOIN / RIGHT OUTER JOIN
5. JOIN
6. USING (MERGE 구문)
7. WITH 절 (CTE)
8. Oracle 조인 (콤마 방식)

#### 1.3 TableTargetPattern
타겟 테이블 추출 패턴을 정의합니다.

**패턴 상수:**
```java
public static final String INSERT_PATTERN = "INSERT\\s+INTO";
public static final String UPDATE_PATTERN = "UPDATE";
public static final String DELETE_FROM_PATTERN = "DELETE\\s+FROM";
public static final String DELETE_PATTERN = "DELETE";
public static final String MERGE_PATTERN = "MERGE\\s+INTO";
```

**지원 패턴:**
1. INSERT INTO
2. UPDATE
3. DELETE FROM
4. DELETE (Oracle 방식)
5. MERGE INTO

### 2. 파싱 로직 클래스 (file.parser)

#### 2.1 TableParser
위의 3개 패턴 클래스를 참조하여 실제 파싱을 수행합니다.

**주요 메소드:**
```java
// 메인 추출 메소드
public TablesInfo extractTables(String sql)

// 타겟 테이블 추출
private void extractTargetTables(String sql, Set<String> targets)

// 소스 테이블 추출
private void extractSourceTables(String sql, Set<String> sources)

// 주석 제거
private String removeComments(String sql)

// 패턴별 추출 메소드
private void extractByPattern(String sql, String keyword, Set<String> tables)
private void extractDeletePattern(String sql, Set<String> targets)
private void extractWithClause(String sql, Set<String> sources)
private void extractOracleJoin(String sql, Set<String> sources)
```

## 사용 예시

### 기본 사용
```java
TableParser parser = new TableParser();
TablesInfo info = parser.extractTables(sql);

// 소스 테이블
Set<String> sources = info.getSources();

// 타겟 테이블
Set<String> targets = info.getTargets();
```

### FileParserProcessor를 통한 사용
```java
FileParserProcessor processor = FileParserProcessor.withDefaults();
TablesInfo info = processor.parse(sql);
```

## 패턴별 예시

### 1. 타겟 테이블 추출

#### INSERT INTO
```sql
INSERT INTO DM.`요약01`
SELECT * FROM DW.`기지국01`
```
**추출**: DM.\`요약01\`

#### UPDATE
```sql
UPDATE BM.`회사`
SET name = 'New Name'
WHERE id = 1
```
**추출**: BM.\`회사\`

#### DELETE
```sql
-- DELETE FROM 방식
DELETE FROM DW.`부서06`

-- Oracle 방식
DELETE DW.`부서06` WHERE id = 1
```
**추출**: DW.\`부서06\`

#### MERGE INTO
```sql
MERGE INTO DM.일별카드발급현황 AS T
USING DW.카드목록 AS S
ON T.id = S.id
WHEN MATCHED THEN UPDATE SET T.name = S.name
```
**추출**: DM.일별카드발급현황

### 2. 소스 테이블 추출

#### FROM 절
```sql
SELECT * FROM DW.`기지국01` AS N1
```
**추출**: DW.\`기지국01\`

#### LEFT JOIN
```sql
SELECT *
FROM DW.`기지국02` AS N1
LEFT JOIN DW.`부서02` AS N2
  ON N1.ENT_ID = N2.ENT_ID
```
**추출**: DW.\`기지국02\`, DW.\`부서02\`

#### Oracle 조인 (콤마)
```sql
SELECT *
FROM DW.`기지국04` AS N1
   , DW.`부서04` AS N2
WHERE N1.ENT_ID = N2.ENT_ID
```
**추출**: DW.\`기지국04\`, DW.\`부서04\`

#### WITH 절 (CTE)
```sql
WITH `모수` AS (
  SELECT * FROM DW.`기지국05`
)
SELECT * FROM `모수`
```
**추출**: DW.\`기지국05\`

## 테스트 결과

### 입력 (sample002.sql)
```sql
/* STEP001 */
BEGIN
    INSERT INTO DM.`요약01`
    SELECT * FROM DW.`기지국01` AS N1
END;

/* STEP007 */
BEGIN
    MERGE INTO DM.일별카드발급현황 AS T
    USING DW.카드목록 AS S
    ON T.id = S.id
    WHEN MATCHED THEN UPDATE SET T.name = S.name;
END;
```

### 출력
```
STEP001:
  Sources: DW.`기지국01`
  Targets: DM.`요약01`

STEP007:
  Sources: DW.카드목록
  Targets: DM.일별카드발급현황
```

## 아키텍처 설계 원칙

### 1. 관심사의 분리 (Separation of Concerns)
- **패턴 정의** (file.pattern): 무엇을 찾을 것인가
- **파싱 로직** (file.parser): 어떻게 찾을 것인가

### 2. 단일 책임 원칙 (Single Responsibility Principle)
- TableNamePattern: 테이블명 패턴과 유효성만 담당
- TableSourcePattern: 소스 패턴 정의만 담당
- TableTargetPattern: 타겟 패턴 정의만 담당
- TableParser: 파싱 로직만 담당

### 3. 개방-폐쇄 원칙 (Open-Closed Principle)
- 새로운 패턴 추가 시 패턴 클래스에 상수만 추가
- 기존 코드 수정 없이 확장 가능

### 4. 의존성 역전 원칙 (Dependency Inversion Principle)
- TableParser는 구체적인 정규식이 아닌 패턴 상수에 의존
- 패턴 변경 시 TableParser 수정 불필요

## 장점

### 1. 유지보수성
- 패턴 정의가 한 곳에 집중되어 관리 용이
- 새로운 패턴 추가가 간단함

### 2. 테스트 용이성
- 각 클래스를 독립적으로 테스트 가능
- 패턴별 단위 테스트 작성 가능

### 3. 가독성
- 명확한 클래스명과 메소드명
- 패턴 정의와 로직이 분리되어 이해하기 쉬움

### 4. 확장성
- 새로운 SQL 패턴 추가 용이
- 다른 프로젝트에서 재사용 가능

## 참고 파일

- `TableNamePattern_패턴요건.txt` - 테이블명 패턴 요구사항
- `TableSourcePattern_패턴요건.txt` - 소스 테이블 패턴 요구사항
- `TableTargetPattern_패턴요건.txt` - 타겟 테이블 패턴 요구사항


## 개요
SQL 쿼리에서 소스/타겟 테이블을 패턴별로 추출하는 클래스입니다.
TableExtractor를 대체하여 더 명확한 구조와 높은 가독성을 제공합니다.

## 주요 기능

### 1. 타겟 테이블 추출
타겟 테이블은 데이터가 **변경**되는 테이블을 의미합니다.

#### 1.1 INSERT 패턴
```sql
INSERT INTO DM.`요약01`
SELECT * FROM DW.`기지국01`
```
- **추출**: `DM.`요약01``

#### 1.2 UPDATE 패턴
```sql
UPDATE BM.`회사`
SET name = 'New Name'
WHERE id = 1
```
- **추출**: `BM.`회사``

#### 1.3 DELETE 패턴
```sql
-- DELETE FROM 방식
DELETE FROM DW.`부서06`

-- Oracle 방식
DELETE DW.`부서06` WHERE id = 1
```
- **추출**: `DW.`부서06``

#### 1.4 MERGE 패턴
```sql
MERGE INTO DM.일별카드발급현황 AS T
USING DW.카드목록 AS S
ON T.id = S.id
WHEN MATCHED THEN UPDATE SET T.name = S.name
```
- **추출**: `DM.일별카드발급현황`

### 2. 소스 테이블 추출
소스 테이블은 데이터를 **읽는** 테이블을 의미합니다.

#### 2.1 FROM 절
```sql
SELECT * FROM DW.`기지국01` AS N1
```
- **추출**: `DW.`기지국01``

#### 2.2 LEFT JOIN / INNER JOIN
```sql
SELECT *
FROM DW.`기지국02` AS N1
LEFT JOIN DW.`부서02` AS N2
  ON N1.ENT_ID = N2.ENT_ID
```
- **추출**: `DW.`기지국02``, `DW.`부서02``

#### 2.3 Oracle 조인 문법 (콤마)
```sql
SELECT *
FROM DW.`기지국04` AS N1
   , DW.`부서04` AS N2
WHERE N1.ENT_ID = N2.ENT_ID
```
- **추출**: `DW.`기지국04``, `DW.`부서04``

#### 2.4 WITH 절 (CTE)
```sql
WITH `모수` AS (
  SELECT * FROM DW.`기지국05`
)
SELECT * FROM `모수`
```
- **추출**: `DW.`기지국05``

#### 2.5 USING 절 (MERGE)
```sql
MERGE INTO target_table
USING DW.카드목록 AS S
ON target.id = S.id
```
- **추출**: `DW.카드목록`

### 3. 테이블명 패턴 지원

다양한 테이블명 형식을 지원합니다:

| 패턴 | 예시 | 설명 |
|------|------|------|
| \`table\` | \`요약01\` | 백틱으로 감싼 테이블명 |
| schema.\`table\` | DW.\`기지국01\` | 스키마.백틱 테이블 |
| schema.table | DM.일별카드발급현황 | 스키마.테이블 |
| table | users | 단순 테이블명 |

## 클래스 구조

### 메소드 분류

#### 타겟 테이블 추출 메소드
- `extractTargetTables()` - 타겟 테이블 총괄 추출
- `extractInsertTargets()` - INSERT 패턴
- `extractUpdateTargets()` - UPDATE 패턴
- `extractDeleteTargets()` - DELETE 패턴
- `extractMergeTargets()` - MERGE 패턴

#### 소스 테이블 추출 메소드
- `extractSourceTables()` - 소스 테이블 총괄 추출
- `extractFromTables()` - FROM 절
- `extractJoinTables()` - JOIN 절 (LEFT, INNER, RIGHT)
- `extractUsingTables()` - USING 절
- `extractWithTables()` - WITH 절 (CTE)
- `extractOracleJoinTables()` - Oracle 조인 (콤마)

#### 유틸리티 메소드
- `removeComments()` - SQL 주석 제거
- `extractTablesByPattern()` - 정규식으로 테이블 추출
- `addTableIfValid()` - 유효성 검사 후 추가
- `cleanTableName()` - 테이블명 정리
- `isTrailingChar()` - 끝 문자 체크

## 사용 방법

### 기본 사용
```java
TablePattern pattern = new TablePattern();
TablesInfo info = pattern.extractTables(sql);

// 소스 테이블
Set<String> sources = info.getSources();

// 타겟 테이블
Set<String> targets = info.getTargets();
```

### 스텝별 추출
```java
String[] steps = sql.split("/\\*\\s*STEP\\d+\\s*\\*/");

for (int i = 1; i < steps.length; i++) {
    TablesInfo stepInfo = pattern.extractTables(steps[i]);
    System.out.println("STEP" + i);
    System.out.println("Sources: " + stepInfo.getSources());
    System.out.println("Targets: " + stepInfo.getTargets());
}
```

## SQL 키워드 제외

다음 키워드는 테이블명으로 인식하지 않습니다:
```
SELECT, FROM, WHERE, INSERT, UPDATE, DELETE, JOIN, LEFT, RIGHT,
INNER, OUTER, ON, AND, OR, NOT, IN, EXISTS, BETWEEN, LIKE,
ORDER, GROUP, BY, HAVING, LIMIT, OFFSET, UNION, ALL, DISTINCT,
AS, INTO, VALUES, SET, CASCADE, RESTRICT, PUBLIC, PRIVATE,
GRANT, REVOKE, COMMIT, ROLLBACK, SAVEPOINT, TRANSACTION, BEGIN,
END, IF, THEN, ELSE, CASE, WHEN, NULL, TRUE, FALSE, USING
```

## FileParserProcessor와의 통합

`FileParserProcessor`는 `TablePattern`을 래핑하여 사용합니다:

```java
public class FileParserProcessor {
    private final TablePattern extractor;

    public static FileParserProcessor withDefaults() {
        return new FileParserProcessor(new TablePattern());
    }

    public TablesInfo parse(String sql) {
        return extractor.extractTables(sql);
    }
}
```

## 테스트 예시

### 입력 (sample002.sql)
```sql
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
```

### 출력
```
STEP001:
  Sources: DW.`기지국01`
  Targets: DM.`요약01`

STEP002:
  Sources: DW.`기지국02`, DW.`부서02`
  Targets: (없음)
```

## TableExtractor와의 차이점

| 항목 | TableExtractor | TablePattern |
|------|----------------|--------------|
| 구조 | 단일 메소드에 모든 로직 | 패턴별 메소드 분리 |
| 가독성 | 낮음 | 높음 |
| 유지보수성 | 어려움 | 쉬움 |
| 확장성 | 제한적 | 우수 |
| 패턴 추가 | 복잡함 | 간단함 |

## 설계 원칙

1. **단일 책임 원칙**: 각 메소드는 하나의 패턴만 처리
2. **개방-폐쇄 원칙**: 새로운 패턴 추가가 용이
3. **명확한 네이밍**: 메소드명으로 기능 파악 가능
4. **테스트 용이성**: 패턴별 개별 테스트 가능

## 참고 파일

- `TableNamePattern_패턴요건.txt` - 테이블명 패턴 요구사항
- `TableSourcePattern_패턴요건.txt` - 소스 테이블 패턴 요구사항
- `TableTargetPattern_패턴요건.txt` - 타겟 테이블 패턴 요구사항

