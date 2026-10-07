package org.brainbot.api.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Locale;
import org.brainbot.api.domain.*;
import org.brainbot.api.config.JwtProperties;
import org.brainbot.api.repo.UserRepository;
import org.brainbot.api.security.JwtService;
import org.brainbot.api.service.PasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users; private final PasswordEncoder passwords; private final JwtService jwt; private final JwtProperties jwtProperties; private final PasswordResetService resets;
    public AuthController(UserRepository users,PasswordEncoder passwords,JwtService jwt,JwtProperties jwtProperties,PasswordResetService resets){this.users=users;this.passwords=passwords;this.jwt=jwt;this.jwtProperties=jwtProperties;this.resets=resets;}
    public record Register(@NotBlank @Size(max=80) String name,@Email @NotBlank @Size(max=254) String email,@NotBlank @Size(min=12,max=72) String password,Role role,@AssertTrue boolean consentAccepted) { }
    public record Login(@Email @NotBlank String email,@NotBlank String password) { }
    public record ResetRequest(@Email @NotBlank @Size(max=254) String email) { }
    public record ResetConfirm(@NotBlank String token,@NotBlank @Size(min=12,max=72) String newPassword) { }
    public record Session(String token,String name,String email,Role role,long expiresInSeconds,long expiresAtEpochSeconds) { }
    @PostMapping("/register") public Session register(@Valid @RequestBody Register body) {
        String email=body.email().trim().toLowerCase(Locale.ROOT);
        if(users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Unable to register with those details");
        Role role=body.role()==null?Role.STUDENT:body.role();
        if(role!=Role.STUDENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Public registration is for students only");
        AppUser user=users.save(new AppUser(body.name().trim(),email,passwords.encode(body.password()),role,"wellness-v2")); return session(user);
    }
    @PostMapping("/login") public Session login(@Valid @RequestBody Login body) {
        AppUser user=users.findByEmail(body.email().trim().toLowerCase(Locale.ROOT)).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password"));
        if(!passwords.matches(body.password(),user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password");
        return session(user);
    }
    @PostMapping("/password-reset/request") public java.util.Map<String,String> requestPasswordReset(@Valid @RequestBody ResetRequest body) {
        resets.request(body.email());
        return java.util.Map.of("message","If an account matches that email, a reset link will be sent.");
    }
    @PostMapping("/password-reset/confirm") public java.util.Map<String,String> confirmPasswordReset(@Valid @RequestBody ResetConfirm body) {
        resets.reset(body.token(),body.newPassword());
        return java.util.Map.of("message","Password updated. Sign in with your new password.");
    }
    private Session session(AppUser u){String token=jwt.issue(u);long expiresAt=jwt.parse(token).getExpiration().toInstant().getEpochSecond();return new Session(token,u.getName(),u.getEmail(),u.getRole(),jwtProperties.expirationSeconds(),expiresAt);}
}
