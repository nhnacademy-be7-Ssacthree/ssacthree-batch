package com.nhnacademy.batch.domain;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Builder
@Table(name = "member")
public class Member {

    @Id
    @Column(name="customer_id")
    private Long customerId;

    @Column(name="member_status")
    private String memberStatus;

    @Column(name="member_birthdate")
    private String memberBirthdate;
}
