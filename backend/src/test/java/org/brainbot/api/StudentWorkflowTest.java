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

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class StudentWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired org.brainbot.api.repo.UserRepository users;
    @Autowired org.brainbot.api.security.JwtService jwt;
    @Autowired PasswordEncoder passwords;

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
        mvc.perform(post("/api/support-requests").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("""
            {"kind":"FACULTY_SUPPORT","message":"I would like a conversation."}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_ROUTING"));
        mvc.perform(get("/api/account/export").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.format").value("brainbot-account-export-v1"))
            .andExpect(jsonPath("$.account.email").value("student@example.test"))
            .andExpect(jsonPath("$.checkIns.length()").value(1))
            .andExpect(jsonPath("$.journalEntries[0].content").value("I took a restorative break."))
            .andExpect(jsonPath("$.supportRequests[0].status").value("PENDING_ROUTING"))
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
}
