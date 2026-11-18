package com.brightline.loyalty.member;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Keeps the members it is given in memory and finds them by id. */
public final class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> membersById = new HashMap<>();

    public InMemoryMemberRepository(Collection<Member> members) {
        for (Member member : members) {
            if (membersById.putIfAbsent(member.id(), member) != null) {
                throw new IllegalArgumentException("Duplicate member id: " + member.id());
            }
        }
    }

    @Override
    public Optional<Member> findById(String id) {
        return Optional.ofNullable(membersById.get(id));
    }
}