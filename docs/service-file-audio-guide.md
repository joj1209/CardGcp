# service.queryParser 패키지 음성 학습 가이드

## 개요

안녕하세요. 이 문서는 service.queryParser 패키지의 구조와 동작 원리를 출퇴근하면서 들으며 학습할 수 있도록 정리한 자료입니다.

service.queryParser 패키지는 빅쿼리로 전환된 SQL 스크립트 파일에서 소스 테이블과 타겟 테이블을 자동으로 추출하는 배치 처리 시스템입니다. 스프링 배치 프레임워크와 유사한 구조로 설계되었으며, 리더, 프로세서, 라이터의 3단계 파이프라인으로 동작합니다.

---

## 1부. 전체 아키텍처

### 패키지 구조

service.queryParser 패키지는 다음과 같이 6개의 하위 패키지로 구성됩니다.

첫째, job 패키지는 전체 작업을 조율하는 메인 클래스들이 있습니다.
둘째, reader 패키지는 SQL 파일을 읽는 입력 IO를 담당합니다.
셋째, processor 패키지는 SQL을 파싱하여 테이블 정보를 추출합니다.
넷째, parser 패키지는 실제 파싱 로직을 구현합니다.
다섯째, pattern 패키지는 테이블 추출을 위한 정규식 패턴을 정의합니다.
여섯째, writer 패키지는 추출 결과를 파일로 저장합니다.
일곱째, vo 패키지는 테이블 정보를 담는 데이터 객체를 제공합니다.

### 처리 흐름

전체 처리 흐름은 크게 3단계로 이루어집니다.

1단계, 리드 스텝. SqlReader가 지정된 디렉토리의 모든 SQL 파일을 순회하며 파일 내용을 읽습니다. 기본 인코딩은 EUC-KR이며, 필요에 따라 UTF-8로 변경할 수 있습니다.

2단계, 프로세스 스텝. FileParserProcessor가 SQL 문자열을 분석하여 소스 테이블과 타겟 테이블을 추출합니다. MERGE, INSERT, UPDATE, DELETE 등의 DML 문을 인식합니다.

3단계, 라이트 스텝. TextWriter가 추출된 테이블 정보를 텍스트 파일로 저장합니다. 또한 CsvWriter가 전체 결과를 CSV 파일로 저장하여 엑셀에서 확인할 수 있습니다.

---

## 2부. Job 계층 - 작업 조율자

### AppJob 클래스

AppJob은 전체 배치 작업을 총괄하는 메인 클래스입니다.

AppJob의 역할은 다음과 같습니다.
첫째, 입력과 출력 경로를 관리합니다.
둘째, 리더, 프로세서, 라이터를 생성하고 조립합니다.
셋째, 전체 파이프라인을 실행하고 결과를 취합합니다.

기본 입력 경로는 D 드라이브의 11 Project, 11 DB, BigQuery 폴더입니다.
기본 출력 경로는 D 드라이브의 11 Project, 11 DB, BigQuery_out 폴더입니다.

AppJob은 createDefault 팩토리 메서드를 제공하여 기본 설정으로 쉽게 인스턴스를 생성할 수 있습니다.

실행 방법은 다음과 같습니다.
AppJob job을 AppJob.createDefault로 생성하고, job.stepRead를 호출하면 모든 SQL 파일이 자동으로 처리됩니다.

### AppStepJob 클래스

AppStepJob은 AppJob과 유사하지만, SQL 스크립트를 STEP 단위로 분할하여 처리합니다.

오라클 배치를 빅쿼리로 전환한 스크립트는 보통 STEP001, STEP002와 같이 스텝별로 구성되어 있습니다. AppStepJob은 각 스텝마다 별도로 소스 테이블과 타겟 테이블을 추출합니다.

AppStepJob의 처리 구조는 다음과 같습니다.
SqlReader가 파일을 읽고, FileStepParserProcessor가 스텝별로 파싱하며, TextStepWriter가 스텝별 결과를 저장합니다.

입력은 디렉토리뿐만 아니라 단일 파일도 가능합니다.

---

## 3부. Reader 계층 - 파일 입력

### SqlReader 클래스

SqlReader는 SQL 파일을 읽는 입력 IO를 담당하는 클래스입니다.

주요 기능은 세 가지입니다.
첫째, 지정된 문자셋으로 파일을 안전하게 읽습니다.
둘째, 인코딩 오류가 발생해도 대체 문자로 처리하여 계속 진행합니다.
셋째, 디렉토리를 재귀적으로 순회하며 모든 SQL 파일을 찾습니다.

기본 문자셋은 UTF-8입니다. 하지만 생성자에 다른 Charset을 전달하면 EUC-KR 등 다른 인코딩도 사용할 수 있습니다.

readFile 메서드는 파일 경로를 받아서 전체 내용을 문자열로 반환합니다. 내부적으로 CharsetDecoder를 사용하여 인코딩 오류를 안전하게 처리합니다.

run 메서드는 입력 디렉토리를 순회하며 모든 SQL 파일을 찾아서 핸들러에 전달합니다. 핸들러는 함수형 인터페이스로 정의되어 있어 람다 표현식으로 간단하게 처리 로직을 작성할 수 있습니다.

---

## 4부. Processor와 Parser 계층 - SQL 파싱

### FileParserProcessor 클래스

FileParserProcessor는 SQL 문자열을 받아서 테이블 정보를 추출하는 프로세서입니다.

내부적으로 TableParser를 사용하며, withDefaults 팩토리 메서드로 기본 설정의 프로세서를 생성할 수 있습니다.

parse 메서드는 SQL 문자열을 받아서 TablesInfo 객체를 반환합니다. TablesInfo에는 소스 테이블 목록과 타겟 테이블 목록이 담겨 있습니다.

### FileStepParserProcessor 클래스

FileStepParserProcessor는 SQL 스크립트를 스텝별로 파싱하는 프로세서입니다.

내부적으로 TableStepParser를 사용하며, parse 메서드는 Map을 반환합니다. Map의 키는 스텝 이름이고, 값은 해당 스텝의 TablesInfo입니다.

예를 들어, STEP001에 해당하는 소스 테이블과 타겟 테이블, STEP002에 해당하는 소스 테이블과 타겟 테이블이 각각 별도로 저장됩니다.

### TableParser 클래스

TableParser는 실제 SQL 파싱 로직을 구현하는 핵심 클래스입니다.

extractTables 메서드는 다음 순서로 동작합니다.

첫째, 주석을 제거합니다. 슬래시 별표 형태의 블록 주석과 대시 대시 형태의 라인 주석을 모두 제거합니다.

둘째, WITH 절의 CTE 별칭을 추출합니다. Common Table Expression은 임시 테이블이므로 실제 소스 테이블에서 제외해야 합니다.

셋째, 타겟 테이블을 추출합니다. MERGE INTO, INSERT INTO, UPDATE, DELETE FROM 등의 패턴을 인식합니다.

넷째, 소스 테이블을 추출합니다. FROM, JOIN, LEFT JOIN, INNER JOIN, RIGHT JOIN, USING 등의 패턴을 인식합니다.

다섯째, 오라클 조인 문법도 지원합니다. FROM 절에 콤마로 구분된 여러 테이블을 인식합니다.

여섯째, CTE 별칭을 소스 테이블 목록에서 제거합니다.

extractCteAliases 메서드는 WITH 절의 별칭을 추출합니다. 예를 들어 WITH 백틱 모수 백틱 AS 괄호 시작 형태의 패턴을 찾아서 모수를 별칭으로 인식합니다.

extractTargetTables 메서드는 타겟 테이블을 추출합니다. 여러 패턴을 순차적으로 적용하여 모든 DML 문을 처리합니다.

extractSourceTables 메서드는 소스 테이블을 추출합니다. FROM 절뿐만 아니라 다양한 JOIN 절, USING 절, WITH 절, 오라클 조인까지 모두 처리합니다.

extractOracleJoin 메서드는 특별히 오라클 조인 문법을 처리합니다. FROM 절에 콤마로 구분된 여러 테이블이 있고, WHERE 절에 조인 조건이 있는 형태를 인식합니다. 예를 들어 FROM 회사목록 N1, 사무실 N2 WHERE N1.회사ID = N2.회사ID 형태를 처리합니다.

### TableStepParser 클래스

TableStepParser는 SQL 스크립트를 스텝 단위로 분할하여 파싱하는 클래스입니다.

extractTablesByStep 메서드는 다음 순서로 동작합니다.

첫째, splitBySteps 메서드로 SQL을 스텝별로 분할합니다.
둘째, 각 스텝마다 TableParser로 테이블 정보를 추출합니다.
셋째, 스텝 이름을 키로 하고 TablesInfo를 값으로 하는 Map을 반환합니다.

splitBySteps 메서드는 정규식으로 스텝 패턴을 찾습니다. 슬래시 별표 STEP001 별표 슬래시 또는 대시 대시 STEP001 형태를 모두 인식합니다.

각 스텝의 SQL은 해당 스텝 주석부터 다음 스텝 주석 전까지의 내용입니다.

formatStepTables 메서드는 스텝별 결과를 보기 좋은 텍스트 형식으로 포맷팅합니다. 각 스텝마다 소스 테이블과 타겟 테이블을 구분하여 출력합니다.

추가로 countSteps 메서드로 스텝 개수를 세거나, extractTablesForStep 메서드로 특정 스텝의 정보만 추출할 수도 있습니다.

---

## 5부. Pattern 계층 - 정규식 패턴

### TableNamePattern 클래스

TableNamePattern은 테이블명 인식을 위한 정규식 패턴을 정의합니다.

지원하는 테이블명 형식은 네 가지입니다.

첫째, 백틱으로 감싼 테이블명. 예를 들어 백틱 사용자 백틱 형태입니다.
둘째, 스키마 점 백틱 테이블. 예를 들어 DM 점 백틱 월별매출현황 백틱 형태입니다.
셋째, 스키마 점 테이블. 예를 들어 DM 점 daily_sales 형태입니다.
넷째, 단순 테이블명. 예를 들어 users 형태입니다.

TABLE_NAME_REGEX 상수는 이 모든 형식을 매칭할 수 있는 정규식입니다.

isValidTableName 메서드는 테이블명이 유효한지 검사합니다. SQL 키워드가 아니고, 2자 이상인 경우만 유효한 테이블로 인정합니다.

KEYWORDS 상수는 제외할 SQL 키워드 목록입니다. SELECT, FROM, WHERE, INSERT, UPDATE, DELETE, JOIN 등의 키워드가 테이블명으로 인식되지 않도록 합니다.

cleanTableName 메서드는 테이블명 끝의 불필요한 문자를 제거합니다. 콤마, 세미콜론, 괄호, 줄바꿈 등을 제거하여 깔끔한 테이블명을 얻습니다.

buildPattern 메서드는 SQL 키워드와 테이블명 패턴을 결합하여 정규식을 생성합니다. 예를 들어 FROM 키워드 뒤에 나오는 테이블명을 찾는 패턴을 만들 수 있습니다.

### TableSourcePattern 클래스

TableSourcePattern은 소스 테이블을 찾기 위한 패턴을 정의합니다.

소스 테이블은 데이터를 읽는 테이블입니다. 주로 SELECT 문의 FROM 절이나 JOIN 절에 나타납니다.

정의된 패턴은 다음과 같습니다.

FROM_PATTERN은 FROM 키워드를 찾습니다.
LEFT_JOIN_PATTERN은 LEFT JOIN 또는 LEFT OUTER JOIN을 찾습니다.
INNER_JOIN_PATTERN은 INNER JOIN을 찾습니다.
RIGHT_JOIN_PATTERN은 RIGHT JOIN 또는 RIGHT OUTER JOIN을 찾습니다.
JOIN_PATTERN은 단순 JOIN을 찾습니다.
USING_PATTERN은 MERGE 문의 USING 절을 찾습니다.

FROM_CLAUSE_RANGE_PATTERN은 특별히 오라클 조인을 위한 패턴입니다. FROM 키워드부터 WHERE, GROUP, ORDER 등의 키워드 전까지의 범위를 찾습니다.

ALL_SOURCE_PATTERNS 배열은 모든 소스 패턴을 포함합니다.

### TableTargetPattern 클래스

TableTargetPattern은 타겟 테이블을 찾기 위한 패턴을 정의합니다.

타겟 테이블은 데이터가 변경되는 테이블입니다. INSERT, UPDATE, DELETE, MERGE 등의 DML 문에 나타납니다.

정의된 패턴은 다음과 같습니다.

INSERT_PATTERN은 INSERT INTO 키워드를 찾습니다.
UPDATE_PATTERN은 UPDATE 키워드를 찾습니다.
DELETE_FROM_PATTERN은 DELETE FROM 키워드를 찾습니다.
DELETE_PATTERN은 단순 DELETE 키워드를 찾습니다. 오라클 방식의 DELETE를 지원합니다.
MERGE_PATTERN은 MERGE INTO 키워드를 찾습니다.

ALL_TARGET_PATTERNS 배열은 모든 타겟 패턴을 포함합니다. MERGE가 DELETE보다 먼저 배치되어 있어 MERGE INTO가 DELETE보다 우선적으로 매칭됩니다.

---

## 6부. Writer 계층 - 결과 저장

### TextWriter 클래스

TextWriter는 테이블 정보를 텍스트 파일로 저장하는 클래스입니다.

생성자는 출력 디렉토리와 문자셋을 받습니다. 기본 문자셋은 UTF-8입니다.

write 메서드는 상대 경로와 내용을 받아서 파일을 저장합니다. 출력 디렉토리가 없으면 자동으로 생성합니다.

writeTables 메서드는 TablesInfo 객체를 받아서 포맷팅한 후 파일로 저장합니다.

formatTables 메서드는 테이블 정보를 보기 좋은 형식으로 변환합니다. 소스 테이블 섹션과 타겟 테이블 섹션으로 구분하여 각 테이블을 한 줄씩 출력합니다. 테이블 목록은 알파벳순으로 정렬됩니다.

### TextStepWriter 클래스

TextStepWriter는 스텝별 테이블 정보를 텍스트 파일로 저장하는 클래스입니다.

TextWriter와 유사하지만 Map 형태의 스텝별 데이터를 처리합니다.

writeStepTables 메서드는 스텝별 TablesInfo Map을 받아서 파일로 저장합니다.

formatStepTables 메서드는 각 스텝마다 등호 60개로 구분선을 그어서 보기 좋게 포맷팅합니다. 각 스텝 이름 아래에 소스 테이블과 타겟 테이블이 표시됩니다.

buildOutputName 메서드는 입력 파일명에서 출력 파일명을 생성합니다. 확장자를 SQL에서 step_tables.txt로 변경합니다.

처리 과정이 로그로 출력되어 어떤 파일이 처리되고 있는지 실시간으로 확인할 수 있습니다.

### CsvWriter 클래스

CsvWriter는 테이블 정보를 CSV 형식으로 저장하는 클래스입니다. 엑셀에서 열어볼 수 있어 결과를 한눈에 파악하기 좋습니다.

생성자는 출력 파일 경로와 문자셋을 받습니다. 내부적으로 헤더 목록과 레코드 목록을 관리합니다.

기본 헤더는 세 개입니다.
File Name은 파일명입니다.
Source Tables는 소스 테이블 목록입니다.
Target Tables는 타겟 테이블 목록입니다.

addHeader 메서드로 추가 헤더를 동적으로 추가할 수 있습니다. 단, 파일을 쓰기 전에만 가능합니다.

addRecord 메서드는 파일명과 TablesInfo를 받아서 레코드를 추가합니다. 테이블 목록은 세미콜론으로 구분된 문자열로 저장됩니다.

오버로딩된 addRecord 메서드는 추가 필드를 Map으로 받을 수 있어 확장성이 좋습니다.

write 메서드는 모든 레코드를 CSV 파일로 저장합니다. 중요한 점은 UTF-8 BOM을 추가한다는 것입니다. 이렇게 하면 엑셀에서 한글이 깨지지 않고 정상적으로 표시됩니다.

escapeCsv 메서드는 CSV 규칙에 맞게 필드를 이스케이프 처리합니다. 콤마, 따옴표, 줄바꿈이 포함된 경우 큰따옴표로 감싸고, 내부 따옴표는 이중으로 처리합니다.

---

## 7부. VO 계층 - 데이터 객체

### TablesInfo 클래스

TablesInfo는 소스 테이블과 타겟 테이블 목록을 보관하는 값 객체입니다.

내부적으로 두 개의 LinkedHashSet을 사용합니다. sources는 소스 테이블 목록이고, targets는 타겟 테이블 목록입니다. LinkedHashSet을 사용하여 삽입 순서를 유지하면서 중복을 제거합니다.

getSources 메서드와 getTargets 메서드는 각각 소스와 타겟 Set을 반환합니다.

getSortedSources 메서드와 getSortedTargets 메서드는 알파벳순으로 정렬된 TreeSet을 반환합니다. 결과 파일에 정렬된 목록을 출력할 때 사용됩니다.

isEmpty 메서드는 소스와 타겟이 모두 비어있는지 확인합니다.

addSource 메서드와 addTarget 메서드는 각각 소스와 타겟 테이블을 추가합니다. null이나 빈 문자열은 자동으로 무시됩니다.

---

## 8부. 실행 방법과 활용

### AppJob 실행 방법

AppJob을 실행하는 방법은 매우 간단합니다.

main 메서드가 있는 클래스이므로 IDE에서 직접 실행할 수 있습니다.

코드는 다음과 같습니다.
AppJob job = AppJob.createDefault();
job.stepRead();

createDefault 메서드는 기본 설정으로 Job을 생성합니다. 입력 경로는 D 드라이브의 BigQuery 폴더이고, 출력 경로는 BigQuery_out 폴더입니다.

stepRead 메서드를 호출하면 모든 SQL 파일이 자동으로 처리됩니다.

처리 과정은 콘솔에 로그로 출력됩니다. 어떤 파일이 처리되고 있는지, 몇 개의 테이블이 추출되었는지 실시간으로 확인할 수 있습니다.

모든 파일 처리가 완료되면 CSV 파일이 저장됩니다. summary.csv 파일을 엑셀로 열어서 전체 결과를 한눈에 확인할 수 있습니다.

### AppStepJob 실행 방법

AppStepJob은 스텝별로 결과를 보고 싶을 때 사용합니다.

실행 방법은 두 가지입니다.

첫째, main 메서드에서 직접 실행합니다. 인자 없이 실행하면 기본 경로를 사용합니다.

둘째, 커맨드 라인 인자로 입력 경로를 지정할 수 있습니다. 예를 들어 java AppStepJob "D:\myFolder" 형태로 실행합니다.

입력은 디렉토리 또는 단일 파일이 가능합니다. 디렉토리를 지정하면 모든 SQL 파일이 처리되고, 파일을 지정하면 해당 파일만 처리됩니다.

결과 파일은 원본 파일명에 step_tables.txt가 붙은 형태로 저장됩니다. 각 스텝마다 구분선으로 나뉘어 소스와 타겟 테이블이 표시됩니다.

### 커스텀 설정 방법

기본 설정 외에 커스텀 설정도 가능합니다.

예를 들어 입력 문자셋을 EUC-KR로 변경하려면 다음과 같이 합니다.

SqlReader reader = new SqlReader(Charset.forName("EUC-KR"));
FileParserProcessor processor = FileParserProcessor.withDefaults();
TextWriter writer = new TextWriter(outputPath, Charset.forName("UTF-8"));
Path csvPath = outputPath.resolve("summary.csv");
CsvWriter csvWriter = new CsvWriter(csvPath, Charset.forName("UTF-8"));
AppJob job = new AppJob(inputPath, reader, processor, writer, csvWriter);
job.stepRead();

이처럼 각 컴포넌트를 개별적으로 생성하여 조합할 수 있습니다.

출력 디렉토리를 변경하거나, 다른 인코딩을 사용하거나, 커스텀 파서를 사용하는 등 다양한 설정이 가능합니다.

---

## 9부. 주요 기능과 특징

### 1. 다양한 SQL 패턴 지원

이 시스템은 다양한 SQL 패턴을 인식합니다.

DML 문으로는 INSERT, UPDATE, DELETE, MERGE를 모두 지원합니다.

JOIN 문으로는 INNER JOIN, LEFT JOIN, RIGHT JOIN, CROSS JOIN을 모두 지원합니다.

오라클 조인 문법도 지원합니다. FROM 절에 콤마로 구분된 테이블과 WHERE 절의 조인 조건을 인식합니다.

WITH 절의 CTE도 올바르게 처리합니다. CTE 별칭이 소스 테이블로 잘못 인식되지 않도록 제외합니다.

### 2. 스텝별 처리

오라클 배치를 빅쿼리로 전환한 스크립트는 보통 스텝별로 구성됩니다.

각 스텝은 독립적인 작업 단위입니다. 예를 들어 STEP001은 기초 데이터를 적재하고, STEP002는 집계하고, STEP003은 최종 결과를 저장하는 식입니다.

AppStepJob을 사용하면 각 스텝마다 어떤 테이블을 읽고 쓰는지 명확하게 파악할 수 있습니다.

이는 데이터 lineage를 추적하거나 의존성을 분석할 때 매우 유용합니다.

### 3. 인코딩 안전성

다양한 인코딩의 파일을 안전하게 처리합니다.

SqlReader는 CharsetDecoder를 사용하여 인코딩 오류를 대체 문자로 처리합니다. 파일 읽기가 중단되지 않고 계속 진행됩니다.

입력 파일이 EUC-KR이어도, UTF-8이어도, 또는 섞여 있어도 문제없이 처리됩니다.

출력은 기본적으로 UTF-8을 사용하여 현대적인 시스템과의 호환성을 보장합니다.

CSV 파일은 UTF-8 BOM을 추가하여 엑셀에서 한글이 깨지지 않습니다.

### 4. 확장 가능한 구조

시스템은 확장을 고려하여 설계되었습니다.

CsvWriter는 동적으로 헤더를 추가할 수 있습니다. 파일명, 소스, 타겟 외에 추가 정보를 기록하고 싶다면 addHeader 메서드로 쉽게 추가할 수 있습니다.

addRecord 메서드도 오버로딩되어 있어 추가 필드를 Map으로 전달할 수 있습니다.

패턴 클래스들은 상수로 정의되어 있어 새로운 패턴을 추가하기 쉽습니다.

Parser 클래스는 메서드 단위로 기능이 분리되어 있어 특정 부분만 수정하거나 확장하기 용이합니다.

### 5. 스프링 배치 유사 구조

전체 구조는 스프링 배치 프레임워크를 모방했습니다.

Reader는 데이터를 읽고, Processor는 변환하고, Writer는 저장합니다.

Job은 전체 작업을 조율하고, Step은 작업의 단위입니다.

이런 구조는 배치 처리의 표준 패턴으로 널리 알려져 있습니다.

따라서 스프링 배치를 알고 있는 개발자라면 쉽게 이해하고 사용할 수 있습니다.

또한 순수 자바로 구현되어 스프링 의존성 없이 독립적으로 실행됩니다.

---

## 10부. 실제 사용 시나리오

### 시나리오 1: 전체 SQL 파일 분석

상황: 빅쿼리로 전환한 SQL 파일이 100개 정도 있습니다. 각 파일이 어떤 테이블을 읽고 쓰는지 전체적으로 파악하고 싶습니다.

해결: AppJob을 사용합니다.

먼저 모든 SQL 파일을 BigQuery 폴더에 모읍니다.

그 다음 AppJob.createDefault로 Job을 생성하고 stepRead를 호출합니다.

처리가 완료되면 BigQuery_out 폴더에 각 파일별 결과 텍스트 파일이 생성됩니다.

또한 summary.csv 파일이 생성되어 전체 결과를 엑셀로 확인할 수 있습니다.

CSV 파일을 엑셀로 열어서 소스 테이블로 필터링하거나 정렬하면 어떤 테이블이 가장 많이 사용되는지 파악할 수 있습니다.

### 시나리오 2: 스텝별 데이터 흐름 분석

상황: 특정 SQL 파일이 10개의 스텝으로 구성되어 있습니다. 각 스텝이 어떤 테이블을 읽고 쓰는지 순서대로 파악하고 싶습니다.

해결: AppStepJob을 사용합니다.

해당 SQL 파일 경로를 인자로 전달하여 AppStepJob을 실행합니다.

결과 파일에는 STEP001부터 STEP010까지 각 스텝마다 소스와 타겟 테이블이 구분되어 표시됩니다.

이를 통해 데이터가 어떻게 흘러가는지 시각적으로 파악할 수 있습니다.

예를 들어 STEP001에서 원천 테이블을 읽어 기초 테이블에 쓰고, STEP002에서 기초 테이블을 읽어 집계 테이블에 쓰는 흐름을 명확히 알 수 있습니다.

### 시나리오 3: 특정 테이블 의존성 분석

상황: DM.월별매출현황 테이블이 어떤 SQL 파일에서 사용되는지 찾고 싶습니다.

해결: 전체 SQL 파일을 처리한 후 CSV 파일을 활용합니다.

AppJob으로 모든 파일을 처리합니다.

summary.csv 파일을 엑셀로 엽니다.

Target Tables 컬럼에서 DM.월별매출현황을 검색합니다.

해당 테이블을 타겟으로 하는 모든 파일 목록이 표시됩니다.

마찬가지로 Source Tables 컬럼에서 검색하면 해당 테이블을 읽는 모든 파일도 찾을 수 있습니다.

### 시나리오 4: EUC-KR 파일 처리

상황: 레거시 시스템의 SQL 파일이 EUC-KR로 인코딩되어 있습니다.

해결: SqlReader의 문자셋을 변경합니다.

SqlReader reader = new SqlReader(Charset.forName("EUC-KR"));
이후 다른 컴포넌트는 동일하게 구성합니다.

AppJob을 생성할 때 이 reader를 전달합니다.

EUC-KR로 파일을 읽어서 UTF-8로 결과를 저장하므로 현대적인 시스템에서도 문제없이 사용할 수 있습니다.

---

## 11부. 기술적 세부사항

### 정규식 패턴의 이해

시스템은 정규식을 활용하여 SQL을 파싱합니다.

TABLE_NAME_REGEX는 복잡한 정규식입니다. 백틱으로 감싼 테이블명, 스키마가 포함된 테이블명, 단순 테이블명을 모두 매칭합니다.

(?is) 플래그는 대소문자를 구분하지 않고, 점이 줄바꿈도 매칭하도록 합니다.

\b는 단어 경계를 의미하여 부분 매칭을 방지합니다.

그룹 캡처를 사용하여 매칭된 테이블명을 추출합니다.

### CTE 별칭 제외 로직

WITH 절의 Common Table Expression은 임시 테이블입니다.

예를 들어 WITH 백틱 모수 백틱 AS 괄호 SELECT ... 괄호 형태에서 모수는 실제 테이블이 아닙니다.

만약 이후 쿼리에서 FROM 백틱 모수 백틱이 나오면 이것을 소스 테이블로 인식하면 안 됩니다.

따라서 extractCteAliases 메서드로 먼저 CTE 별칭을 추출하고, 최종 소스 테이블 목록에서 제거합니다.

### 오라클 조인 처리

오라클은 ANSI 표준 JOIN 외에 전통적인 조인 문법을 지원합니다.

예를 들어 FROM table1, table2 WHERE table1.id = table2.id 형태입니다.

이 경우 FROM 절에 콤마로 구분된 모든 테이블이 소스 테이블입니다.

extractOracleJoin 메서드는 FROM 절의 범위를 찾고, 콤마로 분리하여 각 테이블을 추출합니다.

서브쿼리나 인라인뷰가 포함된 경우는 제외하여 정확도를 높입니다.

### UTF-8 BOM 처리

엑셀은 UTF-8 파일을 열 때 BOM이 없으면 기본 인코딩으로 해석합니다.

한글이 포함된 경우 깨져서 보이는 문제가 발생합니다.

CsvWriter는 UTF-8 BOM인 EF BB BF를 파일 맨 앞에 추가합니다.

자바에서는 \ufeff 문자를 쓰면 BOM이 추가됩니다.

이렇게 하면 엑셀이 자동으로 UTF-8로 인식하여 한글이 정상적으로 표시됩니다.

### LinkedHashSet의 활용

TablesInfo는 Set을 사용하여 중복을 제거합니다.

HashSet이 아닌 LinkedHashSet을 사용하는 이유는 삽입 순서를 유지하기 위함입니다.

SQL에서 테이블이 나타나는 순서대로 결과에 표시되면 이해하기 쉽습니다.

정렬이 필요한 경우에는 getSortedSources나 getSortedTargets 메서드로 TreeSet을 얻을 수 있습니다.

---

## 12부. 주의사항과 제한사항

### 복잡한 SQL 처리

매우 복잡한 SQL은 완벽하게 파싱되지 않을 수 있습니다.

예를 들어 동적 SQL, 중첩된 서브쿼리, 복잡한 CASE 문 등은 정규식으로 처리하기 어렵습니다.

대부분의 일반적인 DML과 SELECT 문은 잘 처리되지만, 특수한 경우는 검토가 필요합니다.

결과 파일을 확인하여 예상과 다르면 수동으로 보정해야 할 수 있습니다.

### 주석 처리

슬래시 별표 형태와 대시 대시 형태의 주석은 제거됩니다.

하지만 문자열 리터럴 안의 주석 패턴은 잘못 제거될 수 있습니다.

예를 들어 SELECT '-- this is not comment' FROM table 같은 경우입니다.

현재 구현은 단순 정규식으로 주석을 제거하므로 이런 경우는 고려하지 않습니다.

실제로는 이런 경우가 드물어 큰 문제가 되지 않습니다.

### 파일 크기

매우 큰 SQL 파일은 메모리에 모두 로드되므로 주의가 필요합니다.

readFile 메서드는 파일 전체를 byte 배열로 읽습니다.

수백 메가바이트 이상의 파일은 OutOfMemoryError를 발생시킬 수 있습니다.

일반적인 SQL 스크립트는 수 킬로바이트에서 수 메가바이트이므로 문제없습니다.

### 인코딩 혼재

하나의 파일에 여러 인코딩이 섞여 있으면 제대로 읽히지 않을 수 있습니다.

SqlReader는 하나의 문자셋을 사용하여 전체 파일을 읽습니다.

일부는 EUC-KR이고 일부는 UTF-8인 파일은 올바르게 처리할 수 없습니다.

이런 경우는 파일을 먼저 통일된 인코딩으로 변환해야 합니다.

---

## 13부. 확장과 커스터마이징

### 새로운 패턴 추가

새로운 SQL 패턴을 지원하려면 패턴 클래스를 수정합니다.

예를 들어 TRUNCATE TABLE을 타겟 테이블로 인식하려면 TableTargetPattern에 TRUNCATE_PATTERN을 추가합니다.

그리고 TableParser의 extractTargetTables 메서드에 해당 패턴 추출 로직을 추가합니다.

패턴 추가는 매우 간단하며 기존 코드를 수정할 필요가 없습니다.

### CSV 필드 추가

CSV에 새로운 컬럼을 추가하려면 CsvWriter를 수정합니다.

initializeDefaultHeaders 메서드에 새로운 헤더를 추가합니다.

addRecord 메서드에 해당 필드 값을 설정하는 로직을 추가합니다.

또는 동적으로 addHeader 메서드를 사용하고 추가 필드 Map을 전달할 수도 있습니다.

### 커스텀 Parser 구현

TableParser를 상속하거나 새로운 Parser를 구현할 수 있습니다.

예를 들어 특정 회사의 SQL 방언을 처리하는 CustomParser를 만들 수 있습니다.

FileParserProcessor에 이 Parser를 주입하면 됩니다.

Job 생성 시 커스텀 Processor를 전달하여 사용합니다.

### 결과 포맷 변경

TextWriter의 formatTables 메서드를 수정하면 출력 형식을 바꿀 수 있습니다.

예를 들어 JSON 형식이나 XML 형식으로 출력하도록 변경할 수 있습니다.

또는 새로운 Writer 클래스를 구현하여 다른 형식으로 저장할 수도 있습니다.

Job에 여러 Writer를 주입하여 동시에 여러 형식으로 출력하는 것도 가능합니다.

---

## 14부. 성능과 최적화

### 파일 IO 성능

현재 구현은 각 파일을 순차적으로 처리합니다.

파일 수가 많으면 시간이 오래 걸릴 수 있습니다.

병렬 처리를 도입하면 성능을 크게 향상시킬 수 있습니다.

예를 들어 Java Stream의 parallel을 사용하거나 ExecutorService를 활용할 수 있습니다.

### 정규식 컴파일

정규식은 사용 전에 컴파일되어 Pattern 객체로 저장됩니다.

buildPattern 메서드는 매번 새로운 Pattern을 생성합니다.

자주 사용되는 패턴은 static 필드로 미리 컴파일해두면 성능이 향상됩니다.

하지만 현재 수준에서도 충분히 빠르므로 큰 문제는 없습니다.

### 메모리 사용

각 파일의 내용이 String으로 메모리에 로드됩니다.

파일 수가 많으면 메모리 사용량이 증가합니다.

현재는 파일 하나씩 처리하고 바로 결과를 쓰므로 메모리 문제는 거의 없습니다.

더 최적화하려면 스트림 처리 방식으로 변경할 수 있습니다.

### CSV 쓰기 최적화

CsvWriter는 모든 레코드를 메모리에 모았다가 한 번에 씁니다.

레코드 수가 매우 많으면 메모리가 부족할 수 있습니다.

스트리밍 방식으로 변경하면 메모리 사용량을 줄일 수 있습니다.

하지만 일반적으로 수백 개 파일 정도는 문제없이 처리됩니다.

---

## 15부. 마무리

### 핵심 요약

service.queryParser 패키지는 SQL 파일에서 테이블 정보를 자동으로 추출하는 시스템입니다.

스프링 배치와 유사한 Reader, Processor, Writer 구조로 설계되었습니다.

다양한 SQL 패턴을 지원하며, 스텝별 처리도 가능합니다.

인코딩 안전성이 보장되고, 결과를 텍스트와 CSV로 저장합니다.

확장 가능한 구조로 새로운 요구사항에 쉽게 대응할 수 있습니다.

### 학습 포인트

이 시스템을 통해 다음을 배울 수 있습니다.

첫째, 정규식을 활용한 텍스트 파싱 기법.
둘째, 함수형 인터페이스와 람다를 활용한 콜백 패턴.
셋째, 팩토리 메서드와 의존성 주입을 활용한 객체 생성.
넷째, 파일 IO와 문자셋 처리의 실전 기법.
다섯째, CSV 형식과 인코딩 처리.

### 실무 활용

이 시스템은 다음과 같은 실무 상황에서 유용합니다.

데이터베이스 마이그레이션 시 테이블 의존성 파악.
레거시 시스템 분석 시 데이터 흐름 추적.
SQL 스크립트 문서화 자동화.
데이터 리니지 분석의 기초 자료 생성.
코드 리뷰 시 테이블 사용 현황 파악.

### 추가 학습 자료

더 깊이 공부하려면 다음 문서들을 참고하세요.

docs 폴더의 AppJob.md는 AppJob 클래스를 상세히 설명합니다.
SqlReader.md는 파일 읽기 메커니즘을 설명합니다.
TableStepParser.md는 스텝별 파싱 로직을 설명합니다.
table-pattern-system.md는 패턴 시스템 전체를 설명합니다.

### 지속적인 개선

시스템은 계속 발전할 수 있습니다.

사용하면서 발견한 문제점이나 개선 아이디어를 기록하세요.

새로운 SQL 패턴이 필요하면 패턴 클래스를 확장하세요.

성능 문제가 생기면 병렬 처리나 스트리밍을 도입하세요.

결과 형식이 다양하게 필요하면 Writer를 추가하세요.

### 감사의 말

이 문서를 끝까지 들어주셔서 감사합니다.

출퇴근 시간을 활용하여 service.queryParser 패키지를 이해하는 데 도움이 되었기를 바랍니다.

실제로 코드를 실행해보고, 수정해보면서 더 깊이 이해할 수 있을 것입니다.

질문이나 피드백이 있으면 언제든 문서를 업데이트하겠습니다.

즐거운 코딩 되세요!

---

## 부록: 주요 클래스 한눈에 보기

### Job 계층
- **AppJob**: 전체 SQL 파일을 처리하는 메인 Job
- **AppStepJob**: 스텝별로 SQL 파일을 처리하는 Job

### Reader 계층
- **SqlReader**: SQL 파일을 읽는 입력 IO

### Processor 계층
- **FileParserProcessor**: SQL 전체를 파싱하는 프로세서
- **FileStepParserProcessor**: 스텝별로 SQL을 파싱하는 프로세서

### Parser 계층
- **TableParser**: 실제 파싱 로직을 구현하는 핵심 클래스
- **TableStepParser**: 스텝별 파싱 로직을 구현하는 클래스

### Pattern 계층
- **TableNamePattern**: 테이블명 정규식 패턴
- **TableSourcePattern**: 소스 테이블 추출 패턴
- **TableTargetPattern**: 타겟 테이블 추출 패턴

### Writer 계층
- **TextWriter**: 텍스트 파일 출력
- **TextStepWriter**: 스텝별 텍스트 파일 출력
- **CsvWriter**: CSV 파일 출력

### VO 계층
- **TablesInfo**: 소스/타겟 테이블 정보를 담는 값 객체

---

**문서 끝. 좋은 하루 되세요!**

