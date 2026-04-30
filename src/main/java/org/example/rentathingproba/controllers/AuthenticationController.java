package org.example.rentathingproba.controllers;

import org.example.rentathingproba.dto.LoginUserDTO;
import org.example.rentathingproba.dto.RegisteredUserDTO;
import org.example.rentathingproba.dto.VerifiedUserDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.LoginResponse;
import org.example.rentathingproba.responses.MessageResponse;
import org.example.rentathingproba.service.application.AuthenticationService;
import org.example.rentathingproba.service.infrastructure.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;

    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }
    //expose the mapping for signing up, so ppl can create accounts. (postmapping)
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> register(@RequestBody RegisteredUserDTO registeredUserDTO) {
        authenticationService.signUp(registeredUserDTO);
        return ResponseEntity.ok(new MessageResponse(
                "Registration successful. Please check your email for the verification code."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDTO loginUserDTO) {
        User authenticatedUser = authenticationService.authenticate(loginUserDTO);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(
                jwtToken,
                jwtService.getExpirationTime(),
                authenticatedUser.getId(),
                authenticatedUser.getUsername(),
                authenticatedUser.getEmail()
        );
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody VerifiedUserDTO verifiedUserDTO) {
        authenticationService.verifyUser(verifiedUserDTO);
        return ResponseEntity.ok("Account verified successfully.");
    }

    @PostMapping("/resend")
    public ResponseEntity<MessageResponse> resendVerificationCode(@RequestParam String email) {
        authenticationService.resendVerificationCode(email);
        return ResponseEntity.ok(new MessageResponse(
                "Verification code sent."));
    }
}
