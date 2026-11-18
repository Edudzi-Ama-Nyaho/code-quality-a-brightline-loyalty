package com.brightline.loyalty.member;

import java.util.Optional;

/** Finds loyalty scheme members by id. */
public interface MemberRepository {

    Optional<Member> findById(String id);
}