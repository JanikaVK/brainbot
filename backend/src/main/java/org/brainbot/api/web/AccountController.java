package org.brainbot.api.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.brainbot.api.domain.*;
import org.brainbot.api.repo.*;
import org.brainbot.api.security.EncryptedPayloadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/account")
public class AccountController {
    private final UserRepository users;
    private final CheckInRepository checkIns;
    private final SupportRequestRepository requests;
    private final AssessmentRepository assessments;
    private final JournalRepository journals;
    private final EncryptedPayloadService encryption;
    private final ObjectMapper json;

    public AccountController(UserRepository users, CheckInRepository checkIns, SupportRequestRepository requests,
            AssessmentRepository assessments, JournalRepository journals, EncryptedPayloadService encryption, ObjectMapper json) {
        this.users=users; this.checkIns=checkIns; this.requests=requests; this.assessments=assessments;
        this.journals=journals; this.encryption=encryption; this.json=json;
    }

    public record AccountData(String name, String email, Role role) { }
    public record AssessmentData(long id, String prediction, String modelVersion, Map<String,Double> probabilities, Instant createdAt) { }
    public record SupportData(long id, SupportRequest.Kind kind, SupportRequest.Status status, String message, Instant createdAt) { }
    public record DataExport(String format, Instant exportedAt, AccountData account,
            List<CheckInController.Item> checkIns, List<AssessmentData> assessments,
            List<JournalController.Item> journalEntries, List<SupportData> supportRequests) { }

    @GetMapping("/export") @PreAuthorize("hasRole('STUDENT')") @Transactional(readOnly=true)
    public DataExport export(Authentication authentication) {
        long userId=userId(authentication);
        AppUser user=users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session is no longer valid"));
        try {
            List<CheckInController.Item> checkInItems=checkIns.findTop90ByUserIdOrderByCreatedAtDesc(userId).stream()
                    .map(c->new CheckInController.Item(c.getId(),c.getCreatedAt(),c.getMood(),c.getEnergy(),c.getSleepQuality(),c.getWorkload(),c.getConcentration(),c.getStress(),c.getSocialConnection(),c.getStudyHours(),c.getBreakFrequency())).toList();
            List<AssessmentData> assessmentItems=assessments.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream().map(a->{
                try { return new AssessmentData(a.getId(),a.getPrediction(),a.getModelVersion(),json.readValue(a.getProbabilitiesJson(),new TypeReference<>(){}),a.getCreatedAt()); }
                catch(Exception ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Your account export could not be created"); }
            }).toList();
            List<JournalController.Item> journalItems=journals.findTop50ByUserIdOrderByUpdatedAtDesc(userId).stream().map(entry->{
                try {
                    JournalController.Content content=json.readValue(encryption.decrypt(userId,entry.getIv(),entry.getCiphertext()),JournalController.Content.class);
                    return new JournalController.Item(entry.getId(),content.title(),content.content(),entry.getCreatedAt(),entry.getUpdatedAt());
                } catch(Exception ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Encrypted journal data could not be included in the export"); }
            }).toList();
            List<SupportData> supportItems=requests.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                    .map(r->{
                        try {
                            String message=r.getMessageCiphertext()==null?null:encryption.decrypt(userId,r.getMessageIv(),r.getMessageCiphertext());
                            return new SupportData(r.getId(),r.getKind(),r.getStatus(),message,r.getCreatedAt());
                        } catch(Exception ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Encrypted support request data could not be included in the export"); }
                    }).toList();
            return new DataExport("brainbot-account-export-v1",Instant.now(),new AccountData(user.getName(),user.getEmail(),user.getRole()),checkInItems,assessmentItems,journalItems,supportItems);
        } catch(ResponseStatusException ex) { throw ex; }
        catch(Exception ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Your account export could not be created"); }
    }

    @DeleteMapping @PreAuthorize("isAuthenticated()") @Transactional
    public void delete(Authentication authentication) {
        long userId=userId(authentication);
        if(!users.existsById(userId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found");
        checkIns.deleteByUserId(userId);
        requests.deleteByUserId(userId);
        assessments.deleteByUserId(userId);
        journals.deleteByUserId(userId);
        users.deleteById(userId);
    }

    private long userId(Authentication authentication) {
        try { return Long.parseLong(authentication.getName()); }
        catch(Exception ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session is no longer valid"); }
    }
}
