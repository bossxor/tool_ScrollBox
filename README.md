# ScrollBox

로컬 파일 탐색 + 텍스트/이미지 뷰어 Android 앱.

패키지: `com.bossxor.scrollbox`

| | |
|---|---|
| 패키지 | `com.bossxor.scrollbox` |
| 버전 | **1.0.11** (versionCode **111**) |
| minSdk / targetSdk | 9 / 28 |
| 기반 | legacy viewer patch (패키지·브랜딩 교체) |
| 저장소 | https://github.com/bossxor/tool_ScrollBox |
| 작업 로그 | https://bossxor.netlify.app/ |

## 다운로드

- 작업 로그: [bossxor.netlify.app](https://bossxor.netlify.app/)
- APK: [`ScrollBox.apk`](https://github.com/bossxor/tool_ScrollBox/raw/main/releases/ScrollBox.apk) (v1.0.11)

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
- 앱 잠금: **PIN 또는 패턴 중 택1**(+ 선택적 **생체**: 지문 성공 시 바로 입장, 실패 시 PIN/패턴) + **재잠금 타임아웃**(즉시/1/5/15분)
- 사이드바 하단 버전 표시 (`ScrollBox 1.0.11`)
- 뷰어: 하단 내비만 숨기고, **상태바 표시함** 설정 존중
- **페이지 넘김**: 상하 터치 시 1줄 겹침 — 스크롤 후 넘김에서 한 줄 건너뜀 완화
- **백업/복원**: 설정·최근목록·책갈피(이어읽기 위치)·테마 설정값(배경 이미지 제외)·파일별 인코딩 — 생성 직후 공유 시트
- Android 11+: **모든 파일 접근** 안내 (파일 목록 진입 시)
- 텍스트: 경로별 인코딩 자동 고정(감지 결과 저장·재오픈 시 복원)

- **1.0.11**: 시작 시 지문 후 PIN/패턴 재입력되던 이중 잠금 화면 수정, 잠금 방식 택1 UI, 사이드바 확대·개선
- **사이드바**: 폴더 즐겨찾기 바로가기(길게 눌러 해제), 뷰어 글꼴 선택(고딕/명조/필기체 등)
- **파일 목록**: 파일별 읽음 진행률(%) 표시

### 숨김·제거
- 광고 / 인앱결제 / 푸시
- 웹 커뮤니티 탭
- 사이드바: 로그인, 공지, FAQ, 버전정보 화면
- 웹소설 설정

## 빌드 (패치 APK)

작업 트리: `_diff/v105` (apktool).

```bash
apktool b <작업폴더> -o ScrollBox-unsigned.apk --use-aapt1
# d8로 SbUx.dex 병합 → zipalign + apksigner (scrollbox.keystore, alias scrollbox)
```

## 디렉터리

```
releases/ScrollBox.apk   # 배포용 서명 APK
_diff/v105/              # apktool 패치 소스
```

## patch/java

1.0.11에서 추가한 Java 헬퍼(LockUi, SideExtra). javac → d8 → baksmali 로 smali 변환 후 apktool 작업 트리에 복사.
