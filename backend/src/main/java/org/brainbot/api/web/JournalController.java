package org.brainbot.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.brainbot.api.domain.AppUser;
import org.brainbot.api.domain.JournalEntry;
import org.brainbot.api.repo.JournalRepository;
import org.brainbot.api.repo.UserRepository;
import org.brainbot.api.security.EncryptedPayloadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/journal") @PreAuthorize("hasRole('STUDENT')")
public class JournalController {
    private final JournalRepository entries; private final UserRepository users; private final EncryptedPayloadService encryption; private final ObjectMapper json;
    public JournalController(JournalRepository entries,UserRepository users,EncryptedPayloadService encryption,ObjectMapper json){this.entries=entries;this.users=users;this.encryption=encryption;this.json=json;}
    public record Write(@NotBlank @Size(max=100) String title,@NotBlank @Size(max=10000) String content) { }
    public record Content(String title,String content) { }
    public record Item(long id,String title,String content,Instant createdAt,Instant updatedAt) { }

    @PostMapping @Transactional public Item create(Authentication auth,@Valid @RequestBody Write input){
        long userId=id(auth); var encrypted=encrypt(userId,input); AppUser user=users.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session is no longer valid"));
        return item(entries.save(new JournalEntry(user,encrypted.iv(),encrypted.ciphertext())),userId);
    }
    @GetMapping public List<Item> list(Authentication auth){long userId=id(auth);return entries.findTop50ByUserIdOrderByUpdatedAtDesc(userId).stream().map(e->item(e,userId)).toList();}
    @PutMapping("/{entryId}") @Transactional public Item update(Authentication auth,@PathVariable long entryId,@Valid @RequestBody Write input){long userId=id(auth);JournalEntry existing=owned(entryId,userId);var encrypted=encrypt(userId,input);existing.replace(encrypted.iv(),encrypted.ciphertext());return item(existing,userId);}
    @DeleteMapping("/{entryId}") @Transactional public void delete(Authentication auth,@PathVariable long entryId){entries.delete(owned(entryId,id(auth)));}

    private JournalEntry owned(long entryId,long userId){return entries.findByIdAndUserId(entryId,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Journal entry not found"));}
    private EncryptedPayloadService.Encrypted encrypt(long userId,Write input){
        try{return encryption.encrypt(userId,json.writeValueAsString(new Content(input.title().trim(),input.content().trim())));}
        catch(Exception ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Encrypted journal storage is not configured");}
    }
    private Item item(JournalEntry entry,long userId){
        try{Content c=json.readValue(encryption.decrypt(userId,entry.getIv(),entry.getCiphertext()),Content.class);return new Item(entry.getId(),c.title(),c.content(),entry.getCreatedAt(),entry.getUpdatedAt());}
        catch(Exception ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"This journal entry could not be opened");}
    }
    private long id(Authentication auth){try{return Long.parseLong(auth.getName());}catch(Exception ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session is no longer valid");}}
}
