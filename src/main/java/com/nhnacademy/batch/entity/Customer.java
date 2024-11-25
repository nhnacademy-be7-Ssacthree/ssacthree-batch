package com.nhnacademy.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Setter
public class Customer {

    public Customer(String customerName, String customerEmail, String customerPhoneNumber) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhoneNumber = customerPhoneNumber;
    }

    @Column(name = "customer_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private long customerId;

    private String customerName;

    private String customerEmail;

    private String customerPhoneNumber;
}
