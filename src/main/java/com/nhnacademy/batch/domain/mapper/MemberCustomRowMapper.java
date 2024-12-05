package com.nhnacademy.batch.domain.mapper;

import com.nhnacademy.batch.domain.Member;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MemberCustomRowMapper implements RowMapper<Member> {
    @Override
    public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
        Member member = Member.builder()
                .customerId(rs.getLong("customer_id"))
                .build();
        return member;
    }
}
