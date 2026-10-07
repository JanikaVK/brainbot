package org.brainbot.api.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.brainbot.api.domain.AppUser;
import org.brainbot.api.repo.UserRepository;
import org.brainbot.api.security.JwtService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {
    private static final Logger log=LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final JavaMailSender mail;
    private final String publicBaseUrl;
    private final String fromAddress;

    public PasswordResetService(UserRepository users, PasswordEncoder passwords, JwtService jwt,
            ObjectProvider<JavaMailSender> mail, @Value("${brainbot.password-reset.base-url:}") String publicBaseUrl,
            @Value("${brainbot.password-reset.from:BrainBot <no-reply@localhost>}") String fromAddress,
            @Value("${spring.mail.host:}") String mailHost) {
        this.users=users; this.passwords=passwords; this.jwt=jwt; this.mail=mail.getIfAvailable();
        this.publicBaseUrl=publicBaseUrl; this.fromAddress=fromAddress;
        this.mailConfigured=StringUtils.hasText(mailHost);
    }

    private final boolean mailConfigured;

    @Transactional
    public void request(String email) {
        if (!mailConfigured || mail == null || !StringUtils.hasText(publicBaseUrl))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Password recovery is not configured");
        String normalized=email.trim().toLowerCase(Locale.ROOT);
        var account=users.findByEmailForUpdate(normalized).orElse(null);
        if(account==null) return;
        Instant now=Instant.now();
        if(account.getPasswordResetRequestedAt()!=null && account.getPasswordResetRequestedAt().plus(REQUEST_COOLDOWN).isAfter(now)) return;
        account.setPasswordResetRequestedAt(now);
        users.save(account);
        String token=jwt.issuePasswordReset(account);
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(fromAddress); message.setTo(account.getEmail()); message.setSubject("Reset your BrainBot password");
        message.setText("Use this one-time link within 15 minutes to choose a new BrainBot password:\n\n"
                +publicBaseUrl+"?token="+URLEncoder.encode(token,StandardCharsets.UTF_8)+"\n\n"
                +"If you did not request this, you can ignore this email.");
        try { mail.send(message); }
        catch(MailException ex) { log.warn("Password reset email delivery failed"); }
    }

    @Transactional
    public void reset(String token,String newPassword) {
        try {
            var claims=jwt.parse(token);
            if(!"password_reset".equals(claims.get("purpose",String.class))) throw new IllegalArgumentException("Wrong token purpose");
            long id=Long.parseLong(claims.getSubject());
            AppUser account=users.findByIdForUpdate(id).orElseThrow(()->new IllegalArgumentException("Unknown account"));
            Long issuedVersion=claims.get("passwordResetVersion",Long.class);
            if(issuedVersion==null || issuedVersion.longValue()!=account.getPasswordResetVersion()) throw new IllegalArgumentException("Reset token has already been used");
            account.setPasswordHash(passwords.encode(newPassword));
            account.setPasswordResetVersion(account.getPasswordResetVersion()+1);
            account.setPasswordResetRequestedAt(null);
            users.save(account);
        } catch(ResponseStatusException ex) { throw ex; }
        catch(Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This password reset link is invalid or expired"); }
    }
}
