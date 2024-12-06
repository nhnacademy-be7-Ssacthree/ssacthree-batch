DROP TABLE IF EXISTS member_coupon;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS coupon_rule;
DROP TABLE IF EXISTS member;

CREATE TABLE member (
                        customer_id BIGINT PRIMARY KEY,
                        member_status VARCHAR(10),
                        member_birthdate DATE
);

-- COUPON_RULE 테이블 생성


CREATE TABLE coupon_rule (
                             coupon_rule_id BIGINT NOT NULL AUTO_INCREMENT,

                             coupon_rule_name VARCHAR(20) NOT NULL,
                             coupon_is_used BOOLEAN NOT NULL,

                             PRIMARY KEY (coupon_rule_id)
);

-- COUPON 테이블 생성


CREATE TABLE coupon (
                        coupon_id BIGINT NOT NULL AUTO_INCREMENT,
                        coupon_rule_id BIGINT NOT NULL,
                        coupon_name VARCHAR(30) NOT NULL,
                        coupon_description TEXT NOT NULL,
                        coupon_effective_period INT NULL,
                        coupon_effective_period_unit TINYINT NULL COMMENT '단위 : 일.월,년',
                        coupon_create_at DATETIME NOT NULL,
                        coupon_expired_at DATETIME NULL,
                        PRIMARY KEY (coupon_id),

                        CONSTRAINT FK_coupon_rule_TO_coupon FOREIGN KEY (coupon_rule_id) REFERENCES coupon_rule(coupon_rule_id)
);




CREATE TABLE member_coupon (
                               member_coupon_id BIGINT NOT NULL AUTO_INCREMENT,
                               customer_id BIGINT NOT NULL,
                               coupon_id BIGINT NOT NULL,
                               member_coupon_created_at DATETIME NOT NULL,
                               member_coupon_expired_at DATETIME NOT NULL COMMENT '쿠폰 만료일이 현재 시간 이전이면, 만료된 쿠폰으로 간주',
                               member_coupon_used_at DATETIME NULL,
                               PRIMARY KEY (member_coupon_id),

    -- customer 외래키 연결 --
                               CONSTRAINT FK_customer_TO_member_coupon FOREIGN KEY (customer_id) REFERENCES member(customer_id),

    -- 쿠폰 외래키 연결 --
                               CONSTRAINT FK_coupon_TO_member_coupon FOREIGN KEY (coupon_id) REFERENCES coupon(coupon_id)
);