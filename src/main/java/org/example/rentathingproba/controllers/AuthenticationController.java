package org.example.rentathingproba.controllers;

import org.apache.coyote.Response;
import org.example.rentathingproba.dto.LoginUserDTO;
import org.example.rentathingproba.dto.RegisteredUserDTO;
import org.example.rentathingproba.dto.VerifiedUserDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.LoginResponse;
import org.example.rentathingproba.service.AuthenticationService;
import org.example.rentathingproba.service.JwtService;
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
    public ResponseEntity<User> register(@RequestBody RegisteredUserDTO registeredUserDTO) {
        User registeredUser = authenticationService.signUp(registeredUserDTO);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDTO loginUserDTO) {
        User authenticatedUser = authenticationService.authenticate(loginUserDTO);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifiedUserDTO verifiedUserDTO) {
        try {
            authenticationService.verifyUser(verifiedUserDTO);
            return ResponseEntity.ok("Račun je uspješno verificiran");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verifikacijski kod je poslan");
        } catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
