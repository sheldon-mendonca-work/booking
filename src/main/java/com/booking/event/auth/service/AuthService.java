package com.booking.event.auth.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.booking.event.auth.constants.response.AuthResponseConstants;
import com.booking.event.auth.entity.UserEntity;
import com.booking.event.auth.repository.AuthRepository;
import com.booking.event.auth.dto.loginUser.LoginUserRequestDTO;
import com.booking.event.auth.dto.loginUser.LoginUserResponseDTO;
import com.booking.event.auth.dto.registerUser.RegisterUserRequestDTO;
import com.booking.event.auth.dto.registerUser.RegisterUserResponseDTO;
import com.booking.event.global.exception.auth.InvalidCredentialsError;
import com.booking.event.global.exception.auth.UserAlreadyExistsError;
import com.booking.event.global.security.JwtService;

@Service
public class AuthService {
  private AuthRepository authRepository;
  private PasswordEncoder passwordEncoder;
  private JwtService jwtService;

  public AuthService(
      AuthRepository authRepository,
      PasswordEncoder passwordEncoder,
      JwtService service
    ) {
    this.authRepository = authRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = service;
  }

  public RegisterUserResponseDTO handleUserRegistration(RegisterUserRequestDTO request) {
    // validation handled by jakarta validation

    // check if existing
    Optional<UserEntity> existingUser = authRepository.findUserByEmailAndRole(request.email(), request.role());
    if (!existingUser.isEmpty()) {
      throw new UserAlreadyExistsError(AuthResponseConstants.USER_ALREADY_EXISTS);
    }

    String passwordHash = passwordEncoder.encode(request.password());
    // Create entity
    UserEntity user = new UserEntity();
    user.setName(request.name());
    user.setEmail(request.email());
    user.setPasswordHash(passwordHash);
    user.setRole(request.role());

    // Save
    UserEntity savedUser = authRepository.save(user);

    String token = jwtService.generateToken(user);


    // Create response
    RegisterUserResponseDTO response = new RegisterUserResponseDTO(
        AuthResponseConstants.USER_CREATED_SUCCESSFULLY,
        HttpStatus.CREATED.value(),
        savedUser.getId().toString(),
        savedUser.getName(),
        savedUser.getEmail(),
        savedUser.getRole().name(),
        token
      );

    return response;
  }

  public LoginUserResponseDTO handleUserLogin(LoginUserRequestDTO request) {
    // validation handled by jakarta validation

    // check if existing
    Optional<UserEntity> existingUser = authRepository.findUserByEmailAndRole(request.email(), request.role());
    if (existingUser.isEmpty()) {
        throw new InvalidCredentialsError(
                AuthResponseConstants.INVALID_CREDENTIALS);
    }

    UserEntity user = existingUser.get();
    if (!passwordEncoder.matches(
            request.password(),
            user.getPasswordHash())) {

        throw new InvalidCredentialsError(
                AuthResponseConstants.INVALID_CREDENTIALS);
    }
    String token = jwtService.generateToken(user);
    
    // Create response
    LoginUserResponseDTO response = new LoginUserResponseDTO(
        AuthResponseConstants.USER_CREATED_SUCCESSFULLY,
        HttpStatus.CREATED.value(),
        user.getId().toString(),
        user.getName(),
        user.getEmail(),
        user.getRole().name(),
        token
    );

    return response;
  }

  
}
