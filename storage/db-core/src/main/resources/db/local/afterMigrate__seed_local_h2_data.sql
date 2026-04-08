-- Local H2 sample data
-- Coupon seed below is yogieat-style mock data for local manual testing only.
-- It mirrors yogieat's region/category tone, but is not synchronized with yogieat-server DB rows.
-- Admin account: minjun.kim@naver.com / coupon1234!
-- All seeded users share the same password: coupon1234!

DELETE FROM t_authentication_history;
DELETE FROM t_coupon_issue;
DELETE FROM t_restaurant_coupon;
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
    traffic_policy,
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
    traffic_policy,
    discount_amount,
    max_discount_amount,
    min_order_amount,
    total_quantity,
    created_hour_offset
) AS (
    VALUES
        (1, '20260408_CP_R001', '홍대입구역 일식 점심 사시미 4천원 할인', 'FIXED', 'HOT_FCFS_ASYNC', 4000, NULL, 18000, 100, -120),
        (2, '20260408_CP_R002', '강남역 양식 브런치 파스타 10% 할인', 'PERCENTAGE', 'HOT_FCFS_ASYNC', 10, 6000, 22000, 100, -116),
        (3, '20260408_CP_R003', '을지로3가역 한식 직장인 백반 3천원 할인', 'FIXED', 'HOT_FCFS_ASYNC', 3000, NULL, 15000, 100, -112),
        (4, '20260408_CP_R004', '잠실역 아시안 저녁 쌀국수 12% 할인', 'PERCENTAGE', 'HOT_FCFS_ASYNC', 12, 7000, 25000, 100, -108),
        (5, '20260408_CP_R005', '공덕역 중식 점심 마라탕 3500원 할인', 'FIXED', 'HOT_FCFS_ASYNC', 3500, NULL, 17000, 100, -104),
        (6, '20260408_CP_R006', '사당역 한식 저녁 고기구이 5천원 할인', 'FIXED', 'HOT_FCFS_ASYNC', 5000, NULL, 28000, 100, -100),
        (7, '20260408_CP_R007', '종로3가역 일식 회식 스시 15% 할인', 'PERCENTAGE', 'HOT_FCFS_ASYNC', 15, 9000, 32000, 100, -96),
        (8, '20260408_CP_R008', '삼각지역 양식 데이트 스테이크 6천원 할인', 'FIXED', 'HOT_FCFS_ASYNC', 6000, NULL, 35000, 100, -92),
        (9, '20260408_CP_R009', '강남역 아시안 점심 팟타이 4천원 할인', 'FIXED', 'HOT_FCFS_ASYNC', 4000, NULL, 20000, 100, -88),
        (10, '20260408_CP_R010', '홍대입구역 양식 저녁 화덕피자 12% 할인', 'PERCENTAGE', 'HOT_FCFS_ASYNC', 12, 8000, 26000, 100, -84),
        (11, '20260408_CP_R011', '잠실역 한식 가족 외식 불고기 5천원 할인', 'FIXED', 'HOT_FCFS_ASYNC', 5000, NULL, 30000, 100, -80),
        (12, '20260408_CP_R012', '공덕역 중식 야근 짜장면 8% 할인', 'PERCENTAGE', 'HOT_FCFS_ASYNC', 8, 5000, 12000, 100, -76)
)
SELECT
    2000 + seq,
    coupon_code,
    name,
    coupon_type,
    'ACTIVE',
    traffic_policy,
    discount_amount,
    max_discount_amount,
    min_order_amount,
    total_quantity,
    total_quantity,
    DATEADD('DAY', -7, CURRENT_TIMESTAMP),
    DATEADD('DAY', 30, CURRENT_TIMESTAMP),
    DATEADD('HOUR', created_hour_offset, CURRENT_TIMESTAMP),
    DATEADD('HOUR', created_hour_offset, CURRENT_TIMESTAMP),
    NULL
FROM coupon_source;

-- Today's restaurant coupon mappings for local manual testing only.
-- mock restaurantId 101L -> couponId 2001L -> 홍대입구역 일식 점심 사시미 4천원 할인
-- mock restaurantId 201L -> couponId 2002L -> 강남역 양식 브런치 파스타 10% 할인
-- mock restaurantId 301L -> couponId 2003L -> 을지로3가역 한식 직장인 백반 3천원 할인
INSERT INTO t_restaurant_coupon (
    id,
    restaurant_id,
    coupon_id,
    status,
    available_at,
    end_at,
    created_at,
    updated_at,
    deleted_at
)
VALUES
    (
        3001,
        101,
        2001,
        'ACTIVE',
        DATEADD('DAY', -1, CURRENT_TIMESTAMP),
        DATEADD('DAY', 7, CURRENT_TIMESTAMP),
        DATEADD('HOUR', -3, CURRENT_TIMESTAMP),
        DATEADD('HOUR', -3, CURRENT_TIMESTAMP),
        NULL
    ),
    (
        3002,
        201,
        2002,
        'ACTIVE',
        DATEADD('DAY', -1, CURRENT_TIMESTAMP),
        DATEADD('DAY', 7, CURRENT_TIMESTAMP),
        DATEADD('HOUR', -2, CURRENT_TIMESTAMP),
        DATEADD('HOUR', -2, CURRENT_TIMESTAMP),
        NULL
    ),
    (
        3003,
        301,
        2003,
        'ACTIVE',
        DATEADD('DAY', -1, CURRENT_TIMESTAMP),
        DATEADD('DAY', 7, CURRENT_TIMESTAMP),
        DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
        DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
        NULL
    );
