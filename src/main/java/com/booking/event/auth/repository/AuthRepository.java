package com.booking.event.auth.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.booking.event.auth.constants.UserRoleEnum;
import com.booking.event.auth.entity.UserEntity;

@Repository 
public interface AuthRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findUserByEmailAndRole(String email, UserRoleEnum role);
  List<UserEntity> findAllByIdIn(Collection<Long> ids);
}
