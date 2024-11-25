package com.nhnacademy.batch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    public Coupon(String couponName, String couponDescription, int couponEffectivePeriod, CouponEffectivePeriodUnit couponEffectivePeriodUnit, LocalDateTime couponExpiredAt, CouponRule couponRule) {
        this.couponName = couponName;
        this.couponDescription = couponDescription;
        this.couponEffectivePeriod = couponEffectivePeriod;
        this.couponEffectivePeriodUnit = couponEffectivePeriodUnit;
        this.couponExpiredAt = couponExpiredAt;
        this.couponRule = couponRule;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="coupon_id")
    private Long couponId;

    @NotBlank
    @Size(max = 30)
    private String couponName;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String couponDescription;

    private int couponEffectivePeriod;

    private CouponEffectivePeriodUnit couponEffectivePeriodUnit;

    @NotNull
    private LocalDateTime couponCreateAt = LocalDateTime.now();

    private LocalDateTime couponExpiredAt;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "couponRuleId")
    private CouponRule couponRule;
}
