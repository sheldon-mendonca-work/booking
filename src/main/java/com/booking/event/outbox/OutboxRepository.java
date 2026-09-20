package com.booking.event.outbox;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM OutboxEntity o
            WHERE o.status IN :statuses
            ORDER BY o.createdAt ASC
            """)
    List<OutboxEntity> findNextPublishableBatch(
            @Param("statuses") Collection<OutboxStatus> statuses,
            Pageable pageable);
}
