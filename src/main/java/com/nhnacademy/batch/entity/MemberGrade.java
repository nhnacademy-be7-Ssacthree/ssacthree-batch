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
@Table(name = "member_grade")
public class MemberGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long memberGradeId;

    public MemberGrade(String memberGradeName, boolean memberGradeIsUsed, float memberGradePointSave) {
        this.memberGradeName = memberGradeName;
        this.memberGradeIsUsed = memberGradeIsUsed;
        this.memberGradePointSave = memberGradePointSave;
    }

    @Setter
    private String memberGradeName;

    @Setter
    private boolean memberGradeIsUsed;


    private LocalDateTime memberGradeCreateAt = LocalDateTime.now();

    @Setter
    private float memberGradePointSave;
}
