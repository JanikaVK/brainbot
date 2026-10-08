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
import org.brainbot.api.security.EncryptedPayloadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/support-requests") @PreAuthorize("hasRole('STUDENT')")
public class SupportRequestController {
    private final SupportRequestRepository requests; private final UserRepository users; private final EncryptedPayloadService encryption;
    public SupportRequestController(SupportRequestRepository requests,UserRepository users,EncryptedPayloadService encryption){this.requests=requests;this.users=users;this.encryption=encryption;}
    public record Create(@NotNull SupportRequest.Kind kind,@Size(max=1000) String message) { }
    public record Item(long id,SupportRequest.Kind kind,SupportRequest.Status status,String message,Instant createdAt) { }
    @PostMapping public Item create(Authentication auth,@Valid @RequestBody Create input){
        AppUser user=current(auth);
        String message=input.message()==null?null:input.message().trim();
        var encrypted=message==null||message.isEmpty()?null:encryption.encrypt(user.getId(),message);
        SupportRequest saved=requests.save(new SupportRequest(user,input.kind(),encrypted==null?null:encrypted.iv(),encrypted==null?null:encrypted.ciphertext()));
        return item(saved);
    }
    @GetMapping public List<Item> mine(Authentication auth){return requests.findTop50ByUserIdOrderByCreatedAtDesc(current(auth).getId()).stream().map(this::item).toList();}
    private AppUser current(Authentication auth){try{return users.findById(Long.parseLong(auth.getName())).orElseThrow();}catch(Exception ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session is no longer valid");}}
    private Item item(SupportRequest r){
        String message=null;
        if(r.getMessageCiphertext()!=null) try { message=encryption.decrypt(r.getUserId(),r.getMessageIv(),r.getMessageCiphertext()); }
        catch(Exception ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Encrypted support request could not be opened"); }
        return new Item(r.getId(),r.getKind(),r.getStatus(),message,r.getCreatedAt());
    }
}
