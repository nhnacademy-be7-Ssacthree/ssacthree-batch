INSERT INTO member (customer_id, member_status, member_birthdate)
VALUES (1, 'ACTIVE', CURRENT_DATE());

INSERT INTO coupon_rule (coupon_rule_id, coupon_rule_name, coupon_is_used)
VALUES (1, '생일 쿠폰',  1);


INSERT INTO coupon (coupon_id, coupon_rule_id, coupon_name, coupon_description, coupon_effective_period, coupon_effective_period_unit, coupon_create_at)
VALUES (1, 1, '생일 쿠폰', '생일 축하드려요! 10000원 이상 구매시 3000원 할인 가능!', 3, 0, CURRENT_TIMESTAMP);