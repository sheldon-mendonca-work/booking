package com.booking.event.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.booking.event.auth.entity.UserEntity;
import com.booking.event.booking.entity.BookingEntity;

@Repository 
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
  @Query("""
      SELECT b
      FROM BookingEntity b
      JOIN FETCH b.event
      JOIN FETCH b.customer
      WHERE b.event.id = :eventId
      """)
  List<BookingEntity> findAllByEventIdWithEventAndCustomer(@Param("eventId") Long eventId);

  @Query("""
      SELECT b
      FROM BookingEntity b
      JOIN FETCH b.event
      JOIN FETCH b.customer
      """)
  List<BookingEntity> findAllWithEventAndCustomer();

  @Query("""
      SELECT b
      FROM BookingEntity b
      JOIN FETCH b.event
      JOIN FETCH b.customer
      WHERE b.id = :bookingId
      """)
  Optional<BookingEntity> findByIdWithEventAndCustomer(@Param("bookingId") Long bookingId);

  @Query("""
      SELECT DISTINCT b.customer
      FROM BookingEntity b
      JOIN b.customer
      WHERE b.event.id = :eventId
      """)
  List<UserEntity> findDistinctCustomersByEventId(@Param("eventId") Long eventId);
}
