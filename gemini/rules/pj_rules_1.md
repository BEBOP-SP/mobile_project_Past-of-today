# 프로젝트 규칙: 과거의 오늘 (TimePop)

## 1. 개발 환경 및 필수 제약 조건
- **핵심 프레임워크**: Android Studio (Kotlin)
- **필수 구현 사항 (기말고사 채점 기준)**:
    1. **8장 어댑터 뷰**: 메인 화면 및 스크랩 보관함에 `RecyclerView`와 커스텀 어댑터 필수 적용.
    2. **10장 지역화 (Localization)**: UI 텍스트는 하드코딩 금지, `strings.xml`로 철저히 분리.
    3. **12장 데이터베이스**: 사용자 계정 및 스크랩 데이터를 위한 DB(Firebase 혹은 SQLite) 연동 필수.

## 2. 코드 품질 및 구현 지침
- **모듈화 (Packages)**: 코드를 한 파일에 몰아넣지 말고 기능별로 패키지를 엄격히 분리할 것.
    - `ui`: Activity, Fragment, Popup
    - `adapter`: RecyclerView Adapters
    - `api`: Network, Wikipedia API Service
    - `db`: Database Helpers / Firebase Services
    - `receiver`: BroadcastReceiver (Screen-On 감지)
- **주석 (Critical)**: 교수님 평가 항목에 '주석'이 포함되어 있으므로, 새로 생성하거나 수정하는 모든 Kotlin 코드에는 기능 설명, 파라미터 역할, 알고리즘 의도가 담긴 **상세한 한글 주석**을 무조건 작성할 것.

## 3. 예외 처리 규칙
- Wikipedia API 호출 시 데이터가 없거나, `image_url`이 null인 경우 앱이 크래시 나지 않도록 기본 플레이스홀더 이미지나 그래디언트 배경을 적용하는 방어적 코딩(Try-Catch)을 적용할 것.