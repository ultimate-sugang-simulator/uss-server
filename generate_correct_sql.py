#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import csv
import re
from typing import List, Tuple, Dict, Optional

# 요일 매핑
DAY_MAP = {
    '월': 'MONDAY',
    '화': 'TUESDAY',
    '수': 'WEDNESDAY',
    '목': 'THURSDAY',
    '금': 'FRIDAY',
    '토': 'SATURDAY',
    '일': 'SUNDAY'
}

# 대학 매핑
COLLEGE_MAP = {
    '인문대학': 'HUMANITIES',
    '자연과학대학': 'NATURAL_SCIENCES',
    '사회과학대학': 'SOCIAL_SCIENCES',
    '글로벌정경대학': 'COMMERCE_PUBLIC_AFFAIRS',
    '공과대학': 'ENGINEERING',
    '정보기술대학': 'INFORMATION_TECHNOLOGY',
    '경영대학': 'BUSINESS',
    '예술체육대학': 'ARTS_PHYSICAL_EDUCATION',
    '사범대학': 'EDUCATION',
    '도시과학대학': 'URBAN_SCIENCE',
    '생명과학기술대학': 'LIFE_SCIENCES_BIOENGINEERING',
    '단과대구분없음': 'NONE',
    '융합자유전공대학': 'LIBERAL_ARTS_COLLEGE',
    '단과대구분없음(법학)': 'LAW'
}

# 학과 매핑 (CSV 학과명 → CourseDepartment enum)
DEPARTMENT_MAP = {
    # 인문대학
    '국어국문학과': 'KOREAN_LITERATURE',
    '영어영문학과': 'ENGLISH_LITERATURE',
    '독어독문학과': 'GERMAN_STUDIES',
    '불어불문학과': 'FRENCH_STUDIES',
    '일본지역문화학과': 'JAPANESE_LITERATURE',
    '중어중국학과': 'CHINESE_STUDIES',

    # 자연과학대학
    '수학과': 'MATHEMATICS',
    '물리학과': 'PHYSICS',
    '화학과': 'CHEMISTRY',
    '패션산업학과': 'FASHION_INDUSTRY',
    '해양학과': 'MARINE_SCIENCE',

    # 사회과학대학
    '사회복지학과': 'SOCIAL_WELFARE',
    '미디어커뮤니케이션학과': 'MEDIA_COMMUNICATION',
    '문헌정보학과': 'LIBRARY_INFO',
    '창의인재개발학과': 'CREATIVE_HRD',

    # 글로벌정경대학
    '행정학과': 'PUBLIC_ADMINISTRATION',
    '정치외교학과': 'POLITICS_DIPLOMACY',
    '경제학과': 'ECONOMICS',
    '경제학과(야)': 'ECONOMICS_NIGHT',
    '무역학부': 'TRADE',
    '무역학부(야)': 'TRADE_NIGHT',
    '소비자학과': 'CONSUMER_SCIENCE',

    # 공과대학
    '기계공학과': 'MECHANICAL_ENGINEERING',
    '전기공학과': 'ELECTRICAL_ENGINEERING',
    '전자공학과': 'ELECTRONICS_ENGINEERING',
    '전자공학부': 'ELECTRONICS_ENGINEERING_DEPARTMENT',
    '전자공학전공': 'ELECTRONICS_ENGINEERING_MAJOR',
    '반도체융합전공': 'SEMICONDUCTOR_CONVERGENCE',
    '산업경영공학과': 'INDUSTRIAL_MANAGEMENT',
    '신소재공학과': 'MATERIAL_SCIENCE',
    '안전공학과': 'SAFETY_ENGINEERING',
    '에너지화학공학과': 'ENERGY_CHEMICAL',
    '바이오-로봇시스템공학과': 'BIO_ROBOTICS_ENGINEERING',

    # 정보기술대학
    '컴퓨터공학부': 'COMPUTER_ENGINEERING',
    '정보통신공학과': 'INFORMATION_COMMUNICATION_ENGINEERING',
    '임베디드시스템공학과': 'EMBEDDED_SYSTEM',

    # 경영대학
    '경영학부': 'BUSINESS_ADMINISTRATION',
    '데이터과학과': 'DATA_SCIENCE',
    '세무회계학과': 'TAX_ACCOUNTING',

    # 예술체육대학
    '조형예술학부': 'FINE_ARTS',
    '한국화전공': 'KOREAN_PAINTING',
    '서양화전공': 'WESTERN_PAINTING',
    '디자인학부': 'DESIGN',
    '공연예술학과': 'PERFORMING_ART',
    '스포츠과학부': 'SPORTS_SCIENCE',
    '운동건강학부': 'HEALTH_EXERCISE',

    # 사범대학
    '국어교육과': 'KOREAN_EDUCATION',
    '영어교육과': 'ENGLISH_EDUCATION',
    '일어교육과': 'JAPANESE_EDUCATION',
    '수학교육과': 'MATH_EDUCATION',
    '체육교육과': 'PHYSICAL_EDUCATION',
    '유아교육과': 'EARLY_CHILDHOOD_EDUCATION',
    '역사교육과': 'HISTORY_EDUCATION',
    '윤리교육과': 'ETHICS_EDUCATION',

    # 도시과학대학
    '도시행정학과': 'URBAN_ADMINISTRATION',
    '도시환경공학부': 'URBAN_ENVIRONMENT_ENGINEERING_DEPARTMENT',
    '건설환경공학전공': 'CIVIL_ENVIRONMENT_ENGINEERING',
    '환경공학전공': 'ENVIRONMENT_ENGINEERING',
    '도시공학과': 'URBAN_ENGINEERING',
    '도시건축학부': 'URBAN_ARCHITECTURE_DEPARTMENT',
    '건축공학전공': 'ARCHITECTURE_ENGINEERING',
    '도시건축학전공': 'URBAN_ARCHITECTURE',

    # 생명과학기술대학
    '생명과학부': 'LIFE_SCIENCE_DEPARTMENT',
    '생명과학전공': 'LIFE_SCIENCE',
    '분자의생명전공': 'MOLECULAR_LIFE_SCIENCE',
    '생명공학부': 'BIOENGINEERING_DEPARTMENT',
    '생명공학전공': 'BIOENGINEERING',
    '나노바이오공학전공': 'NANO_BIOENGINEERING',

    # 기타
    '동북아국제통상전공': 'NORTHEAST_ASIAN_TRADE',
    '스마트물류공학전공': 'SMART_LOGISTICS_ENGINEERING',
    'IBE전공': 'IBE',
    '자유전공학부': 'LIBERAL_ARTS',
    '법학부': 'LAW',
    '교양': 'GENERAL_EDUCATION',
    '교직': 'TEACHING',
    '일선': 'GENERAL_ELECTIVE',
    '군사학': 'MILITARY',
    '광전자공학전공(연계)': 'OPTOELECTRONICS',
    '물류학전공(연계)': 'LOGISTICS',
    '국제개발협력연계전공': 'INTERNATIONAL_DEVELOPMENT_COOPERATION',
    '창의적디자인연계전공': 'CREATIVE_DESIGN',
    '인문문화예술기획연계전공': 'HUMANITIES_CULTURE_ART_PLANNING',
    '소셜데이터사이언스연계전공': 'SOCIAL_DATA_SCIENCE',
    '미래자동차연계전공': 'FUTURE_AUTOMOBILE',
    '미래교육디자인연계전공': 'FUTURE_EDUCATION_DESIGN',
    'HUSS포용사회이니셔티브학부': 'HUSS_INCLUSIVE_SOCIETY_INITIATIVE',
    'HUSS(타대학)': 'HUSS_OTHER_UNIVERSITY'
}

# 학년 매핑
GRADE_MAP = {
    '1': 'FRESHMAN',
    '2': 'SOPHOMORE',
    '3': 'JUNIOR',
    '4': 'SENIOR',
    '전학년': 'ALL'
}

# 이수구분 매핑
CLASSIFICATION_MAP = {
    '교양필수': 'GENERAL_REQUIRED',
    '교양선택': 'GENERAL_ELECTIVE',
    '전공기초': 'MAJOR_BASIC',
    '전공필수': 'MAJOR_REQUIRED',
    '전공선택': 'MAJOR_ELECTIVE',
    '전공핵심': 'MAJOR_CORE',
    '전공심화': 'MAJOR_ADVANCED',
    '기초교양': 'BASIC_LIBERAL_ARTS',
    '핵심교양': 'CORE_LIBERAL_ARTS',
    '심화교양': 'ADVANCED_LIBERAL_ARTS',
    '교직': 'TEACHING',
    '일반선택': 'GENERAL_ELECTIVE',
    '군사학': 'MILITARY'
}

# 수업유형 매핑
COURSE_TYPE_MAP = {
    '강의(이론)': 'LECTURE',
    '이론(어학)': 'THEORY_LANGUAGE',
    '실험실습': 'LAB',
    '이론실험실습': 'THEORY_LAB',
    '체육실기': 'PHYSICAL_EDUCATION',
    '미술실기': 'ART_PRACTICE',
    '예술체육실기': 'ARTS_PHYSICAL_PRACTICE',
    'e-Learning': 'E_LEARNING',
    '온라인혼합형강좌': 'ONLINE_BLENDED',
    'K-MOOC': 'K_MOOC',
    '열린사이버대학(OCU)': 'OCU',
    '사회봉사(1)': 'SOCIAL_SERVICE_1',
    '사회봉사(2)': 'SOCIAL_SERVICE_2',
    '사회봉사(3)': 'SOCIAL_SERVICE_3',
    'RISE(시간표 있음)': 'RISE_WITH_SCHEDULE',
    'RISE(시간표 없음)': 'RISE_WITHOUT_SCHEDULE',
    '자기설계세미나': 'SELF_DESIGNED_SEMINAR',
    '현장실습': 'FIELD_TRAINING'
}

# 교양 course_area 매핑 (course_code -> CourseArea)
LIBERAL_ARTS_AREA_MAP = {
    # CORE_INU_SEMINAR
    **{code: 'CORE_INU_SEMINAR' for code in [
        '0004322001', '0004322002', '0004339001', '0010154001', '0010154002', '0011229001',
        '0011230001', '0011231001', '0011232001', '0011244001', '0011631001', '0011845001'
    ]},

    # CORE_HUMANITIES
    **{code: 'CORE_HUMANITIES' for code in [
        '0002644001', '0002644002', '0004323001', '0004345001', '0004345002', '0004345003',
        '0005068001', '0005068002', '0007030001', '0007399001', '0007399002', '0007399003',
        '0007399004', '0012550001', '0012554001', '0012554002', 'XAA1352001', 'XAA1352002',
        'XAA1352003', 'XAA1352004', 'XAA1352005', 'XAA1490001', 'XAA1490002', 'XAA1490003',
        'XAA2032001', 'XAA2032002'
    ]},

    # CORE_SOCIAL
    **{code: 'CORE_SOCIAL' for code in [
        '0002650001', '0005063001', '0005063002', '0005077001', '0005077002', '0006403001',
        '0006433001', '0006433002', '0006433003', '0010641001', '0010641002', '0010641003',
        '0010641004', '0010641005', '0010641006', '0011241001', '0011241002', '0011851001',
        '0012551001', '0012551002', 'XAA8063001'
    ]},

    # CORE_SCIENCE_TECHNOLOGY
    **{code: 'CORE_SCIENCE_TECHNOLOGY' for code in [
        '0005069001', '0007357001', '0007369001', '0007370001', '0009338001', '0009338002',
        '0009338003', 'XAA1360001', 'XAA1360002'
    ]},

    # CORE_ARTS_SPORTS
    **{code: 'CORE_ARTS_SPORTS' for code in [
        '0003601001', '0010541001', '0011861001', '0011863001', 'XAA1386001', 'XAA1386002'
    ]},

    # CORE_FOREIGN_LANGUAGE
    **{code: 'CORE_FOREIGN_LANGUAGE' for code in [
        '0002666001', '0002666002', '0002666003', '0002666004', '0002667001', '0002667002',
        '0002667003', '0005414001', '0005414002', '0011640001', '0011640002', '0011640003',
        '0011640004', '0011644001', '0011644002', '0011645001', '0011645002', '0011645003',
        '0011645004'
    ]},

    # HUMANITIES (심화)
    **{code: 'HUMANITIES' for code in [
        '0004338001', '0004338002', '0005067001', '0005413001', '0005413002', '0006111001',
        '0006404001', '0006992001', '0006992002', '0006998001', '0007008001', '0007400001',
        '0007910001', '0008297001', '0009083001', '0009090001', '0009091001', '0009092001',
        '0009336001', '0009336002', '0009337001', '0009780001', '0010146001', '0010354001',
        '0010354002', '0010356001', '0010356002', '0011235001', '0011235002', '0011236001',
        '0011238001', '0011239001', '0011619001', '0011621001', '0011622001', '0011632001',
        '0011649001', '0011852001', '0011864001', '0012516001', '0012531001', '0012531002',
        '0012539001', '0012539002', '0012546001', '0012553001', '0012553002', '0012555001',
        'O810241001', 'O810410001', 'XAA1430001', 'XAA8044001'
    ]},

    # SOCIAL (심화)
    **{code: 'SOCIAL' for code in [
        '0002649001', '0003604001', '0004330001', '0004342001', '0004342002', '0004344001',
        '0005070001', '0005073001', '0005093001', '0005093002', '0005103001', '0006989001',
        '0007001001', '0007001002', '0007002001', '0007366001', '0008300001', '0008590001',
        '0008628001', '0009076001', '0009329001', '0009334001', '0010355001', '0010355002',
        '0010355003', '0010635001', '0010640001', '0011618001', '0011620001', '0011625001',
        '0011626001', '0011633001', '0011635001', '0011643001', '0011652001', '0011847001',
        '0011858001', '0011865001', '0011866001', '0012519001', '0012521001', '0012540001',
        '0012540002', '0012547001', '0012556001', '0012563001', 'O810301001', 'O810554001',
        'XAA1133001', 'XAA1133002', 'XAA8043001', 'XAA8043002', 'XAA8057001'
    ]},

    # SCIENCE_TECHNOLOGY (심화)
    **{code: 'SCIENCE_TECHNOLOGY' for code in [
        '0002646001', '0002646002', '0002647001', '0004327001', '0004327002', '0006109001',
        '0006987001', '0007360001', '0010352001', '0010352002', '0010353001', '0010353002',
        '0010536001', '0011617001', '0011623001', '0011624001', '0011653001', '0011842001',
        '0011862001', '0012522001', '0012523001', '0012528001', '0012532001', '0012549001',
        '0012557001', '0012557002', '0012559001', 'O810280001', 'O810558001', 'O810709001',
        'O810710001', 'O820057001', 'XAA2047001'
    ]},

    # ARTS_SPORTS (심화)
    **{code: 'ARTS_SPORTS' for code in [
        '0003597001', '0003597002', '0003597003', '0003598001', '0003598002', '0003598003',
        '0005417001', '0008606001', '0008607001', '0009089001', '0009775001', '0009775002',
        '0009775003', '0009775004', '0009775005', '0009775006', '0009776001', '0009776002',
        '0009777001', '0009777002', '0009777003', '0009777004', '0009777005', '0009777006',
        '0009777007', '0009778001', '0009778002', '0010137001', '0010357001', '0010357002',
        '0011243001', '0011629001', '0011854001', '0011855001', '0011856001', '0011856002',
        '0011857001', '0011860001', '0011860002', '0012525001', '0012533001', '0012534001',
        '0012535001', '0012536001', '0012542001', '0012582001', '0012582002', 'O810474001',
        'O810574001', 'XAA1391001', 'XAA1470001', 'XAA1470002', 'XAA1486001'
    ]},

    # FOREIGN_LANGUAGE (심화)
    **{code: 'FOREIGN_LANGUAGE' for code in [
        '0007363001', '0008620001', '0008621001', '0008622001', '0008623001', '0008624001',
        '0008625001', '0009081001', '0009340001', '0009341001', '0010143001', '0011242001',
        '0011637001', '0011637002', '0011639001', '0011639002', '0011642001', '0011850001',
        '0012529001', '0012537001', '0012538001'
    ]},

    # BASIC_SCIENCE_ENGINEERING
    **{code: 'BASIC_SCIENCE_ENGINEERING' for code in [
        'XAA1077003', 'XAA1077004', 'XAA1077005', 'XAA1077006', 'XAA1080001', 'XAA1080002',
        'XAA1080003', 'XAA1080004', 'XAA1080005', 'XAA1080006', 'XAA1080007', 'XAA1493001',
        'XAA1493002', 'XAA1493003', 'XAA1077007', 'XAA1077008', 'XAA1077009', 'XAA1114001',
        'XAA1114002', 'XAA1114003', 'EPC6059001', 'EPC6059002', 'EPC6059003', '0002399001',
        '0002402001', '0002396001', 'XAA1077001', '0011616001', '0006940001', '0006940002',
        '0011611001', '0011611002', 'XAA1114005', 'XAA1114006', '0011613001', 'XAA1114007',
        'XAA1077002', 'EPD6103001', 'XAA1078001', '0006754001', '0006754002'
    ]},

    # ACADEMIC_FOUNDATION
    **{code: 'ACADEMIC_FOUNDATION' for code in
        # 0009316001-042
        [f'0009316{str(i).zfill(3)}' for i in range(1, 43)] +
        # 0012560001-045
        [f'0012560{str(i).zfill(3)}' for i in range(1, 46)] +
        # 0012561001-028
        [f'0012561{str(i).zfill(3)}' for i in range(1, 29)] +
        # 0005060001-108
        [f'0005060{str(i).zfill(3)}' for i in range(1, 109)] +
        # XAA1358001-038 (대학수학(1))
        [f'XAA1358{str(i).zfill(3)}' for i in range(1, 39)]
    }
}


def extract_classroom_codes(classroom_text: str) -> str:
    """강의실 텍스트에서 강의실 코드들을 추출"""
    if not classroom_text or classroom_text.strip() == '':
        return ''

    # [15-119] 형태의 코드 추출
    codes = re.findall(r'\[([0-9]+-[0-9A-Z]+)\]', classroom_text)
    return ', '.join(codes) if codes else ''


def parse_schedule_slots(time_slot_text: str, time_text: str) -> List[Dict]:
    """시간표 정보를 파싱하여 요일별 스케줄 정보 반환

    핵심: schedule_text에 강의실 정보를 포함하지 않음!
    """
    schedules = []

    if not time_slot_text or time_slot_text.strip() == '':
        return schedules

    # 공백 또는 쉼표로 구분된 여러 블록 처리
    # 예: [15-119:화(5B-6),수(5B-6)] 또는 [15-118B:화(7-8A)] [15-403:목(7-8A)]
    blocks = re.findall(r'\[([^\]]+)\]', time_slot_text)

    # 시간 정보 파싱 (동일한 방식)
    time_blocks = re.findall(r'\[([^\]]+)\]', time_text) if time_text else []

    # 요일별 시간 매핑
    time_map = {}
    for block in time_blocks:
        if ':' in block:
            _, time_part = block.split(':', 1)
        else:
            time_part = block

        # 쉼표로 구분된 요일별 시간
        day_times = re.split(r',\s*', time_part)

        for day_time in day_times:
            match = re.match(r'([월화수목금토일])\(([0-9:~]+)\)', day_time.strip())
            if match:
                day_kr = match.group(1)
                times = match.group(2)
                if '~' in times:
                    start, end = times.split('~')
                    time_map[day_kr] = (start, end)

    # 요일별 교시 추출
    for block in blocks:
        # 강의실:요일(교시) 형태에서 요일(교시) 부분만 추출
        if ':' in block:
            _, schedule_part = block.split(':', 1)
        else:
            schedule_part = block

        # 쉼표로 구분된 요일별 교시
        day_slots = re.split(r',\s*', schedule_part)

        for day_slot in day_slots:
            match = re.match(r'([월화수목금토일])\(([^)]+)\)', day_slot.strip())
            if match:
                day_kr = match.group(1)
                time_info = match.group(2)

                # schedule_text는 요일과 교시만 (강의실 정보 제외!)
                schedule_text = f"{day_kr}({time_info})"

                schedule = {
                    'day_kr': day_kr,
                    'schedule_text': schedule_text,
                    'time_info': time_info
                }

                # 시간 정보 추가
                if day_kr in time_map:
                    schedule['start_time'] = time_map[day_kr][0]
                    schedule['end_time'] = time_map[day_kr][1]

                schedules.append(schedule)

    return schedules


def escape_sql_string(s: str) -> str:
    """SQL 문자열 이스케이프"""
    if s is None:
        return ''
    return s.replace("'", "''")


def generate_course_insert(row: Dict) -> Optional[str]:
    """Course INSERT 문 생성"""
    try:
        college = COLLEGE_MAP.get(row['대학(원)'], 'NONE')

        # 학과 매핑
        dept_name = row['학과(부)'].strip()
        department = DEPARTMENT_MAP.get(dept_name)
        if not department:
            print(f"[WARNING] Unknown department: {dept_name} for course {row['학수번호']}")
            return None

        classification = CLASSIFICATION_MAP.get(row['이수구분'])
        if not classification:
            print(f"[WARNING] Unknown classification: {row['이수구분']} for course {row['학수번호']}")
            return None

        # course_area 결정
        course_code = row['학수번호']
        if classification in ['BASIC_LIBERAL_ARTS', 'CORE_LIBERAL_ARTS', 'ADVANCED_LIBERAL_ARTS']:
            # 교양 과목: course_code로 매핑
            course_area = LIBERAL_ARTS_AREA_MAP.get(course_code)
            if not course_area:
                print(f"[WARNING] Course code {course_code} not found in liberal arts area mapping")
                return None
        elif classification in ['TEACHING', 'GENERAL_ELECTIVE', 'MILITARY']:
            # 교직, 일반선택, 군사학: classification과 동일
            course_area = classification
        else:
            # 전공 과목: classification과 동일
            course_area = classification

        course_type = COURSE_TYPE_MAP.get(row['수업유형'], 'LECTURE')
        grade = GRADE_MAP.get(row['학년'], 'ALL')

        # 강의실 코드 추출
        classroom = extract_classroom_codes(row['강의실'])

        # 원어강의 판단
        is_english = row['원어강의구분'] and '원어강의' in row['원어강의구분']

        # 교수명 처리
        professor = row['담당교수'].strip() if row['담당교수'].strip() else 'NULL'
        if professor != 'NULL':
            professor = f"'{escape_sql_string(professor)}'"

        # classroom 처리
        if classroom:
            classroom = f"'{classroom}'"
        else:
            classroom = 'NULL'

        # SQL VALUES 부분 생성
        values = f"""('{escape_sql_string(row['교과목명'])}', '{escape_sql_string(row['교과목명(영문)'])}', '{course_code}', '{college}', '{department}', '{classification}', '{course_area}', '{course_type}', '{grade}', {professor}, {classroom}, {row['학점']}, {str(is_english).lower()}, 100, 0)"""

        return values
    except Exception as e:
        print(f"[ERROR] Failed to generate course insert for {row.get('학수번호', 'unknown')}: {e}")
        return None


def generate_schedule_inserts(course_code: str, row: Dict) -> List[str]:
    """Schedule INSERT 문들 생성"""
    inserts = []

    try:
        schedule_slots = parse_schedule_slots(row['시간표(교시)'], row['시간표(시간)'])

        for slot in schedule_slots:
            if 'start_time' not in slot or 'end_time' not in slot:
                continue

            day_kr = slot['day_kr']
            day_en = DAY_MAP.get(day_kr, 'MONDAY')
            schedule_text = slot['schedule_text']  # 이미 강의실 정보 제거됨

            # 시간 포맷팅
            start_time = slot['start_time']
            end_time = slot['end_time']

            if start_time.count(':') == 1:
                start_time += ':00'
            if end_time.count(':') == 1:
                end_time += ':00'

            insert = f"((SELECT id FROM courses WHERE course_code = '{course_code}'), '{schedule_text}', '{day_en}', '{start_time}', '{end_time}')"
            inserts.append(insert)
    except Exception as e:
        print(f"[ERROR] Failed to generate schedule inserts for {course_code}: {e}")

    return inserts


def main():
    csv_file = '/Users/xunsseoie/Desktop/melong/project/ultimate-sugang-simulator/uss-server/src/main/resources/sql/course.csv'
    valid_codes_file = '/Users/xunsseoie/Desktop/melong/project/ultimate-sugang-simulator/uss-server/valid_course_codes.txt'

    # 유효한 학수번호 목록 로드
    with open(valid_codes_file, 'r', encoding='utf-8') as f:
        valid_course_codes = set(line.strip() for line in f if line.strip())

    print(f'Loaded {len(valid_course_codes)} valid course codes')

    course_inserts = []
    schedule_inserts = []

    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)

        for row in reader:
            try:
                # 유효한 학수번호만 처리
                course_code = row.get('학수번호', '').strip()
                if not course_code or course_code not in valid_course_codes:
                    continue

                # Course INSERT 생성
                course_insert = generate_course_insert(row)
                if course_insert:
                    course_inserts.append(course_insert)

                    # Schedule INSERTs 생성
                    schedule_insert_list = generate_schedule_inserts(row['학수번호'], row)
                    schedule_inserts.extend(schedule_insert_list)
            except Exception as e:
                print(f"[ERROR] Processing row {row.get('순번', 'unknown')}: {e}")
                continue

    # Course INSERT SQL 파일 생성 (배치 분할)
    batch_size = 1000
    total_batches = (len(course_inserts) + batch_size - 1) // batch_size

    for batch_idx in range(total_batches):
        start_idx = batch_idx * batch_size
        end_idx = min((batch_idx + 1) * batch_size, len(course_inserts))
        batch_inserts = course_inserts[start_idx:end_idx]

        file_num = batch_idx + 1
        output_file = f'/Users/xunsseoie/Desktop/melong/project/ultimate-sugang-simulator/uss-server/src/main/resources/sql/course_{str(file_num).zfill(2)}.sql'

        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(f'-- Generated Course INSERT statements (Batch {file_num})\n')
            f.write(f'-- Total courses in this batch: {len(batch_inserts)}\n\n')
            f.write('INSERT INTO courses (title_kr, title_en, course_code, course_college, course_department, course_classification,\n')
            f.write('                     course_area, course_type, course_grade, professor_name, classroom, credits, is_english_course,\n')
            f.write('                     max_capacity, current_enrollment)\n')
            f.write('VALUES\n')
            f.write(',\n'.join(batch_inserts))
            f.write(';\n')

        print(f'Generated course_{str(file_num).zfill(2)}.sql with {len(batch_inserts)} courses')

    # Schedule INSERT SQL 파일 생성 (배치 분할)
    schedule_batch_size = 1500
    total_schedule_batches = (len(schedule_inserts) + schedule_batch_size - 1) // schedule_batch_size

    for batch_idx in range(total_schedule_batches):
        start_idx = batch_idx * schedule_batch_size
        end_idx = min((batch_idx + 1) * schedule_batch_size, len(schedule_inserts))
        batch_inserts = schedule_inserts[start_idx:end_idx]

        file_num = batch_idx + 1
        output_file = f'/Users/xunsseoie/Desktop/melong/project/ultimate-sugang-simulator/uss-server/src/main/resources/sql/course_schedule_{str(file_num).zfill(2)}.sql'

        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(f'-- Generated Schedule INSERT statements (Batch {file_num})\n')
            f.write(f'-- Total schedules in this batch: {len(batch_inserts)}\n\n')
            f.write('INSERT INTO course_schedules (course_id, schedule_text, course_day, start_time, end_time)\n')
            f.write('VALUES\n')
            f.write(',\n'.join(batch_inserts))
            f.write(';\n')

        print(f'Generated course_schedule_{str(file_num).zfill(2)}.sql with {len(batch_inserts)} schedules')

    print(f'\n=== Summary ===')
    print(f'Total courses: {len(course_inserts)}')
    print(f'Total schedules: {len(schedule_inserts)}')
    print(f'Course batches: {total_batches}')
    print(f'Schedule batches: {total_schedule_batches}')


if __name__ == '__main__':
    main()
