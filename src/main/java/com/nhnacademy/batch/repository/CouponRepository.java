package com.nhnacademy.batch.repository;

import com.nhnacademy.batch.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
