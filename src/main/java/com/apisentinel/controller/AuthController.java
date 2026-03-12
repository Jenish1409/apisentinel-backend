package com.apisentinel.controller;

import com.apisentinel.entity.User;
import com.apisentinel.payload.request.LoginRequest;
import com.apisentinel.payload.request.SignupRequest;
import com.apisentinel.payload.response.JwtResponse;
import com.apisentinel.payload.response.ApiResponseWrapper;
import com.apisentinel.repository.UserRepository;
import com.apisentinel.security.jwt.JwtUtils;
import com.apisentinel.security.services.UserDetailsImpl;
import com.apisentinel.service.EmailService;
import com.apisentinel.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired AuthenticationManager authenticationManager;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtils jwtUtils;
    @Autowired EmailService emailService;
    @Autowired OtpService otpService;

    // ── Current user info (used to sync role from DB on page load) ────────────
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponseWrapper<>(false, "Not authenticated"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getEmail())
                .map(u -> ResponseEntity.ok(new ApiResponseWrapper<>(true, "OK",
                        java.util.Map.of("email", u.getEmail(), "role", u.getRole() != null ? u.getRole() : "ROLE_USER"))))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Login ────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Login successful",
                new JwtResponse(jwt, userDetails.getId(), userDetails.getEmail(), userDetails.getRole())));
    }

    // ── Step 1: Request OTP ──────────────────────────────────────────────────
    @PostMapping("/register/request-otp")
    public ResponseEntity<?> requestOtp(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseWrapper<>(false, "Email is already registered."));
        }

        if (otpService.hasPending(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseWrapper<>(false, "A verification code was already sent. Please wait before requesting a new one."));
        }

        String encodedPassword = encoder.encode(signUpRequest.getPassword());
        String otp = otpService.createOtp(signUpRequest.getEmail(), encodedPassword);

        try {
            emailService.sendOtpEmail(signUpRequest.getEmail(), otp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponseWrapper<>(false, "Failed to send verification email. Check SMTP config."));
        }

        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Verification code sent to " + signUpRequest.getEmail() + ". Valid for 10 minutes."));
    }

    // ── Step 2: Verify OTP and create account ───────────────────────────────
    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseWrapper<>(false, "Email and OTP are required."));
        }

        String encodedPassword = otpService.validateAndConsume(email, otp);
        if (encodedPassword == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseWrapper<>(false, "Invalid or expired verification code."));
        }

        // Double-check email not taken between OTP request and verify
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseWrapper<>(false, "Email is already registered."));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        userRepository.save(user);

        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "Account created successfully! You can now log in."));
    }

    // ── Legacy register (kept for backward compatibility) ────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseWrapper<>(false, "Error: Email is already in use!"));
        }
        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponseWrapper<>(true, "User registered successfully!"));
    }
}
