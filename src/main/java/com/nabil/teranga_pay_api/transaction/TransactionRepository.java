package com.nabil.teranga_pay_api.transaction;


import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    @Query("""
        select t from Transaction t
        where (t.fromAccount.id = :accountId or t.toAccount.id = :accountId)
          and t.type in :types
          and t.createdAt >= :from
          and t.createdAt < :to
        """)
    Page<Transaction> search(@Param("accountId") UUID accountId,
                             @Param("types") Collection<TransactionType> types,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);
}
