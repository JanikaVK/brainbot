package org.brainbot.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class StudentWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired org.brainbot.api.repo.UserRepository users;
    @Autowired org.brainbot.api.repo.SupportRequestRepository supportRequests;
    @Autowired org.brainbot.api.security.JwtService jwt;
    @Autowired PasswordEncoder passwords;
    @MockitoBean JavaMailSender mailSender;

    @Test void registerCheckInJournalSupportAndDeleteAccount() throws Exception {
        String session=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Test Student","email":"student@example.test","password":"strong-test-password-123","role":"STUDENT","consentAccepted":true}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"))
            .andReturn().getResponse().getContentAsString();
        var sessionJson=new com.fasterxml.jackson.databind.ObjectMapper().readTree(session);
        assertTrue(sessionJson.get("expiresAtEpochSeconds").asLong()>Instant.now().getEpochSecond());
        String token=sessionJson.get("token").asText();
        String checkIn="""
            {"mood":3,"energy":4,"sleepQuality":3,"workload":3,"concentration":4,"stress":2,"socialConnection":3,"studyHours":4,"breakFrequency":2}
            """;
        mvc.perform(post("/api/check-ins").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(checkIn)).andExpect(status().isOk()).andExpect(jsonPath("$.id").exists());
        mvc.perform(get("/api/check-ins").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].mood").value(3));
        mvc.perform(get("/api/check-ins/trends").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.checkInsLast7Days").value(1));
        mvc.perform(post("/api/assessment/predict").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(checkIn))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value("unavailable"));
        mvc.perform(get("/api/assessments").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/journal").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("""
            {"title":"Small win","content":"I took a restorative break."}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("I took a restorative break."));
        String supportMessage="I would like a conversation.";
        mvc.perform(post("/api/support-requests").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("""
            {"kind":"FACULTY_SUPPORT","message":"I would like a conversation."}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_ROUTING"))
            .andExpect(jsonPath("$.message").value(supportMessage));
        long studentId=users.findByEmail("student@example.test").orElseThrow().getId();
        var storedRequest=supportRequests.findTop50ByUser_IdOrderByCreatedAtDesc(studentId).get(0);
        org.junit.jupiter.api.Assertions.assertNotNull(storedRequest.getMessageIv());
        org.junit.jupiter.api.Assertions.assertNotNull(storedRequest.getMessageCiphertext());
        org.junit.jupiter.api.Assertions.assertFalse(new String(storedRequest.getMessageCiphertext(),java.nio.charset.StandardCharsets.UTF_8).contains(supportMessage));
        mvc.perform(get("/api/support-requests").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].message").value(supportMessage));
        mvc.perform(get("/api/account/export").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.format").value("brainbot-account-export-v1"))
            .andExpect(jsonPath("$.account.email").value("student@example.test"))
            .andExpect(jsonPath("$.checkIns.length()").value(1))
            .andExpect(jsonPath("$.journalEntries[0].content").value("I took a restorative break."))
            .andExpect(jsonPath("$.supportRequests[0].status").value("PENDING_ROUTING"))
            .andExpect(jsonPath("$.supportRequests[0].message").value(supportMessage))
            .andExpect(jsonPath("$.account.passwordHash").doesNotExist());
        mvc.perform(delete("/api/account").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/check-ins").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }

    @Test void checkInsRequireStudentAuthentication() throws Exception {
        mvc.perform(get("/api/check-ins")).andExpect(status().isUnauthorized());
    }

    @Test void facultyRoleCannotReadStudentCheckIns() throws Exception {
        var faculty=users.save(new org.brainbot.api.domain.AppUser("Faculty","faculty@example.test",passwords.encode("another-test-password-123"),org.brainbot.api.domain.Role.FACULTY,"wellness-v2"));
        mvc.perform(get("/api/check-ins").header("Authorization","Bearer "+jwt.issue(faculty))).andExpect(status().isForbidden());
    }

    @Test void passwordResetIsOneTimeAndRevokesExistingSessions() throws Exception {
        String response=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Reset Student","email":"reset@example.test","password":"original-password-123","role":"STUDENT","consentAccepted":true}
            """))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String oldToken=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("token").asText();
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"reset@example.test"}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("If an account matches that email, a reset link will be sent."));
        mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"nobody@example.test"}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("If an account matches that email, a reset link will be sent."));
        ArgumentCaptor<SimpleMailMessage> email=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(email.capture());
        String text=email.getValue().getText();
        String resetToken=text.substring(text.indexOf("?token=")+7).split("\\s")[0];
        mvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("token",resetToken,"newPassword","replacement-password-456"))))
            .andExpect(status().isOk());
        mvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("token",resetToken,"newPassword","another-replacement-456"))))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/check-ins").header("Authorization","Bearer "+oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"reset@example.test","password":"original-password-123"}
            """))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"reset@example.test","password":"replacement-password-456"}
            """))
            .andExpect(status().isOk());
    }
}
