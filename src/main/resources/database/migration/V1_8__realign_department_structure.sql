-- 학과 체계를 실제 학사구조에 맞추면서 바뀐 enum 이름을 기존 데이터에 반영한다.
-- members와 courses는 같은 옛 이름을 서로 다른 새 이름으로 옮기므로(예: URBAN_ARCHITECTURE가
-- 회원 쪽에서는 도시건축학부, 강의 쪽에서는 도시건축학전공이다) 테이블별로 묶어 실행한다.

-- 회원: 학사구조에서 폐지된 학과를 후신 학부로 옮긴다
UPDATE members SET department = 'ELECTRONICS_ENGINEERING_SCHOOL' WHERE department = 'ELECTRONICS_ENGINEERING';
UPDATE members SET department = 'GLOBAL_TRADE_SERVICE' WHERE department = 'TRADE';

-- 회원: 학부임이 이름에 드러나도록 바꾼 상수를 반영한다
UPDATE members SET department = 'URBAN_ENVIRONMENT_ENGINEERING_SCHOOL' WHERE department = 'CIVIL_ENVIRONMENT_ENGINEERING';
UPDATE members SET department = 'URBAN_ARCHITECTURE_SCHOOL' WHERE department = 'URBAN_ARCHITECTURE';
UPDATE members SET department = 'LIFE_SCIENCE_SCHOOL' WHERE department = 'LIFE_SCIENCE';
UPDATE members SET department = 'BIOENGINEERING_SCHOOL' WHERE department = 'BIOENGINEERING';
UPDATE members SET department = 'FINE_ARTS_SCHOOL' WHERE department = 'FINE_ARTS';

-- 강의: 학부는 _SCHOOL, 전공은 _MAJOR로 층위를 드러낸 상수명을 반영한다 (20건)
UPDATE courses SET department = 'ELECTRONICS_ENGINEERING_SCHOOL' WHERE department = 'ELECTRONICS_ENGINEERING_DEPARTMENT';
UPDATE courses SET department = 'SEMICONDUCTOR_CONVERGENCE_MAJOR' WHERE department = 'SEMICONDUCTOR_CONVERGENCE';

UPDATE courses SET department = 'URBAN_ENVIRONMENT_ENGINEERING_SCHOOL' WHERE department = 'URBAN_ENVIRONMENT_ENGINEERING_DEPARTMENT';
UPDATE courses SET department = 'CIVIL_ENVIRONMENT_ENGINEERING_MAJOR' WHERE department = 'CIVIL_ENVIRONMENT_ENGINEERING';
UPDATE courses SET department = 'ENVIRONMENT_ENGINEERING_MAJOR' WHERE department = 'ENVIRONMENT_ENGINEERING';

UPDATE courses SET department = 'URBAN_ARCHITECTURE_SCHOOL' WHERE department = 'URBAN_ARCHITECTURE_DEPARTMENT';
UPDATE courses SET department = 'ARCHITECTURE_ENGINEERING_MAJOR' WHERE department = 'ARCHITECTURE_ENGINEERING';
UPDATE courses SET department = 'URBAN_ARCHITECTURE_MAJOR' WHERE department = 'URBAN_ARCHITECTURE';

UPDATE courses SET department = 'LIFE_SCIENCE_SCHOOL' WHERE department = 'LIFE_SCIENCE_DEPARTMENT';
UPDATE courses SET department = 'LIFE_SCIENCE_MAJOR' WHERE department = 'LIFE_SCIENCE';
UPDATE courses SET department = 'MOLECULAR_LIFE_SCIENCE_MAJOR' WHERE department = 'MOLECULAR_LIFE_SCIENCE';

UPDATE courses SET department = 'BIOENGINEERING_SCHOOL' WHERE department = 'BIOENGINEERING_DEPARTMENT';
UPDATE courses SET department = 'BIOENGINEERING_MAJOR' WHERE department = 'BIOENGINEERING';
UPDATE courses SET department = 'NANO_BIOENGINEERING_MAJOR' WHERE department = 'NANO_BIOENGINEERING';

UPDATE courses SET department = 'FINE_ARTS_SCHOOL' WHERE department = 'FINE_ARTS';
UPDATE courses SET department = 'KOREAN_PAINTING_MAJOR' WHERE department = 'KOREAN_PAINTING';
UPDATE courses SET department = 'WESTERN_PAINTING_MAJOR' WHERE department = 'WESTERN_PAINTING';

UPDATE courses SET department = 'NORTHEAST_ASIAN_TRADE_MAJOR' WHERE department = 'NORTHEAST_ASIAN_TRADE';
UPDATE courses SET department = 'SMART_LOGISTICS_ENGINEERING_MAJOR' WHERE department = 'SMART_LOGISTICS_ENGINEERING';
UPDATE courses SET department = 'IBE_MAJOR' WHERE department = 'IBE';
