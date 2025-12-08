# IntelliJ IDEA 실행 가이드 (Run Dashboard & Run Configuration)

## 개요
IntelliJ IDEA에서 자주 실행하는 애플리케이션을 쉽게 관리하고 실행할 수 있도록 Run Configuration을 설정했습니다.

## ⚠️ 버전별 차이점

### Ultimate Edition (얼티메이트 버전)
- ✅ **Run Dashboard** 기능 사용 가능
- ✅ `Alt + 5`로 Run Dashboard 열기 가능
- ✅ 시각적 대시보드에서 여러 애플리케이션 관리

### Community Edition (커뮤니티 버전)
- ❌ **Run Dashboard** 기능 미지원
- ✅ **Run Configuration** 기능은 동일하게 사용 가능
- ✅ 툴바 드롭다운 및 단축키로 실행 가능

---

## 커뮤니티 버전 사용자를 위한 가이드

### 방법 1: 툴바에서 실행 (가장 간편)
1. 상단 툴바의 **Run Configuration 드롭다운** 클릭
2. 목록에서 `AppJob`, `AppStepJob`, 또는 `ScanSourceTarget` 선택
3. 옆의 **초록색 실행 버튼** 클릭 또는 `Shift + F10`

### 방법 2: 단축키로 실행 (가장 빠름)
1. 실행할 클래스 파일 열기 (예: `AppJob.java`)
2. `Ctrl + Shift + F10`: 현재 파일 실행
3. `Shift + F10`: 마지막으로 실행한 Configuration 재실행
4. `Shift + F9`: 마지막으로 실행한 Configuration 디버그

### 방법 3: Run 패널 사용
1. `Alt + 4`를 눌러 Run 패널 열기
2. 우측 상단의 Run Configuration 드롭다운에서 선택
3. 실행 버튼 클릭

---

## 얼티메이트 버전 사용자를 위한 Run Dashboard

### Run Dashboard 활성화 방법

#### 방법 1: 단축키로 열기
- **Windows/Linux**: `Alt + 5`
- **macOS**: `⌘ + 5`

#### 방법 2: 메뉴로 열기
1. 상단 메뉴: `View → Tool Windows → Run Dashboard`
2. 또는 하단의 Run 탭을 클릭

### Run Dashboard에서 실행
1. `Alt + 5`를 눌러 Run Dashboard 열기
2. 좌측 목록에서 원하는 애플리케이션 선택
3. 다음 중 하나 선택:
   - **Run** (초록색 재생 버튼) 또는 `Ctrl + Shift + F10`
   - **Debug** (벌레 아이콘) 또는 `Shift + F9`
   - 우클릭 → `Run` 또는 `Debug`

---

---

## 설정된 Run Configurations (모든 버전 공통)

프로젝트에 다음 Run Configuration이 영구적으로 설정되어 있습니다:

### 1. **AppJob**
- **클래스**: `file.job.AppJob`
- **용도**: SQL 파일에서 소스/타겟 테이블 추출 (전체 요약)
- **출력**: 
  - 텍스트 파일 (`*_sql_tables.txt`)
  - CSV 요약 파일 (`summary.csv`)

### 2. **AppStepJob**
- **클래스**: `file.job.AppStepJob`
- **용도**: SQL 파일에서 STEP별 소스/타겟 테이블 추출
- **출력**: 텍스트 파일 (`*_step_tables.txt`)

### 3. **ScanSourceTarget**
- **클래스**: `service.scanSourceTarget.ScanSourceTarget`
- **용도**: 소스/타겟 테이블 스캔 및 분석

---

## 공통 실행 방법 (모든 버전)

### 방법 1: 툴바에서 실행 (권장)
1. 상단 툴바의 **Run Configuration 드롭다운** 클릭
2. 목록에서 `AppJob`, `AppStepJob`, 또는 `ScanSourceTarget` 선택
3. 옆의 **초록색 실행 버튼** 클릭 또는 `Shift + F10`

### 방법 2: 단축키로 실행 (가장 빠름)
1. 실행할 클래스 파일 열기 (예: `AppJob.java`)
2. `Ctrl + Shift + F10`: 현재 파일 실행
3. `Shift + F10`: 마지막으로 실행한 Configuration 재실행
4. `Shift + F9`: 마지막으로 실행한 Configuration 디버그

---

---

## 추가 팁 (모든 버전)

### Run Configuration 즐겨찾기 추가
1. `Run → Edit Configurations...` 열기
2. 좌측에서 원하는 Configuration 선택
3. 오른쪽 상단의 **별 아이콘(⭐)** 클릭
4. 툴바 드롭다운 상단에 고정됨

### 새로운 Run Configuration 추가
1. `Run → Edit Configurations...`
2. 좌측 상단의 `+` 버튼 클릭
3. `Application` 선택
4. 다음 정보 입력:
   - **Name**: 설정 이름
   - **Main class**: 실행할 클래스 (예: `file.job.AppJob`)
   - **Module**: `CardGcp` 선택
5. `OK` 클릭

### Run Dashboard 그룹 설정 (얼티메이트 버전 전용)
Run Dashboard에서는 다음과 같이 그룹화됩니다:
- **Application**: Java 애플리케이션
- **Spring Boot**: Spring Boot 애플리케이션 (해당되는 경우)

## 파일 위치

Run Configuration 파일들은 다음 위치에 저장됩니다:
```
.idea/runConfigurations/
├── AppJob.xml
├── AppStepJob.xml
└── ScanSourceTarget.xml
```

이 파일들은 Git에 커밋되어 있어 팀원들과 공유됩니다.

## 문제 해결

### Run Dashboard가 보이지 않는 경우 (얼티메이트 버전)
- **커뮤니티 버전**: Run Dashboard는 지원되지 않습니다. 위의 "커뮤니티 버전 사용자 가이드"를 참고하세요.
- **얼티메이트 버전**: 
  1. `View → Tool Windows → Run Dashboard` 클릭
  2. 또는 `.idea/workspace.xml`에 `RunDashboard` 컴포넌트가 있는지 확인

### Run Configuration이 목록에 나타나지 않는 경우
1. IntelliJ IDEA 재시작
2. `File → Invalidate Caches and Restart` 실행
3. `.idea/runConfigurations/` 폴더가 존재하는지 확인

### 실행 버튼이 비활성화된 경우
1. 프로젝트를 Maven으로 재빌드: `Ctrl + F9`
2. `File → Project Structure → Modules`에서 모듈이 올바르게 설정되었는지 확인

## 참고 자료
- [IntelliJ IDEA Run/Debug Configurations](https://www.jetbrains.com/help/idea/run-debug-configuration.html)
- [IntelliJ IDEA Run Dashboard](https://www.jetbrains.com/help/idea/run-dashboard.html)

