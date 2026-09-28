# ScrollBox

로컬 파일 탐색 + 텍스트/이미지 뷰어 Android 앱.

패키지: `com.bossxor.scrollbox`

| | |
|---|---|
| 패키지 | `com.bossxor.scrollbox` |
| 버전 | 1.0.1 (versionCode 2) |
| minSdk / targetSdk | 26 / 34 |
| 스택 | Kotlin, Jetpack Compose, Material 3, Room, DataStore |
| 저장소 | https://github.com/bossxor/tool_ScrollBox |
| 작업 모음 | https://bossxor.netlify.app/ |

## 다운로드

- 프로젝트 모음: [bossxor.netlify.app](https://bossxor.netlify.app/)
- [Release v1.0.1](https://github.com/bossxor/tool_ScrollBox/releases/tag/v1.0.1)
- APK: [ScrollBox-1.0.1-debug.apk](https://github.com/bossxor/tool_ScrollBox/releases/download/v1.0.1/ScrollBox-1.0.1-debug.apk)

```bash
adb install -r ScrollBox-1.0.1-debug.apk
```

## 주요 기능

### 파일 탐색기
- 폴더 탐색, 정렬(이름/크기/날짜 × 오름·내림)
- 복사 / 잘라내기 / 붙여넣기(이동) / 삭제 / 이름변경
- 폴더·빈 txt 생성, 다중 선택, 파일명 검색
- 경로 클립보드 복사, SAF 폴더 선택
- **zip 압축 해제**: 기본은 zip 이름 폴더 생성 후 풀기, 체크 해제 시 현재 폴더에 풀기
- 암호 zip(N22), 최근 파일 / 즐겨찾기

### 텍스트 뷰어
- 상하·좌우 터치 / 세로 스크롤, 이어읽기
- 본문 검색, 책갈피, 글자크기·줄간격·여백·테마색
- 밝기, 볼륨키 넘김, 화면 켜짐 유지, 다음/이전 파일
- 인코딩 자동 + 수동(UTF-8 / EUC-KR)
- 진행률 %, TTS, 줄번호, 하이라이트·메모
- 제스처 잠금, 맞춤 터치 영역
- epub 간단 텍스트 읽기, 외부 `text/plain` 열기

### 이미지 / 만화
- jpg/png/gif/webp 등, cbz 스크롤

### 설정 · 보안
- 테마: **시스템 / 라이트 / 다크**
- 앱 잠금: PIN, 패턴, 생체(지문/얼굴) + 폴백
- JSON 백업·복원

## 빌드

JDK 17 + Android SDK 필요.

```bash
cd ScrollBox
./gradlew.bat assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

로컬 빌드가 UNC 경로에서 실패하면 `C:\Temp\ScrollBox`에 복사 후 빌드해도 됩니다.

## 디렉터리

```
ScrollBox/          # Android 프로젝트
releases/           # 배포 APK (릴리스에도 첨부)
```

## 라이선스

개인/내부 사용 목적. 신규 구현입니다.
