# PAS 온라인 연결 인수인계 — 2026-09-04

> 최신 인계(2026-09-08): APP_SERVER_INTEGRATION.md 및 CLOUD_RESUME.md가 우선한다. 비공개 Cloud Run 배포, 개인별 일회용 초대, 실제 일반 대기실/서버 snapshot HUD 연결, 실제 서버 통신 검증 19개 완료. 인터넷 공개 승인과 실기기 2대 검증은 남았다. 아래의 미배포/공용 베타 키/미리보기 안내는 과거 기록이다.

다음 작업은 이 문서를 먼저 읽고 아래 파일의 변경분만 확인한다. 전체 전투 코드를 다시 리뷰하거나 완료된 서버를 다시 만들지 않는다.

## 추가 완료: Cloud Run 키 입력 방식

- `CLOUD_SETUP.md` 참고. 루트 `pas-cloud.properties`의 `pasServerUrl`만 개발자가 한 번 설정하고 빌드한다. 파일은 Git 제외, 빈 템플릿 준비. 키 저장 금지.
- 주소가 설정되면 시작 화면에 온라인 협동 베타 진입 버튼 표시. 주소 입력란 숨김, PAS 키 필수. release에서도 사용 가능하다. 주소가 없는 release는 진입 차단, debug는 기존 로컬 테스트 가능.
- `OnlineTestActivity`는 **app/src/main/java/com/pas/game/ui/activity/OnlineTestActivity.java로 이동**했다. 아래의 과거 debug 전용 경로를 다시 만들지 않는다.
- 서버에 `config/CloudDeploymentSafety.java`와 테스트 추가: Cloud Run K_SERVICE 환경에서 PAS_BETA_ACCESS_KEY가 없거나 32~256자 공백 없는 ASCII 조건을 만족하지 않으면 시작 실패. Secret Manager 주입. 로컬 빈 키 개발은 유지.
- 최종 검증: Android **126개 통과/skip 0** (PAS_TEST_BETA_KEY를 설정한 로컬 서버로 실행), 서버 **10개 통과**, debug APK와 release Java 컴파일 성공, lint 오류 0/경고 60.
- `LiveServerConnectionTest`는 이제 선택적 PAS_TEST_BETA_KEY 환경 변수로 로컬 키 인증도 검증한다. URL 속성 없으면 실서버 테스트 4개 skip; 키가 없으면 추가 인증거부 테스트 1개 skip. 실제 키를 소스/빌드설정에 넣지 않는다.
- 클라우드 키 전용 UI는 `.invalid` 검증 주소 APK로 설치/화면 확인 후, **최종 APK는 빈 주소 설정으로 다시 빌드**했다. 실제 URL을 받지 않았으므로 임의 주소를 최종 APK에 남기지 않았다. 캡처: `build/pas-cloud-key-only.png`.
- **아직 실제 Google Cloud 배포/인터넷 접속을 검증한 것은 아니다.** 기존 전체 스킬/룬 및 HUD의 온라인 연결 미완료 항목도 그대로 남는다.

## 직전 작업과 달라진 점

9월 3일 서버 문서 `C:\Users\USER\IdeaProjects\PAS_server\HANDOFF.md`의 미완료 1번(실제 Android REST/WebSocket)은 이번에 구현했다. 2번의 스냅샷 수신 DTO와 3번의 **개발용** 입장 화면도 추가했다. 단, 기존 게임의 전체 전투 화면/로드아웃 선택과 온라인 연결까지 완료한 것은 아니다.

## 실제 반영 파일

Android 프로젝트: `C:\Users\USER\AndroidStudioProjects\project_pas`

- `app/src/main/java/com/pas/game/network/ServerEndpoint.java`: 동일 origin의 REST/WS 주소, HTTPS 기본, 디버그 로컬 HTTP만 허용.
- `network/RoomApiClient.java`: 방 생성/참가/구성/준비/상태 API. 비동기 콜백, 키는 헤더, 리디렉션/실패 자동 재전송 금지.
- `network/WebSocketBattleTransport.java`: 인증 헤더, 서버 초기 SYNC/상대 PUSH, 요청 결과 매칭, 중복 터치 방지, 15초 응답 시간 제한, 최대 5회 백오프 재접속. 끊긴 명령은 자동 재실행하지 않는다.
- `network/RemoteBattleSnapshot.java`: 서버 JSON -> 표시 전용 DTO, SHA-256/revision 검증. 엔진 재구성/패시브 재적용 금지.
- `network/OnlineBattleSession.java`: UUID 요청 ID, 내 유닛 소유권/대기 검사, 오래된 revision으로 표시가 되돌아가지 않음. 콜백/사용은 동일 UI 실행기로 직렬화한다.
- `app/src/main/java/com/pas/game/ui/activity/OnlineTestActivity.java`: 접속/로비/기본 전투 확인 화면. 1인1캐릭터/1인2캐릭터/2인협동 선택. 기본 공격/방어만 장착, 스킬/룬 전체 선택은 미연결. 서버 상태만 표시.
- `app/src/debug/AndroidManifest.xml`: 디버그에만 로컬 HTTP 허용. 온라인 Activity는 main manifest에서 exported=false.
- `app/src/main/java/com/pas/game/ui/activity/MainActivity.java`: 설정에 debug 전용 온라인 테스트 진입 버튼 한 개만 추가. 기존 싱글 흐름은 그대로.
- `app/build.gradle`, `gradle/libs.versions.toml`: BuildConfig 서버 주소, OkHttp/Gson/MockWebServer 버전 고정, opt-in 실서버 테스트 속성. **서명 변경 없음.**
- `app/src/test/java/com/pas/game/NetworkClientTest.java`: HTTP/WS 헤더, 리디렉션 차단, 재접속, 명령 중복 방지, digest/revision 회귀 테스트 10개.
- `app/src/test/java/com/pas/game/LiveServerConnectionTest.java`: 실제 로컬 Spring 서버 대상으로 3개 모드/상대 PUSH/재동기화 테스트. 서버 URL 속성이 없으면 이 3개만 의도적으로 skip.
- `scripts/coop-peer-smoke.ps1`: 로컬 협동 방에 P2 사냥꾼으로 참가하고 자기 턴을 한 번 종료하는 테스트 동료. 토큰은 메모리에만 둔다.
- `scripts/android-online-smoke.ps1`: 실행 중인 로컬 8090 서버와 설치된 debug APK를 사용해 UI 문구 기반으로 1인 2캐릭터 방 생성/양쪽 턴 종료/화면 캡처를 반복 검증한다. APK 설치는 별도로 한다.

새 네트워크 코드는 `multiplayer/`가 아니라 `network/`에 있다. 서버의 전투 코어 복사 스크립트 대상에 Android 네트워크/Activity 코드를 추가하지 않는다. 전투 코어는 변경하지 않았고 서버에는 클라우드 키 설정 검증만 추가했다.

## 검증 근거

- 기존 112개 + 통신 테스트 10개 + 실제 로컬 서버 테스트 3개 = **125개 통과, 실패/오류/skip 0** (실서버 속성을 준 실행).
- debug APK 빌드 성공, 기존 설치 위에 `adb install -r` 성공. 서명 파일/키/설정은 바꾸지 않았다.
- 에뮬레이터 APK + 별도 PowerShell WebSocket 동료: host START revision 1 -> host 턴 종료 -> guest PUSH revision 2 -> guest 턴 종료 -> APK에 **라운드 2, 용사후보 P1, revision 3** 표시 확인.
- AndroidRuntime:E 크래시 출력 없음.
- 최종 APK의 UI 자동 검증에서도 1인 2캐릭터 P1 -> P2 -> 라운드 2 P1, revision 3 확인. 캡처: `build/pas-online-final.png`. 조작/재연결/상세 로그 버튼이 화면 안에 들어오는 것을 시각 확인했다.
- `compileReleaseJavaWithJavac` 성공. 최종 lint는 오류 0, 경고 60 (기존 경고와 의존성 버전/디버그 화면 방향·문구 경고 포함).
- 최종 빌드/화면 재확인 결과는 아래 검증 명령과 `app/build/test-results/testDebugUnitTest`, `app/build/reports/lint-results-debug.txt`로 확인한다. 전체 코드를 다시 읽을 필요 없다.
- 실제 GCP/인터넷/실기기 2대 테스트는 아직 하지 않았다. 에뮬레이터 2대 테스트로 과장하지 않는다.

## 바로 실행하기

서버는 기존 JAR 사용 가능 (서버를 변경하면 먼저 해당 프로젝트에서 test bootJar):

```powershell
& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' -jar 'C:\Users\USER\IdeaProjects\PAS_server\build\libs\PAS_server-0.0.1-SNAPSHOT.jar' --server.address=127.0.0.1 --server.port=8090
```

이 PC에서는 에뮬레이터의 `10.0.2.2` 접속이 실패했다. 공개 포트를 열지 않고 아래 연결로 실제 APK 검증에 성공했다:

```powershell
& 'C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s emulator-5554 reverse tcp:8090 tcp:8090
```

앱 시작 화면 Project-PAS 제목 길게 누르기 → 온라인 서버 연결 테스트 → 주소 `http://127.0.0.1:8090`, 베타 키 비움 → 방 생성/참가 → 캐릭터 → 준비.

PowerShell 테스트 동료는 Android에서 만든 COOP 방 코드를 사용한다:

```powershell
.\scripts\coop-peer-smoke.ps1 -RoomCode ABC234
```

60초 안에 Android에서 준비하고 턴 종료한다. 스크립트가 끝나면 동료는 연결을 닫지만 서버 방은 남는다. 재실행으로 같은 방에 새 guest를 추가하지 않는다. 새 방을 만들어 검증한다.

검증 (현재 Android 프로젝트에서):

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME=(Resolve-Path '.gradle-user-home').Path
$env:ANDROID_USER_HOME='C:\Users\USER\.android'
.\gradlew.bat testDebugUnitTest assembleDebug compileReleaseJavaWithJavac lintDebug -PpasTestServerUrl=http://127.0.0.1:8090 --no-daemon
```

서버가 없으면 `-PpasTestServerUrl=...`를 생략한다 (나머지 테스트는 실제 서버 없이 동작). build/config에는 키를 넣지 않는다. `pasServerUrl`은 앱 기본 주소이고 `pasTestServerUrl`은 JVM 테스트 전용이다. 혼동하지 않는다.

## 아직 해야 할 것 — 여기서 이어가기

1. **기존 캐릭터 선택 화면의 전체 스킬/룬 로드아웃을 RoomApiClient.CharacterLoadout으로 변환하고 실제 온라인 로비와 연결.** 현재 테스트 Activity는 기본 장비만 사용한다.
2. 기존 BattleBoardView/전투 HUD가 로컬 BattleState와 RemoteBattleSnapshot을 공통 표시 모델로 읽도록 어댑터화. 원격 경로에서 BattleEngine/AI/피해/패시브를 재계산하지 않는다. 지금 테스트 화면은 정식 전투 UI를 대체하지 않는다.
3. 전체 스킬 대상으로 서버가 선택 가능한 대상/칸/마법사 선택지를 내려주는 UI 계약을 보강. 현재 snapshot에는 wizardOptionIds/validWizardReturnTiles 같은 선택 후보 정보가 없다. 클라이언트에 엔진 복제 금지.
4. 토큰/방 상태의 앱 프로세스 종료 후 복원, 로비 나가기/방 수명/동료 접속 상태. 현재 자동 재접속은 살아 있는 Activity/세션 안에서만 된다.
5. APK 두 대의 E2E와 실기기 검증. 이번에는 Android 에뮬레이터 한 대 + 별도 소켓 동료, JVM 클라이언트 두 개로 확인했다.
6. GCP 프로젝트 ID/리전/공개 범위 확인 후 배포. 기존 Docker/Cloud Build 준비만 있고 배포는 안 됐다. 메모리 방 저장소라 서버 재시작 시 방 소멸. Redis/다중 인스턴스/지속성은 미구현.

## 충돌 방지

- git 미커밋 기존 변경이 많다. 이번 수정과 이전 작업을 섞어 되돌리거나 초기화하지 않는다.
- 이번 작성 파일은 network/, debug Activity, 두 테스트와 문서. 기존 battle/skill/effect 등의 변경은 직전 작업 것이므로 손대지 않았다.
- 코어를 수정할 때만 Android 원본 -> 서버 `scripts/sync-battle-core.ps1` 동기화와 양쪽 테스트 수행. 현재 네트워크 추가만으로 서버 전체 재빌드는 불필요.
- Android 서명과 사용자 `.android/debug.keystore`를 바꾸지 않는다. 설치 충돌을 이유로 사용자 앱을 임의 삭제하지 않는다.
- debug 연결 테스트 화면은 임시 검증 도구다. 이를 정식 화면으로 간주하여 기존 디자인을 버리지 않는다.

## 다음 요청문

> MULTIPLAYER_HANDOFF.md를 먼저 읽고, 기존 완료 항목을 다시 구현하거나 전체 재리뷰하지 마. 미완료 1번인 전체 스킬/룬 로드아웃과 실제 온라인 로비 연결부터 이어가. 기존 싱글 모드·전투 규칙·APK 서명을 유지해.
