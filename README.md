# PAS 전투 프로토타입

Android 네이티브 Java 기반의 가로형 4×3 턴제 전투 프로토타입이다.

## 실행 흐름

1. 앱에서 `용사후보 선택 · 전투 준비`를 누른다.
2. 왼쪽 목록의 번호를 입력해 6개 슬롯을 구성한다.
3. 필요하면 싱글 파티의 두 번째 캐릭터를 추가하고 전투를 시작한다.
4. `이동` 또는 스킬을 선택한 뒤 대상 칸/유닛을 고르고 `실행`한다.

디버그 패널은 설정 화면에서 개발 쿠폰 `PAS-DEV-2026`을 입력하면 열린다. 코드는 `DebugOptions.DEVELOPER_COUPON` 한 곳에서 변경한다.

## 구조

`BattleCommand → BattleEngine → BattleState → BattleResult/Event → UI 갱신` 흐름을 따른다. UI는 HP나 위치를 직접 변경하지 않으며, 로컬 입력은 `PlayerCommandSource` 계약을 통한다. 원격 입력도 같은 명령을 JSON으로 직렬화해 서버에 제출한다.

- `battle/`: 명령, 엔진, 상태, 턴, 결과
- `skill/`: 불변 설계도, 런타임 자원, 조합형 효과, 저장소
- `status/`: 공통 상태 인스턴스와 지속시간
- `unit/`: 고유 `unitId` 기반 플레이어/적
- `ai/`: 교체 가능한 적 AI 전략
- `ui/`: 상태 렌더링 및 입력 변환

## 싱글 파티와 온라인 협동

캐릭터 수와 접속 플레이어 수는 별도 개념이다.

- 1인 1캐릭터: 기존 기본 플레이. 싱글 규칙을 사용한다.
- 1인 2캐릭터: 하나의 `clientId`가 두 `unitId`를 소유한다. 싱글 규칙을 유지한다.
- 2인 협동: 서로 다른 `clientId`가 각 캐릭터를 소유한다. 이때만 온라인 협동 전용 효과를 사용한다.

`MultiplayerMatchFactory`가 로비 구성을 결정적 랜덤 시드와 함께 전투로 만들고, `AuthoritativeBattleSession`만 실제 `BattleEngine`을 변경한다. 서버는 명령마다 프로토콜 버전, 매치 ID, 클라이언트 소유권, 요청 ID, 예상 상태 리비전을 검사한다. 중복 요청은 한 번만 실행되며, 승인 후에는 전체 `BattleSnapshot`과 SHA-256 상태 다이제스트를 반환한다. 적 AI 역시 서버 세션에서 처리되므로 클라이언트마다 전투를 재계산하지 않는다.

`LoopbackBattleTransport`는 로컬 멀티 테스트용이다. 실제 Spring 서버 연결은 `network/RoomApiClient`, `WebSocketBattleTransport`, `OnlineBattleSession`이 담당한다. 서버 응답은 `RemoteBattleSnapshot` 화면 DTO로만 읽으며 클라이언트에서 엔진을 재실행하지 않는다. 재접속은 새 WebSocket 연결의 서버 `SYNC`를 사용한다. 서버 주소 기본값은 비어 있고 `-PpasServerUrl=https://...` 또는 개발 화면에서 설정한다.

## 온라인 연결 테스트 / 작업 재개

**최신 진행 상태와 미완료 목록은 [MULTIPLAYER_HANDOFF.md](MULTIPLAYER_HANDOFF.md)를 먼저 읽는다.** 서버의 9월 3일 인수인계 문서보다 이 문서가 최신이다.

Android Studio의 debug 앱에서 시작 화면 `Project-PAS` 제목 길게 누르기 → `온라인 서버 연결 테스트`. 방 생성/참가, 캐릭터 선택, 준비, 기본 공격/방어, 이동/턴 종료 및 재동기화를 검증할 수 있다. 완성 게임의 전체 스킬/룬 UI와 온라인 연결은 아직 별도 작업이다.

Cloud Run 연결은 [CLOUD_SETUP.md](CLOUD_SETUP.md)를 따른다. `pas-cloud.properties`에 공개 HTTPS 서버 주소를 한 번 설정하면 debug/release 모두 시작 화면에 `온라인 협동 베타`가 나타나고, 사용자는 PAS 접속 키만 입력한다. 키를 APK/BuildConfig에 넣지 않는다. 주소가 없는 release에서는 온라인 진입을 비활성화한다. 아직 실제 GCP 배포는 하지 않았다.

## 설계 기준과 현재 제한

연결된 Google Drive의 `PAS-전반 설계도`를 기준으로 화염·중독의 턴 종료 스택 처리, 방어전념의 싱글/멀티 제한, 광역 난도질, 구원의 맹세를 반영했다. `신념의 검`이 부여하는 정확한 화염 스택 수치는 문서에도 없으므로 기존 프로토타입 피해량을 유지한다. 실제 WebSocket 서버 배포와 저장 기능, 에피소드·챕터·이벤트 진행은 이번 범위에 포함하지 않는다.
