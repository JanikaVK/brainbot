package org.brainbot.api.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.brainbot.api.domain.AppUser;
import org.brainbot.api.domain.SupportRequest;
import org.brainbot.api.repo.SupportRequestRepository;
import org.brainbot.api.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/support-requests") @PreAuthorize("hasRole('STUDENT')")
public class SupportRequestController {
    private final SupportRequestRepository requests; private final UserRepository users;
    public SupportRequestController(SupportRequestRepository requests,UserRepository users){this.requests=requests;this.users=users;}
    public record Create(@NotNull SupportRequest.Kind kind,@Size(max=1000) String message) { }
    public record Item(long id,SupportRequest.Kind kind,SupportRequest.Status status,String message,Instant createdAt) { }
    @PostMapping public Item create(Authentication auth,@Valid @RequestBody Create input){
        AppUser user=current(auth);
        SupportRequest saved=requests.save(new SupportRequest(user,input.kind(),input.message()==null?null:input.message().trim()));
        return item(saved);
    }
    @GetMapping public List<Item> mine(Authentication auth){return requests.findTop50ByUserIdOrderByCreatedAtDesc(current(auth).getId()).stream().map(SupportRequestController::item).toList();}
    private AppUser current(Authentication auth){try{return users.findById(Long.parseLong(auth.getName())).orElseThrow();}catch(Exception ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session is no longer valid");}}
    private static Item item(SupportRequest r){return new Item(r.getId(),r.getKind(),r.getStatus(),r.getMessage(),r.getCreatedAt());}
}
