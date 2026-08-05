---
description: 학교 연계 API 코드값과 강의 도메인 enum의 매핑표 (PLAN-62 1단계 산출물)
---

# 학교 연계 API 코드값 매핑표

> 근거 데이터: 2026-08-04 수집. `A_MAP_COURSE_INFO` 2,538건, `A_MAP_COURSE_TIMETABLE` 9,490건
> 원본은 `.local/inu-api/` (gitignore 대상, 커밋하지 않음)

## 명세서와 다른 점

| 항목 | 명세서 | 실제 |
|---|---|---|
| `totalpageSize` | 총 페이지수 반환 | **필드 자체가 없음(null)** |
| 페이지네이션 | `totalpageSize`만큼 PAGE 증가 | **전체가 1페이지에 옴.** `totalRecordCount == pageRecordCount` |
| `PAGE=2` 호출 | 다음 페이지 | **JSON이 아닌 "site move" HTML 리다이렉트** |
| 제공 범위 | 2026년 2학기부터 | **2학기 + 여름계절학기가 함께 옴** |

페이지 반복 수집 로직은 필요 없다. PLAN-62의 5단계와 후속 배치 이슈에서 이 전제를 뺀다.

## 데이터 개요

| 항목 | 값 |
|---|---|
| 강좌 | 2,538건 (2학기 2,396 / 여름계절학기 142) |
| 시간표 | 9,490행 (2학기 6,765 / 여름계절학기 2,725) |
| 과목코드 종류 | 1,499 |
| 학수번호 종류 | 2,470 |

## 핵심 검증 결과

### 0. 학수번호 = 과목코드 + 분반번호

**2,538건 전부** `HAKSU_CODE`의 앞 7자리가 `COURSE_CODE`와 일치한다. 예외 0건이다.

```
COURSE_CODE  0000018       과목: "신소재공학실험(1)"
HAKSU_CODE   0000018001    1분반
             0000018002    2분반
             └─────┘└─┘
              과목    분반
```

| | 자릿수 | 종류 | 의미 |
|---|---|---|---|
| `COURSE_CODE` | 7 | 1,499 | 무슨 과목인가 |
| `HAKSU_CODE` | 10 | 2,470 | 어느 분반인가 |

같은 `COURSE_CODE`인데 과목명이 다른 경우는 0건이다. 과목코드 하나가 과목 하나에 정확히 대응한다.

분반 수 분포: 1분반뿐인 과목 1,088개, 2분반 299개, 3분반 37개, 최대 11분반.

**`courses` 테이블의 한 행은 학수번호 하나(= 분반 하나)다.** 학생이 신청하는 단위가 분반이고
시간표와 강의실도 분반마다 다르기 때문이다. `courseCode`는 저장하되 식별자로 쓰지 않는다.

> 기존 `courses.course_code` 컬럼에는 이미 학수번호(`0009062001` 등 10자리)가 들어 있었다.
> 이름은 과목코드인데 내용은 학수번호였던 셈이라, 이번에 이름과 내용을 맞춘다.

### 1. `(YEAR, TERM_CODE, HAKSU_CODE)` 복합키는 유일하다

중복 0건. 복합 UNIQUE 전환의 근거가 확인됐다.

### 2. 학수번호 단독 UNIQUE는 이번 적재에서 즉시 깨진다

`HAKSU_CODE`만으로는 **68건이 중복**된다 (2학기와 여름계절학기에 같은 학수번호가 존재).
"다음 학기에 문제가 된다"가 아니라 **지금 적재하면 바로 터진다.** 학기 축 도입은 선택이 아니다.

### 3. 강의실은 강의당 하나가 아니다

**220개 강의(8.7%)가 두 개 이상의 강의실을 쓴다.** 강의실을 `course_schedules`로 내리는 결정이 데이터로 확인됐다.
`ROOM_NAME`은 빈값이 0건이므로 `NOT NULL`로 잡아도 된다.

### 4. 조인되지 않는 데이터가 양쪽에 있다

| 구분 | 건수 | 성격 |
|---|---|---|
| 시간표 없는 강의 | 194건 | RISE(시간표 없음), e-Learning 등. 정상. `getFormattedCourseSchedules()`가 `-`를 반환 |
| 강좌정보 없는 시간표 | **482개 학수번호 / 1,286행** | 학수번호 체계가 다름. 강좌정보는 전부 10자리인데 이쪽은 9자리가 137개 섞여 있다. 대학원 등 다른 과정으로 추정 |

시드 적재 시 **1,286행(13.6%)을 버려야 한다.** FK를 걸 대상이 없다.

### 5. 교시(LECTM)는 연속 구간이라 현재 모델과 맞는다

31개 코드가 3계열로 나뉜다.

- `A00~A15` - 50분 단일 교시 (`0`, `1`, ..., `야1`, ..., `야6`)
- `B00~B11` - 75분 블록 (`1-2A`, `2B-3`, `5B-6`, `야1-2A` 등)
- `C01~C06` - 야간 B타임 50분 (`야1B` ~ `야6B`)

각 행이 `LECTM_START`, `LECTM_END`를 가진 연속 구간이므로 `CourseSchedule`의 `startTime`, `endTime`에 그대로 들어간다.
현재 시드의 `월(야1-2A)` 표기는 `DAY_NAME` + `LECTM_NAME`(B10) 조합과 정확히 일치한다.

강의당 시간표 행 수는 2~3행이 대부분이나, 45행짜리가 43건 있다 (실습, 현장실습 추정).

## enum 매핑

### `CourseCollege` - 19개, 완전 일치

| CODE | NAME | enum 상수 |
|---|---|---|
| `A000` | 인문대학 | `HUMANITIES` |
| `B000` | 자연과학대학 | `NATURAL_SCIENCES` |
| `C000` | 사회과학대학 | `SOCIAL_SCIENCES` |
| `E000` | 공과대학 | `ENGINEERING` |
| `I000` | 정보기술대학 | `INFORMATION_TECHNOLOGY` |
| `J000` | 경영대학 | `BUSINESS` |
| `V000` | 기타 | `ETC` |
| `W000` | 일선 | `GENERAL_ELECTIVE` |
| `X000` | 교양 | `GENERAL_EDUCATION` |
| `Y000` | 교직 | `TEACHING` |
| `Z000` | 군사학 | `MILITARY` |
| `0000033` | 도시과학대학 | `URBAN_SCIENCE` |
| `0000063` | 사범대학 | `EDUCATION` |
| `0000182` | 생명과학기술대학 | `LIFE_SCIENCES_BIOENGINEERING` |
| `0000190` | 예술체육대학 | `ARTS_PHYSICAL_EDUCATION` |
| `0000465` | 단과대구분없음 | `NONE` |
| `0000689` | 글로벌정경대학 | `COMMERCE_PUBLIC_AFFAIRS` |
| `0000706` | 단과대구분없음(법학) | `LAW` |
| `0000837` | 융합자유전공대학 | `LIBERAL_ARTS_COLLEGE` |

코드 체계가 두 가지(`A000` 형태와 `0000033` 형태)로 섞여 있으니 `String`으로 다룬다.

### `CourseDepartment` - 87개 중 85개 일치, 4개 조정 필요

| 구분 | 이름 | 조치 |
|---|---|---|
| API에만 있음 | `Global Trade & Service학부` (`0000913`) | 상수 추가 |
| API에만 있음 | `지능형로봇시스템연계전공` (`0000912`) | 상수 추가. 연계전공이므로 `isInterdisciplinary()`에도 넣어야 함 |
| enum에만 있음 | `무역학부` | 이번 학기 개설 없음. `무역학부(야)`(`0000703`)는 있다. `Global Trade & Service학부`로 개편된 것으로 보임 |
| enum에만 있음 | `국제개발협력연계전공` | 이번 학기 개설 없음 |

나머지 85개는 이름이 정확히 일치한다. 코드값 전체는 원본 JSON에서 뽑아 쓴다.

### `CourseClassification` (ISU_CODE) - 9개, 완전 일치

| CODE | NAME | enum 상수 |
|---|---|---|
| `11` | 기초교양 | `BASIC_LIBERAL_ARTS` |
| `21` | 핵심교양 | `CORE_LIBERAL_ARTS` |
| `23` | 심화교양 | `ADVANCED_LIBERAL_ARTS` |
| `25` | 전공기초 | `MAJOR_BASIC` |
| `31` | 전공핵심 | `MAJOR_CORE` |
| `41` | 전공심화 | `MAJOR_ADVANCED` |
| `50` | 교직 | `TEACHING` |
| `70` | 군사학 | `MILITARY` |
| `80` | 일반선택 | `GENERAL_ELECTIVE` |

### `CourseArea` (ISU_FLD_CODE) - 19개, 완전 일치

| CODE | NAME | enum 상수 |
|---|---|---|
| `31` | 전공기초 | `MAJOR_BASIC` |
| `34` | 전공핵심 | `MAJOR_CORE` |
| `35` | 전공심화 | `MAJOR_ADVANCED` |
| `51` | 교직 | `TEACHING` |
| `71` | 군사학 | `MILITARY` |
| `81` | 일반선택 | `GENERAL_ELECTIVE` |
| `161` | 학문의기초 | `ACADEMIC_FOUNDATION` |
| `162` | 기초과학ㆍ공학 | `BASIC_SCIENCE_ENGINEERING` |
| `171` | (핵심)INU세미나 | `CORE_INU_SEMINAR` |
| `172` | (핵심)인문 | `CORE_HUMANITIES` |
| `173` | (핵심)사회 | `CORE_SOCIAL` |
| `174` | (핵심)과학기술 | `CORE_SCIENCE_TECHNOLOGY` |
| `175` | (핵심)예술체육 | `CORE_ARTS_SPORTS` |
| `176` | (핵심)외국어 | `CORE_FOREIGN_LANGUAGE` |
| `182` | 인문 | `HUMANITIES` |
| `183` | 사회 | `SOCIAL` |
| `184` | 과학기술 | `SCIENCE_TECHNOLOGY` |
| `185` | 예술체육 | `ARTS_SPORTS` |
| `186` | 외국어 | `FOREIGN_LANGUAGE` |

> `162`의 API 이름은 `기초과학ㆍ공학`인데 현재 enum은 `기초과학·공학`이다.
> 앞은 `ㆍ`(U+318D), 뒤는 `·`(U+00B7)로 **다른 문자**다. 이름으로 매칭하면 깨지므로 반드시 코드로 매칭한다.

### `CourseType` (SUUP_TYPE_CODE) - 20개, 완전 일치

| CODE | NAME | enum 상수 |
|---|---|---|
| `1` | 강의(이론) | `LECTURE` |
| `2` | 실험실습 | `LAB` |
| `3` | 체육실기 | `PHYSICAL_EDUCATION` |
| `4` | 미술실기 | `ART_PRACTICE` |
| `5` | 이론실험실습 | `THEORY_LAB` |
| `7` | 열린사이버대학(OCU) | `OCU` |
| `8` | e-Learning | `E_LEARNING` |
| `11` | 담장너머~,사회봉사(1) | `SOCIAL_SERVICE_1` |
| `12` | 사회봉사(2) | `SOCIAL_SERVICE_2` |
| `13` | 사회봉사(3) | `SOCIAL_SERVICE_3` |
| `17` | 자기설계세미나 | `SELF_DESIGNED_SEMINAR` |
| `20` | 이론(어학) | `THEORY_LANGUAGE` |
| `21` | RISE(시간표 있음) | `RISE_WITH_SCHEDULE` |
| `22` | RISE(시간표 없음) | `RISE_WITHOUT_SCHEDULE` |
| `23` | 예술체육실기 | `ARTS_PHYSICAL_PRACTICE` |
| `24` | 온라인혼합형강좌 | `ONLINE_BLENDED` |
| `25` | K-MOOC | `K_MOOC` |
| `26` | e-Learning(HUSS) | `E_LEARNING_HUSS` |
| `27` | 온라인혼합형강좌(HUSS) | `ONLINE_BLENDED_HUSS` |
| `28` | 현장형(HUSS) | `FIELD_TYPE_HUSS` |

> 코드 `11`의 API 이름은 `담장너머~,사회봉사(1)`로 현재 enum(`사회봉사(1)`)과 다르다.
> 이름에 콤마가 들어 있어 CSV로 중간 가공하면 깨진다. 코드로 매칭한다.
>
> OCU 16건, K-MOOC 14건이 실재하므로 `CourseValidator`의 과목 유형 제한(OCU 2개, K-MOOC 1개)은 그대로 유효하다.

### `CourseGrade` (HY_CODE) - 5개, 완전 일치

| CODE | NAME | enum 상수 | 건수 |
|---|---|---|---|
| `0` | 전학년 | `ALL` | 547 |
| `1` | 1 | `FRESHMAN` | 619 |
| `2` | 2 | `SOPHOMORE` | 637 |
| `3` | 3 | `JUNIOR` | 496 |
| `4` | 4 | `SENIOR` | 239 |

> `HY_NAME`이 `1`, `2`처럼 숫자만 오고 `전학년`만 문자열이다. enum의 `name`(`1학년`)과 다르므로 코드로 매칭한다.

### `CourseDay` (DAY_CODE) - 6개, 일요일만 미사용

| CODE | NAME | enum 상수 | 건수 |
|---|---|---|---|
| `1` | 월 | `MONDAY` | 1,928 |
| `2` | 화 | `TUESDAY` | 2,139 |
| `3` | 수 | `WEDNESDAY` | 2,009 |
| `4` | 목 | `THURSDAY` | 1,934 |
| `5` | 금 | `FRIDAY` | 1,403 |
| `6` | 토 | `SATURDAY` | 77 |

`SUNDAY`는 실데이터에 없으나 상수는 남겨둔다.

### `CourseTerm` (TERM_CODE) - 신규

| CODE | NAME | 상수 | 건수 |
|---|---|---|---|
| `10` | 1학기 | `FIRST` | 0 |
| `20` | 2학기 | `SECOND` | 2,396 |
| `30` | 여름계절학기 | `SUMMER` | 142 |
| `40` | 겨울계절학기 | `WINTER` | 0 |

## 신규 필드 판단

| 필드 | 값 분포 | 판단 |
|---|---|---|
| `ENGLISH_CODE` | `0` 비대상 2,265 / `1` 원어강의(EN) 257 / `2` 글로벌강의(ES) 16 | 현재 `is_english_course` boolean은 `ENGLISH_YN`과 정확히 일치한다(`Y`=257+16=273). 다만 EN과 ES 구분이 사라진다 |
| `CNCTR_ISU_CODE` | `0` 일반 2,530 / `1` 집중A 6 / `3` 집중C 2 | 8건뿐이라 도입 실익이 낮다 |
| `HUSS_COURSE_YN` | `N` 2,498 / `Y` 40 | 40건. `CourseType`의 HUSS 4종과 겹치는지 확인 필요 |

## 컬럼 길이 산정

| 대상 | 실측 최대 | 현재 스키마 | 판단 |
|---|---|---|---|
| `COURSE_NM_KOR` | 38 | `VARCHAR(255)` | 충분 |
| `COURSE_NM_ENG` | 115 | `VARCHAR(255)` | 충분 |
| `ROOM_NAME` | 48 | 신규 | `VARCHAR(255)`면 충분 |
| `HAKSU_CODE` | 10 (시간표엔 9자리도 있음) | 신규 | `VARCHAR(15)` (명세서 기준) |
| `CREDIT` | 1~6, 12, 15 | `INT` | 충분 |

`ROOM_NAME`은 `제15호관 인문대학-503 전용어학실습실-3` 형태로 건물, 호실, 용도가 한 문자열에 붙어 온다.
`schedule` 문자열에 그대로 붙이면 길어진다.
