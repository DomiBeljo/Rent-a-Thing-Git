package org.example.rentathingproba.service;

import jakarta.mail.MessagingException;
import org.example.rentathingproba.dto.LoginUserDTO;
import org.example.rentathingproba.dto.RegisteredUserDTO;
import org.example.rentathingproba.dto.VerifiedUserDTO;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    public User signUp(RegisteredUserDTO input){
        User user = new User(input.getUsername(), input.getEmail(), passwordEncoder.encode(input.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        sendVerificationEmail(user);
        return userRepository.save(user);
    } //da kod bude pregledniji: mapper/converter

    public User authenticate(LoginUserDTO input){
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User nije pronaden"));

        if (!user.isEnabled()){
            throw new RuntimeException("Račun nije verificiran. Molimo Vas verificirajte ga.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword())
        );
        return user;
    }

    public void verifyUser(VerifiedUserDTO input){
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()){
            User user = optionalUser.get();
            if(user.getVerificationCodeExpiration().isBefore(LocalDateTime.now())){
                throw new RuntimeException("Kod za verifikaciju je istekao.");
            }
            if(user.getVerificationCode().equals(input.getVerificationCode())){
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiration(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Kod za verifikaciju nije ispravan. Pokušajte ponovno");
            }
        } else{
            throw new RuntimeException("Korisnik nije pronađen");
        }
    }

    //resendanje verification codea.
    public void resendVerificationCode(String email){
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()){
            User user = optionalUser.get();
            if (user.isEnabled()){
                throw new RuntimeException("Račun je već verificiran!");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(5));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else{
            throw new RuntimeException("Korisnik nije pronađen.");
        }
    }

    public void sendVerificationEmail(User user){
        String subject = "Verifikacija korisničkog računa";
        String verificationCode = user.getVerificationCode();
        //Kod za html
        String htmlMessage = """
                <div style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 500px; margin: auto; background: white; padding: 30px; border-radius: 10px; text-align: center;">

                <h2 style="color: #333;">Rent-a-Thing</h2>

                <p style="font-size: 16px; color: #555;">
                Pozdrav <b>%s</b>,
                </p>

                <p style="font-size: 16px; color: #555;">
                Hvala na registraciji! Za dovršetak procesa, unesite verifikacijski kod:
                </p>

                <div style="margin: 30px 0;">
                <span style="display: inline-block; padding: 15px 25px; font-size: 24px; letter-spacing: 5px;
        background-color: #4CAF50; color: white; border-radius: 8px;">
                %s
                </span>
                </div>

                <p style="font-size: 14px; color: #999;">
                Kod vrijedi 15 minuta.
                </p>

                <hr style="margin: 30px 0;">

                <p style="font-size: 12px; color: #aaa;">
                Ako niste vi napravili ovu registraciju, slobodno ignorirajte ovu poruku.
                </p>

                </div>
                </div>
                """.formatted(user.getUsername(), verificationCode);
        try{
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch(MessagingException e){
            e.printStackTrace();
        }
    }


    private String generateVerificationCode(){
        Random random = new Random();
        int code = random.nextInt(999999) + 100000;
        return String.valueOf(code);
    }
}
