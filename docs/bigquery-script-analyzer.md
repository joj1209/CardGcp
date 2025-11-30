# BigQuery Script Analyzer 개요

`service.BigQuery.BigQueryScriptAnalyzer`는 BigQuery로 변환된 배치 SQL 스크립트를 정적 분석하여 STEP 블록별로 사용 테이블과 BEGIN/END 구조를 확인하는 경량 도구입니다. 인터넷이 단절된 JDK8 환경에서도 동작하도록 표준 라이브러리만 사용합니다.

## 특징
- **STEP 분리**: `STEP001 BEGIN ... END` 패턴을 찾아 각 블록을 맵 형태로 보관합니다.
- **구문 검증**: 블록 내용이 비어 있는지 확인해 BEGIN/END 짝의 기본 무결성을 점검합니다.
- **테이블 추출**: `FROM`, `JOIN`, `INTO`, `UPDATE`, `MERGE INTO` 구문에서 등장하는 테이블 명을 모두 수집합니다.
- **출력**: STEP별로 테이블 목록과 BEGIN/END 유효성 여부를 콘솔에 요약 출력합니다.

## 사용 방법
1. 분석 대상 SQL 파일 경로를 `BigQueryScriptAnalyzer.main`의 `filePath` 상수에 설정합니다.
2. 다음 명령으로 실행합니다.
   ```powershell
   cd "D:\11. Project\02. Java\backend\CardGcp"
   ./mvnw -q -DskipTests exec:java -Dexec.mainClass=service.BigQuery.BigQueryScriptAnalyzer
   ```
3. 콘솔에 STEP별 분석 결과가 순서대로 표시됩니다.

## 제한 사항 및 향후 과제
- BEGIN/END 중첩 구조는 미지원이므로 v2 이후 분석기로 확장 필요합니다.
- 컬럼, 조건절, 에러 검출 등 고급 기능은 `BigQueryScriptAnalyzer2~4`에서 제공됩니다.
- 현재는 파일 경로를 코드에 직접 작성하므로 CLI 인자나 GUI 입력이 필요하면 추후 버전으로 이동하거나 개선해야 합니다.

