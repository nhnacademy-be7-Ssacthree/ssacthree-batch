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

}
