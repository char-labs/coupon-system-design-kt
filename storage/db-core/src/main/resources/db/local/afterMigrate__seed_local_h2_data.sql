-- Local H2 sample data
-- Admin account: minjun.kim@naver.com / coupon1234!
-- All seeded users share the same password: coupon1234!

DELETE FROM t_authentication_history;
DELETE FROM t_coupon_issue;
DELETE FROM t_coupon;
DELETE FROM t_user;

INSERT INTO t_user (
    id,
    user_key,
    name,
    email,
    password,
    role,
    created_at,
    updated_at,
    deleted_at
)
WITH surnames (seq, family_name_ko, family_name_en) AS (
    VALUES
        (1, '김', 'kim'),
        (2, '이', 'lee'),
        (3, '박', 'park'),
        (4, '최', 'choi'),
        (5, '정', 'jung'),
        (6, '강', 'kang'),
        (7, '조', 'jo'),
        (8, '윤', 'yoon'),
        (9, '장', 'jang'),
        (10, '임', 'lim'),
        (11, '한', 'han'),
        (12, '오', 'oh')
),
given_names (seq, given_name_ko, given_name_en) AS (
    VALUES
        (1, '민준', 'minjun'),
        (2, '서연', 'seoyeon'),
        (3, '지훈', 'jihoon'),
        (4, '하은', 'haeun'),
        (5, '도윤', 'doyun'),
        (6, '수빈', 'subin'),
        (7, '현우', 'hyunwoo'),
        (8, '지민', 'jimin'),
        (9, '예린', 'yerin'),
        (10, '태현', 'taehyun')
),
user_source AS (
    SELECT
        ROW_NUMBER() OVER (ORDER BY surnames.seq, given_names.seq) AS row_num,
        surnames.family_name_ko,
        surnames.family_name_en,
        given_names.given_name_ko,
        given_names.given_name_en
    FROM surnames
    CROSS JOIN given_names
)
SELECT
    1000 + row_num,
    '20260326_UK_LOCAL_' || LPAD(CAST(row_num AS VARCHAR), 4, '0'),
    family_name_ko || given_name_ko,
    given_name_en || '.' || family_name_en || '@' ||
        CASE MOD(row_num - 1, 5)
            WHEN 0 THEN 'naver.com'
            WHEN 1 THEN 'gmail.com'
            WHEN 2 THEN 'kakao.com'
            WHEN 3 THEN 'daum.net'
            ELSE 'hanmail.net'
        END,
    '{noop}coupon1234!',
    CASE WHEN row_num = 1 THEN 'ADMIN' ELSE 'USER' END,
    DATEADD('DAY', -(121 - row_num), CURRENT_TIMESTAMP),
    DATEADD('DAY', -(121 - row_num), CURRENT_TIMESTAMP),
    NULL
FROM user_source;

INSERT INTO t_coupon (
    id,
    coupon_code,
    name,
    coupon_type,
    status,
    discount_amount,
    max_discount_amount,
    min_order_amount,
    total_quantity,
    remaining_quantity,
    available_at,
    end_at,
    created_at,
    updated_at,
    deleted_at
)
WITH coupon_source (
    seq,
    coupon_code,
    name,
    coupon_type,
    status,
    discount_amount,
    max_discount_amount,
    min_order_amount,
    total_quantity,
    available_day_offset,
    end_day_offset,
    created_day_offset
) AS (
    VALUES
        (1, '20260326_CP_A001', '강남 직장인 점심 3천원 할인', 'FIXED', 'ACTIVE', 3000, NULL, 12000, 180, -14, 30, -20),
        (2, '20260326_CP_A002', '퇴근길 카페 2천원 할인', 'FIXED', 'ACTIVE', 2000, NULL, 7000, 160, -10, 21, -15),
        (3, '20260326_CP_A003', '주말 마트 장보기 10% 할인', 'PERCENTAGE', 'ACTIVE', 10, 8000, 30000, 220, -12, 25, -18),
        (4, '20260326_CP_A004', '배달 저녁 4천원 할인', 'FIXED', 'ACTIVE', 4000, NULL, 18000, 140, -8, 18, -12),
        (5, '20260326_CP_A005', '올리브영 픽업 15% 할인', 'PERCENTAGE', 'ACTIVE', 15, 7000, 20000, 150, -6, 27, -10),
        (6, '20260326_CP_A006', '동네 빵집 오전 2천원 할인', 'FIXED', 'ACTIVE', 2000, NULL, 10000, 90, -5, 14, -8),
        (7, '20260326_CP_A007', '헬스장 보충제 12% 할인', 'PERCENTAGE', 'ACTIVE', 12, 9000, 35000, 110, -9, 32, -16),
        (8, '20260326_CP_A008', '금요일 영화 예매 5천원 할인', 'FIXED', 'ACTIVE', 5000, NULL, 15000, 80, -7, 20, -11),
        (9, '20260326_CP_A009', '봄 셔츠 장만 10% 할인', 'PERCENTAGE', 'ACTIVE', 10, 12000, 40000, 130, -4, 24, -9),
        (10, '20260326_CP_A010', '반려동물 사료 6천원 할인', 'FIXED', 'ACTIVE', 6000, NULL, 45000, 100, -11, 19, -13),
        (11, '20260326_CP_A011', '주유소 앱 결제 4천원 할인', 'FIXED', 'ACTIVE', 4000, NULL, 50000, 120, -3, 16, -7),
        (12, '20260326_CP_A012', '서점 주말 3천원 할인', 'FIXED', 'ACTIVE', 3000, NULL, 18000, 95, -13, 28, -19),
        (13, '20260326_CP_A013', '아이 간식 꾸러미 8% 할인', 'PERCENTAGE', 'ACTIVE', 8, 6000, 25000, 105, -6, 17, -10),
        (14, '20260326_CP_A014', '야식 치킨 5천원 할인', 'FIXED', 'ACTIVE', 5000, NULL, 23000, 125, -8, 23, -12),
        (15, '20260326_CP_A015', '집들이 생필품 12% 할인', 'PERCENTAGE', 'ACTIVE', 12, 10000, 38000, 115, -9, 29, -14),
        (16, '20260326_CP_A016', '아침 샌드위치 1500원 할인', 'FIXED', 'ACTIVE', 1500, NULL, 6500, 90, -2, 12, -5),
        (17, '20260326_CP_A017', '심야 택시 귀가 3천원 할인', 'FIXED', 'ACTIVE', 3000, NULL, 16000, 85, -5, 15, -8),
        (18, '20260326_CP_A018', '동네 세탁소 2천원 할인', 'FIXED', 'ACTIVE', 2000, NULL, 12000, 70, -7, 26, -11),
        (19, '20260326_CP_A019', '봄나들이 도시락 10% 할인', 'PERCENTAGE', 'ACTIVE', 10, 7000, 22000, 100, -4, 18, -9),
        (20, '20260326_CP_A020', '주방용품 묶음 7천원 할인', 'FIXED', 'ACTIVE', 7000, NULL, 50000, 75, -12, 35, -17),
        (21, '20260326_CP_I001', '벚꽃 주말 카페 20% 할인', 'PERCENTAGE', 'INACTIVE', 12, 9300, 26500, 125, 4, 36, -1),
        (22, '20260326_CP_I002', '어린이날 장난감 1만원 할인', 'FIXED', 'INACTIVE', 4000, NULL, 16000, 98, 7, 42, -2),
        (23, '20260326_CP_I003', '초여름 샌들 12% 할인', 'FIXED', 'INACTIVE', 4500, NULL, 18000, 102, 8, 43, -3),
        (24, '20260326_CP_I004', '장마철 우산 3천원 할인', 'PERCENTAGE', 'INACTIVE', 10, 10200, 31000, 140, 7, 39, -4),
        (25, '20260326_CP_I005', '여름휴가 숙소 8% 할인', 'FIXED', 'INACTIVE', 5500, NULL, 22000, 110, 10, 45, -5),
        (26, '20260326_CP_I006', '캠핑용품 15% 할인', 'FIXED', 'INACTIVE', 6000, NULL, 24000, 114, 11, 46, -6),
        (27, '20260326_CP_I007', '이사철 가전 2만원 할인', 'PERCENTAGE', 'INACTIVE', 16, 11100, 35500, 155, 10, 42, -7),
        (28, '20260326_CP_I008', '추석 선물세트 10% 할인', 'FIXED', 'INACTIVE', 7000, NULL, 28000, 122, 13, 48, -8),
        (29, '20260326_CP_I009', '개강 문구 4천원 할인', 'FIXED', 'INACTIVE', 7500, NULL, 30000, 126, 14, 49, -9),
        (30, '20260326_CP_I010', '명절 기차표 예매 5천원 할인', 'PERCENTAGE', 'INACTIVE', 14, 12000, 40000, 170, 13, 45, -10),
        (31, '20260326_CP_I011', '수험생 간식 꾸러미 12% 할인', 'FIXED', 'INACTIVE', 8500, NULL, 34000, 134, 16, 51, -11),
        (32, '20260326_CP_I012', '겨울 코트 15% 할인', 'FIXED', 'INACTIVE', 9000, NULL, 36000, 138, 17, 52, -12),
        (33, '20260326_CP_I013', '연말 케이크 예약 6천원 할인', 'PERCENTAGE', 'INACTIVE', 12, 12900, 44500, 185, 16, 48, -13),
        (34, '20260326_CP_I014', '신학기 가방 10% 할인', 'FIXED', 'INACTIVE', 10000, NULL, 40000, 146, 19, 54, -14),
        (35, '20260326_CP_I015', '봄맞이 침구 8% 할인', 'FIXED', 'INACTIVE', 10500, NULL, 42000, 150, 20, 55, -15),
        (36, '20260326_CP_E001', '설 연휴 장보기 8천원 할인', 'FIXED', 'EXPIRED', 3200, NULL, 11700, 83, -59, -6, -79),
        (37, '20260326_CP_E002', '화이트데이 디저트 15% 할인', 'PERCENTAGE', 'EXPIRED', 12, 7800, 20600, 108, -68, -12, -88),
        (38, '20260326_CP_E003', '입학 준비 문구 20% 할인', 'FIXED', 'EXPIRED', 4600, NULL, 15100, 89, -57, -8, -77),
        (39, '20260326_CP_E004', '새해 운동 시작 1만원 할인', 'PERCENTAGE', 'EXPIRED', 16, 8600, 23200, 116, -66, -14, -86),
        (40, '20260326_CP_E005', '주말 드라이브 주유 5천원 할인', 'FIXED', 'EXPIRED', 6000, NULL, 18500, 95, -55, -10, -75),
        (41, '20260326_CP_E006', '졸업식 꽃다발 10% 할인', 'PERCENTAGE', 'EXPIRED', 10, 9400, 25800, 124, -64, -16, -84),
        (42, '20260326_CP_E007', '한파 대비 난방용품 12% 할인', 'FIXED', 'EXPIRED', 7400, NULL, 21900, 101, -53, -12, -73),
        (43, '20260326_CP_E008', '발렌타인데이 초콜릿 4천원 할인', 'PERCENTAGE', 'EXPIRED', 14, 10200, 28400, 132, -62, -18, -82),
        (44, '20260326_CP_E009', '겨울 이불 정리 7천원 할인', 'FIXED', 'EXPIRED', 8800, NULL, 25300, 107, -51, -14, -71),
        (45, '20260326_CP_E010', '연초 다이어리 20% 할인', 'PERCENTAGE', 'EXPIRED', 8, 11000, 31000, 140, -60, -20, -80),
        (46, '20260326_CP_E011', '크리스마스 홈파티 10% 할인', 'FIXED', 'EXPIRED', 10200, NULL, 28700, 113, -49, -16, -69),
        (47, '20260326_CP_E012', '연말 회식 배달 6천원 할인', 'PERCENTAGE', 'EXPIRED', 12, 11800, 33600, 148, -58, -22, -78),
        (48, '20260326_CP_E013', '겨울 간식 붕어빵 2천원 할인', 'FIXED', 'EXPIRED', 11600, NULL, 32100, 119, -47, -18, -67),
        (49, '20260326_CP_E014', '눈 오는 날 택시 3천원 할인', 'PERCENTAGE', 'EXPIRED', 16, 12600, 36200, 156, -56, -24, -76),
        (50, '20260326_CP_E015', '설빔 아동복 15% 할인', 'FIXED', 'EXPIRED', 13000, NULL, 35500, 125, -45, -20, -65)
)
SELECT
    2000 + seq,
    coupon_code,
    name,
    coupon_type,
    status,
    discount_amount,
    max_discount_amount,
    min_order_amount,
    total_quantity,
    total_quantity,
    DATEADD('DAY', available_day_offset, CURRENT_TIMESTAMP),
    DATEADD('DAY', end_day_offset, CURRENT_TIMESTAMP),
    DATEADD('DAY', created_day_offset, CURRENT_TIMESTAMP),
    DATEADD('DAY', created_day_offset, CURRENT_TIMESTAMP),
    NULL
FROM coupon_source;
