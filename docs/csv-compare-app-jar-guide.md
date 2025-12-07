# CsvCompareApp JAR 파일 실행 가이드

## 파일 정보

- **JAR 파일명**: `csv-compare-app.jar`
- **위치**: `target/csv-compare-app.jar`
- **크기**: 약 10.8MB
- **메인 클래스**: `service.compare.job.CsvCompareApp`
- **필요 환경**: Java Runtime Environment (JRE) 8 이상

---

## 빌드 방법

### Maven Wrapper 사용 (권장)

```bash
# Windows
.\mvnw.cmd clean compile assembly:single -Dmaven.test.skip=true

# Linux/Mac
./mvnw clean compile assembly:single -Dmaven.test.skip=true
```

### Maven 직접 사용

```bash
mvn clean compile assembly:single -Dmaven.test.skip=true
```

빌드가 완료되면 `target/csv-compare-app.jar` 파일이 생성됩니다.

---

## 실행 방법

### 기본 사용법

```bash
java -jar csv-compare-app.jar <파일1.csv> <파일2.csv> [키컬럼]
```

### 파라미터 설명

| 파라미터 | 필수 여부 | 설명 |
|---------|----------|------|
| 파일1.csv | 필수 | 비교할 첫 번째 CSV 파일 경로 |
| 파일2.csv | 필수 | 비교할 두 번째 CSV 파일 경로 |
| 키컬럼 | 선택 | 비교에 사용할 키 컬럼명 (콤마로 구분, 미지정시 첫 번째 헤더 사용) |

---

## 실행 예시

### 예시 1: 자동 키 지정 (첫 번째 헤더 사용)

```bash
java -jar csv-compare-app.jar data1.csv data2.csv
```

**실행 결과**:
- 첫 번째 파일의 첫 번째 헤더 컬럼을 키로 사용
- 콘솔에 안내 메시지 출력: `[안내] 키 미지정: 첫 번째 헤더 컬럼을 키로 사용합니다 -> ID`

### 예시 2: 단일 키 지정

```bash
java -jar csv-compare-app.jar data1.csv data2.csv ID
```

**설명**: ID 컬럼을 키로 사용하여 두 파일을 비교합니다.

### 예시 3: 복합 키 지정

```bash
java -jar csv-compare-app.jar sales1.csv sales2.csv DATE,STORE_ID,PRODUCT
```

**설명**: DATE, STORE_ID, PRODUCT 세 컬럼을 조합하여 키로 사용합니다.

### 예시 4: 절대 경로 사용

```bash
java -jar csv-compare-app.jar "C:\data\file1.csv" "C:\data\file2.csv" ID
```

### 예시 5: 상대 경로 사용

```bash
# csv-compare-app.jar와 같은 디렉토리에 CSV 파일이 있는 경우
java -jar csv-compare-app.jar test1.csv test2.csv ID
```

---

## 실행 테스트

### 1. 테스트 CSV 파일 생성

**test1.csv**:
```csv
ID,NAME,AGE
1,John,25
2,Jane,28
3,Bob,35
```

**test2.csv**:
```csv
ID,NAME,AGE
1,John,25
2,Jane,30
4,Alice,40
```

### 2. 비교 실행

```bash
java -jar csv-compare-app.jar test1.csv test2.csv ID
```

### 3. 결과 확인

**result_test1.csv** 파일이 생성됩니다:

```csv
ID,NAME,AGE,RESULT,DIFF_DETAIL
1,John,25,MATCHED,
2,Jane,30,MISMATCHED,AGE: '28' -> '30'
3,Bob,35,ONLY_IN_FILE1,
4,Alice,40,ONLY_IN_FILE2,
```

---

## 출력 결과

### 결과 파일 위치

- **파일명**: `result_<원본파일1명>`
- **위치**: 첫 번째 입력 파일과 동일한 디렉토리

**예시**:
- 입력: `C:\data\file1.csv`, `C:\data\file2.csv`
- 출력: `C:\data\result_file1.csv`

### 결과 파일 형식

출력 CSV 파일은 다음과 같은 구조를 가집니다:

| 컬럼 | 설명 |
|------|------|
| 원본 컬럼들 | 두 파일의 모든 컬럼 (합집합) |
| RESULT | 비교 결과 타입 |
| DIFF_DETAIL | 차이점 상세 정보 |

### RESULT 값 종류

| 값 | 의미 | 설명 |
|----|------|------|
| MATCHED | 완전 일치 | 키가 양쪽 파일에 존재하고 모든 값이 동일 |
| MISMATCHED | 값 불일치 | 키가 양쪽 파일에 존재하지만 일부 값이 다름 |
| ONLY_IN_FILE1 | 파일1에만 존재 | 해당 키가 첫 번째 파일에만 존재 |
| ONLY_IN_FILE2 | 파일2에만 존재 | 해당 키가 두 번째 파일에만 존재 |

### DIFF_DETAIL 형식

불일치한 컬럼의 상세 정보를 다음 형식으로 표시합니다:

```
컬럼명: '값1' -> '값2'; 컬럼명2: '값3' -> '값4'
```

**예시**:
```
AGE: '28' -> '30'; CITY: 'Seoul' -> 'Busan'
```

---

## 오류 처리

### 오류 1: 파일을 찾을 수 없음

```
입력 파일을 읽을 수 없습니다: file1.csv / file2.csv
```

**해결방법**: 파일 경로를 확인하고 파일이 존재하는지 확인합니다.

### 오류 2: 키 컬럼이 존재하지 않음

```
키 컬럼이 양쪽 헤더에 모두 존재해야 합니다. 누락: ID
```

**해결방법**: 지정한 키 컬럼이 두 CSV 파일의 헤더에 모두 존재하는지 확인합니다.

### 오류 3: 헤더가 비어있음

```
헤더가 비어 있습니다.
```

**해결방법**: CSV 파일의 첫 줄에 헤더가 있는지 확인합니다.

### 오류 4: 키 중복 발견

```
키 중복 감지(FILE1): 1001 (키전략=CompositeKey[ID])
```

**해결방법**: 입력 파일에서 중복된 키 값을 제거하거나 다른 키 컬럼을 사용합니다.

### 오류 5: Java가 설치되지 않음

```
'java' is not recognized as an internal or external command
```

**해결방법**: Java Runtime Environment (JRE) 8 이상을 설치합니다.

---

## 다른 PC에서 실행하기

### 1. 필요한 파일

다른 PC로 복사해야 할 파일:
- `csv-compare-app.jar` (약 10.8MB)

### 2. 환경 요구사항

- Java Runtime Environment (JRE) 8 이상 설치 필요
- 외부 라이브러리나 추가 설정 불필요 (모든 의존성 포함됨)

### 3. Java 설치 확인

```bash
java -version
```

정상 출력 예시:
```
java version "17.0.1" 2021-10-19 LTS
Java(TM) SE Runtime Environment (build 17.0.1+12-LTS-39)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.1+12-LTS-39, mixed mode, sharing)
```

### 4. 실행

```bash
java -jar csv-compare-app.jar 파일1.csv 파일2.csv
```

---

## 고급 사용법

### 대용량 파일 처리

대용량 파일 처리 시 Java 힙 메모리를 증가시킬 수 있습니다:

```bash
java -Xmx2G -jar csv-compare-app.jar file1.csv file2.csv ID
```

**옵션 설명**:
- `-Xmx2G`: 최대 힙 메모리를 2GB로 설정
- `-Xmx4G`: 최대 힙 메모리를 4GB로 설정

### 배치 파일 생성 (Windows)

**compare.bat**:
```batch
@echo off
java -jar csv-compare-app.jar %1 %2 %3
echo 비교 완료!
pause
```

**사용법**:
```batch
compare.bat data1.csv data2.csv ID
```

### 쉘 스크립트 생성 (Linux/Mac)

**compare.sh**:
```bash
#!/bin/bash
java -jar csv-compare-app.jar "$1" "$2" "$3"
echo "비교 완료!"
```

**사용법**:
```bash
chmod +x compare.sh
./compare.sh data1.csv data2.csv ID
```

---

## 성능 및 제한사항

### 성능

- **처리 속도**: 일반적으로 초당 수만 행 처리 가능
- **메모리**: 전체 파일을 메모리에 로드하므로 파일 크기에 비례

### 제한사항

1. **메모리 제약**: 전체 파일을 메모리에 로드
   - 권장 최대 파일 크기: 100만 행 이하
   - 대용량 파일은 `-Xmx` 옵션으로 메모리 증가 필요

2. **인코딩**: UTF-8만 지원
   - 다른 인코딩 파일은 사전에 UTF-8로 변환 필요

3. **데이터 타입**: 모든 값을 문자열로 처리
   - "1"과 "01"은 다른 값으로 인식
   - 숫자 비교 시 주의 필요

4. **공백 처리**: 값 비교 시 trim() 적용
   - 앞뒤 공백은 무시됨

---

## 문제 해결

### Q1. "Error: Could not find or load main class" 오류 발생

**원인**: JAR 파일이 손상되었거나 잘못 빌드됨

**해결방법**:
```bash
# JAR 파일 재빌드
.\mvnw.cmd clean compile assembly:single -Dmaven.test.skip=true
```

### Q2. OutOfMemoryError 발생

**원인**: 처리할 CSV 파일이 너무 큼

**해결방법**:
```bash
# 힙 메모리 증가
java -Xmx4G -jar csv-compare-app.jar file1.csv file2.csv ID
```

### Q3. 한글이 깨져서 출력됨

**원인**: 터미널 인코딩 설정 문제

**해결방법**:
```bash
# Windows PowerShell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
java -jar csv-compare-app.jar file1.csv file2.csv ID
```

### Q4. 결과 파일이 Excel에서 한글이 깨짐

**원인**: Excel의 기본 인코딩은 UTF-8 BOM 필요

**해결방법**: 
- 메모장으로 열어서 "UTF-8 BOM"으로 다시 저장
- 또는 Excel에서 "데이터 가져오기" 기능 사용하여 UTF-8로 지정

---

## 참고 문서

- [CSV 비교 시스템 상세 문서](./csv-compare-system.md)
- [AppJob 시스템](./AppJob.md)

---

## 버전 정보

- **버전**: 1.0.0
- **빌드 날짜**: 2025-12-07
- **JDK 버전**: 17
- **호환 JRE**: 8 이상
- **외부 의존성**: 없음 (모든 라이브러리 포함)

---

## 라이센스

이 프로그램은 내부 사용을 위해 개발되었습니다.

---

## 기술 지원

문제가 발생하거나 기능 추가가 필요한 경우 개발팀에 문의하세요.

