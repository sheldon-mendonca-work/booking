package com.booking.event.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.event.auth.service.AuthService;
import com.booking.event.auth.dto.loginUser.LoginUserRequestDTO;
import com.booking.event.auth.dto.loginUser.LoginUserResponseDTO;
import com.booking.event.auth.dto.registerUser.RegisterUserRequestDTO;
import com.booking.event.auth.dto.registerUser.RegisterUserResponseDTO;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/auth")
public class AuthController {

  private AuthService authService;

  public AuthController(AuthService authService){
    this.authService = authService;
  }

  @PostMapping ("/register")
  public ResponseEntity<RegisterUserResponseDTO> handleUserRegistration(@Valid @RequestBody RegisterUserRequestDTO request){
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.handleUserRegistration(request));
  }

  @PostMapping ("/login")
  public ResponseEntity<LoginUserResponseDTO> handleUserRegistration(@Valid @RequestBody LoginUserRequestDTO request){
    return ResponseEntity.status(HttpStatus.OK).body(authService.handleUserLogin(request));
  }
}
