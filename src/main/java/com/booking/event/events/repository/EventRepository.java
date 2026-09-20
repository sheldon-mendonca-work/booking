package com.booking.event.events.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.booking.event.events.entity.EventEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM EventEntity e WHERE e.id = :eventId")
  Optional<EventEntity> findByIdForUpdate(@Param("eventId") Long eventId);
}
