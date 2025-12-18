# 헬스장 운동기구 사용 감지 이미지 블로그 시스템 설계 문서

## 1. 목적

본 프로젝트는 헬스장 내 운동기구(런닝머신, 벤치프레스, 스쿼트랙 등)에 카메라와 Edge 장치를 설치하고, YOLOv5 객체 검출을 활용하여 사용 상황의 변화를 자동으로 기록하는 시스템을 구현한다.

사용 흐름은 다음과 같다.

-   Edge가 카메라 영상에서 객체를 검출하고, 특정 객체의 변화(Change Detection)를 감지한다.
-   변화가 발생한 시점의 이미지와 검출 결과를 Django 서버에 REST API로 게시한다.
-   서버는 이미지 블로그 형태로 이벤트를 저장하고, 이미지 목록 및 상세 조회 API를 제공한다.
-   Android 앱은 목록 조회와 상세 조회 중심의 UI로 사용 기록을 확인할 수 있다.

## 2. 필요성

헬스장 운영 및 사용 패턴 파악에는 다음과 같은 문제점이 존재한다.

-   어떤 운동기구가 언제 많이 쓰이는지 객관적인 로그가 부족하다.
-   피크 시간대나 기구별 사용률을 데이터로 확인하기 어렵다.
-   직원이 수동으로 확인하거나 회원이 직접 보고 판단해야 한다.
-   운영 측면에서 장비 배치 변경, 추가 구매, 유지보수 우선순위를 근거 있게 정하기 어렵다.

본 시스템은 카메라 기반 자동 인식을 통해 사용 기록을 수집하고 저장함으로써,

-   실시간 또는 기록 기반의 운영 판단 근거를 제공하고
-   별도 센서 부착 없이 영상 기반만으로 자동화된 로그를 남기며
-   모바일 앱에서 빠르게 목록과 기록을 확인하는 환경을 제공한다.

## 3. 시스템 구성 및 기능

### 3.1 전체 구조

-   Edge System: Python 기반, YOLOv5로 객체 검출 및 Change Detection 수행, 서버에 이벤트 게시
-   Service System: Django 기반, 보안키 인증, Image Blog 저장/관리, REST API 제공, PythonAnywhere 배포
-   Client System: Android Native, 이미지 리스트/상세 UI 제공, 서버 API로 데이터 획득

### 3.2 Edge System 기능 및 역할

Edge System은 운동기구별로 설치된 카메라 영상을 처리한다.

기본 기능

-   Python 기반 실행
-   YOLOv5 pretrained model 사용
-   MS COCO 80개 클래스 검출 가능
-   서버로 이벤트 게시를 위한 HTTP RESTful API 호출

신규 및 핵심 기능

-   한 종류의 객체를 기준으로 Change Detection 수행

    -   본 프로젝트에서는 person 클래스를 핵심 대상으로 사용한다.
    -   0 → 1 이상: 사용 시작 이벤트
    -   1 이상 → 0: 사용 종료 이벤트

-   이벤트 발생 시점만 서버로 전송

    -   변화가 없으면 업로드하지 않아 트래픽과 저장 비용을 절감한다.

Edge에서 관리하는 주요 데이터

-   captured_at: 캡처 시각
-   detections: YOLO 검출 결과(JSON)
-   change_info: Change Detection 결과(JSON)
-   image: 캡처 이미지 파일
-   security_key: 인증용 보안키
-   machine_id: 운동기구 식별자

Edge → Server 업로드 예시

-   POST /api/machines/{machine_id}/events/
-   multipart/form-data

    -   image
    -   detections (JSON 문자열)
    -   change_info (JSON 문자열)
    -   captured_at

### 3.3 Service System 기능 및 역할

Service System은 Django 기반의 서버로 PythonAnywhere에 배포한다.

공통 기능

-   보안키 기반 인증 및 로그인
-   Image Blog 저장 및 관리
-   게시(업로드)용 RESTful API 제공

신규 추가 기능

-   Image 목록 및 획득을 위한 RESTful API 제공

    -   Android 앱이 이미지 리스트를 가져오고 상세 이미지를 획득하는 데 사용한다.

서버 주요 모델 개념

-   ApiUser

    -   security_key 기반 사용자 인증

-   GymMachine

    -   운동기구 메타정보(이름, 위치 등)

-   MachineEvent

    -   기구별 사용 이벤트 로그(이미지, 이벤트 타입, 감지 결과, 시간 정보)

서버 REST API 구성

-   POST /api/auth/login/

    -   보안키 검증

-   GET /api/machines/

    -   운동기구 목록 제공

-   POST /api/machines/{machine_id}/events/

    -   Edge가 사용 시작/종료 이벤트와 이미지를 업로드

-   GET /api/machines/{machine_id}/events/

    -   기구별 이벤트 이미지 목록 제공
    -   필터 예시

        -   event_type=start 또는 end
        -   date_from, date_to
        -   pagination

-   GET /api/events/{event_id}/

    -   이벤트 상세 조회(원본 이미지 URL, change_info, detections 등)

확장 가능 기능

-   GET /api/machines/{machine_id}/stats/

    -   기간별 사용 횟수, 피크 시간대 등 통계 제공

### 3.4 Client System 기능 및 역할

Android Native App은 서버 API를 이용해 사용 기록을 조회한다.

공통 기능

-   Image list view 제공
-   이미지 목록/획득용 RESTful API 사용

신규 추가 기능 중심 UI

-   보안키 로그인 화면
-   운동기구 목록 화면
-   기구별 이벤트 이미지 리스트 화면
-   이벤트 상세 화면
-   통계 화면(선택적 추가 페이지)

## 4. 사용자 시나리오 및 UI 구성

### 4.1 사용자 정의

-   헬스장 관리자 또는 운영자(기록 확인 및 운영 판단 목적)

### 4.2 시나리오 흐름

-   앱 실행
-   보안키로 로그인
-   운동기구 목록 확인
-   특정 기구 선택
-   사용 이벤트(시작/종료) 이미지 리스트 확인
-   특정 이벤트 선택 후 상세 확인
-   필요 시 통계 화면에서 기간별 사용 패턴 확인

### 4.3 UI 구성

로그인 화면

-   입력 요소

    -   security_key 입력 필드

-   동작

    -   로그인 버튼 클릭 시 /api/auth/login/ 호출
    -   성공 시 메인 화면으로 이동

-   저장

    -   성공한 보안키는 로컬에 저장하여 재사용

운동기구 목록 화면

-   리스트 구성(RecyclerView)

    -   기구 이름
    -   위치
    -   설명(옵션)

-   동작

    -   기구 선택 시 기구별 이벤트 목록 화면으로 이동

기구별 이벤트 이미지 리스트 화면

-   리스트 구성(RecyclerView)

    -   썸네일 이미지
    -   이벤트 타입 표시(START 또는 END)
    -   captured_at 시간
    -   간단 요약(사용 시작, 사용 종료)

-   필터(추가 기능)

    -   event_type 필터(start만, end만, 전체)
    -   날짜 범위 필터
    -   페이지네이션

이벤트 상세 화면

-   표시 요소

    -   원본 이미지
    -   이벤트 타입(START 또는 END)
    -   captured_at, created_at
    -   person_count, change_info 요약
    -   detections 요약(선택적으로 간단 표시)

통계 화면(추가 페이지)

-   기간 선택 UI
-   표시 요소

    -   일별 사용 횟수
    -   기구별 사용량 비교
    -   피크 시간대 요약

## 5. 기대효과

운영 효율 향상

-   기구별 실제 사용 데이터를 기반으로 장비 재배치, 추가 구매, 유지보수 우선순위 결정 가능

관리 비용 절감

-   직원이 직접 확인하지 않아도 자동으로 기록이 축적되어 관리 부담 감소

사용 패턴 분석 가능

-   시간대별 혼잡도를 파악하여 운영 전략 수립 가능
-   특정 기구의 활용도가 낮다면 배치 변경 등 개선 가능

서비스 확장성 확보

-   향후 알림 기능, 사용자(회원) 앱 연동, 예약 기능 등으로 확장 가능
-   통계와 로그를 기반으로 데이터 기반 서비스로 발전 가능

학습 및 프로젝트 완성도 측면

-   YOLOv5 객체 인식, Edge 기반 처리, Django REST API 설계, Android 네이티브 UI 구현까지
    전체 파이프라인을 경험하는 통합 프로젝트로 구성 가능

## 6. 요구사항 충족 확인 요약

-   Edge System

    -   Python 기반 구현
    -   YOLOv5 pretrained model 사용
    -   MS COCO 80 클래스 검출 가능
    -   person 객체 기반 Change Detection 구현
    -   RESTful API로 서버에 게시

-   Service System

    -   Django 기반, PythonAnywhere 배포
    -   보안키 기반 로그인 및 인증
    -   Image Blog 저장 및 관리
    -   게시용 RESTful API 제공
    -   이미지 목록/획득용 RESTful API 제공

-   Client System

    -   Android Native App
    -   Image list view 구현
    -   이미지 목록/획득용 REST API 사용
    -   기구 목록, 이벤트 목록, 상세, 통계 페이지 등 UI 시나리오 제공
