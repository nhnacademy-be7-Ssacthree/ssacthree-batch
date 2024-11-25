package com.nhnacademy.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Table(name = "member")
public class Member {

    public Member(
            Customer customer,
            String loginId,
            String password,
            String birthdate
    ) {
        this.customer = customer;
        this.memberLoginId = loginId;
        this.memberPassword = password;
        this.memberBirthdate = birthdate;
    }

    @Id
    private long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @Setter
    private Customer customer;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_grade_id")
    @Setter
    private MemberGrade memberGrade;


    private String memberLoginId;


    private String memberPassword;


    private String memberBirthdate;


    private LocalDateTime memberCreatedAt = LocalDateTime.now();


    private LocalDateTime memberLastLoginAt;


    @Enumerated(EnumType.STRING)
    @Setter
    private MemberStatus memberStatus = MemberStatus.ACTIVE;

    @Setter
    private int memberPoint = 0;
}
