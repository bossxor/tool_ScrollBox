# ScrollBox

로컬 파일 탐색 + 텍스트/이미지 뷰어 Android 앱.

패키지: `com.bossxor.scrollbox`

| | |
|---|---|
| 패키지 | `com.bossxor.scrollbox` |
| 버전 | **1.0.2** (versionCode **102**) |
| minSdk / targetSdk | 9 / 28 |
| 배포 | TIViewer 기반 패치 APK (광고·웹 제거, 패키지·서명 교체) |
| 저장소 | https://github.com/bossxor/tool_ScrollBox |
| 작업 모음 | https://bossxor.netlify.app/ |

## 다운로드

- 프로젝트 모음: [bossxor.netlify.app](https://bossxor.netlify.app/)
- APK: [`ScrollBox-from-TIViewer.apk`](https://github.com/bossxor/tool_ScrollBox/raw/main/releases/ScrollBox-from-TIViewer.apk) (v1.0.2)

```bash
adb install -r releases/ScrollBox-from-TIViewer.apk
# 서명 충돌 시
adb uninstall com.bossxor.scrollbox
adb install releases/ScrollBox-from-TIViewer.apk
```

## 주요 기능

원본 뷰어 기능을 유지하고, 아래만 변경·추가했다.

### 유지 (원본)
- 파일 탐색: 정렬·필터·복사/이동/삭제/이름변경·폴더·txt 생성·검색·홈 경로 등
- 텍스트 뷰어: 넘김 모드·이어읽기·검색·책갈피·테마·밝기·볼륨키·인코딩 등
- 이미지/만화(zip·cbz 등), 최근 목록, 설정

### 추가
- 앱 잠금: **PIN** + **패턴**(3×3 탭) + **생체**(지문/얼굴, PIN/패턴 폴백)

### 제거·숨김
- 광고 / 인앱결제 / 푸시
- 웹·커뮤니티 탭
- 사이드바: 로그인, 공지, FAQ, 관리자 문의, 백업·복구·초기화

## 빌드 (패치 APK)

작업 트리는 보통 `C:\Temp\ScrollBoxApk` (apktool 디컴파일).

```bash
apktool b C:\Temp\ScrollBoxApk -o ScrollBox-unsigned.apk
# zipalign + apksigner (scrollbox.keystore, alias scrollbox)
```

## 디렉터리

```
releases/                 # 배포 APK
ScrollBox/                # (실험용) Compose 프로젝트 — 현재 배포본 아님
TIViewer2.ver.0.9.4/      # 디컴파일 원본 (git 제외 권장)
```

## 라이선스

개인/내부 사용 목적.
