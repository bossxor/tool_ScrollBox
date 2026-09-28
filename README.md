# ScrollBox

로컬 파일 탐색 + 텍스트/이미지 뷰어 Android 앱.

패키지: `com.bossxor.scrollbox`

| | |
|---|---|
| 패키지 | `com.bossxor.scrollbox` |
| 버전 | **1.0.5** (versionCode **105**) |
| minSdk / targetSdk | 9 / 28 |
| 기반 | legacy viewer patch (패키지·브랜딩 교체) |
| 저장소 | https://github.com/bossxor/tool_ScrollBox |
| 작업 로그 | https://bossxor.netlify.app/ |

## 다운로드

- 작업 로그: [bossxor.netlify.app](https://bossxor.netlify.app/)
- APK: [`ScrollBox.apk`](https://github.com/bossxor/tool_ScrollBox/raw/main/releases/ScrollBox.apk) (v1.0.5)

```bash
adb install -r releases/ScrollBox.apk
# 서명 충돌 시
adb uninstall com.bossxor.scrollbox
adb install releases/ScrollBox.apk
```

## 주요 기능

기존 뷰어 기능을 유지하고, 아래를 정리·추가했다.

### 유지 (핵심)
- 파일 탐색: 정렬·필터·복사/이동/삭제/이름변경·폴더·txt 생성·검색·홈 경로 등
- 텍스트 뷰어: 한글 인코딩·이어읽기·검색·책갈피·테마·밝기·볼륨키·전체화면(내비 숨김) 등
- 이미지/만화(zip·cbz 등), 최근 목록, 설정

### 추가
- 앱 잠금: **PIN** + **패턴**(3×3 드래그) + **생체**(지문/얼굴, PIN/패턴 선행)
- 사이드바 하단 버전 표시 (`ScrollBox 1.0.5`)

### 숨김·제거
- 광고 / 인앱결제 / 푸시
- 웹 커뮤니티 탭
- 사이드바: 로그인, 공지, FAQ, 버전정보 화면, 백업/복원/초기화
- 웹소설 설정

## 빌드 (패치 APK)

작업 트리는 로컬 `C:\Temp\ScrollBoxApk` (apktool 디컴파일).

```bash
apktool b C:\Temp\ScrollBoxApk -o ScrollBox-unsigned.apk
# zipalign + apksigner (scrollbox.keystore, alias scrollbox)
```

## 디렉터리

```
releases/                 # 배포 APK
ScrollBox/                # (실험) Compose 프로젝트 — 현재 배포본 아님
```

## 라이선스

개인/사내 사용 목적.