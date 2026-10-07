package org.brainbot.api.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import org.brainbot.api.domain.*;
import org.brainbot.api.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/check-ins")
public class CheckInController {
    private final CheckInRepository checkIns; private final UserRepository users;
    public CheckInController(CheckInRepository c,UserRepository u){checkIns=c;users=u;}
    public record Input(@Min(1) @Max(5) int mood,@Min(1) @Max(5) int energy,@Min(1) @Max(5) int sleepQuality,@Min(1) @Max(5) int workload,@Min(1) @Max(5) int concentration,@Min(1) @Max(5) int stress,@Min(1) @Max(5) int socialConnection,@DecimalMin("0") @DecimalMax("24") double studyHours,@Min(0) @Max(30) int breakFrequency) { }
    public record Item(long id,Instant createdAt,int mood,int energy,int sleepQuality,int workload,int concentration,int stress,int socialConnection,double studyHours,int breakFrequency) { }
    public record Trend(int checkInsLast7Days,Double averageMood,Double averageEnergy,Double averageStress) { }
    @PostMapping @PreAuthorize("hasRole('STUDENT')") public Item create(Authentication auth,@Valid @RequestBody Input in){
        AppUser u=current(auth); CheckIn c=checkIns.save(new CheckIn(u,in.mood(),in.energy(),in.sleepQuality(),in.workload(),in.concentration(),in.stress(),in.socialConnection(),in.studyHours(),in.breakFrequency()));return item(c);
    }
    @GetMapping @PreAuthorize("hasRole('STUDENT')") public List<Item> history(Authentication auth){return checkIns.findTop90ByUserIdOrderByCreatedAtDesc(current(auth).getId()).stream().map(CheckInController::item).toList();}
    @GetMapping("/trends") @PreAuthorize("hasRole('STUDENT')") public Trend trends(Authentication auth){
        Instant cutoff=Instant.now().minus(Duration.ofDays(7));
        List<CheckIn> recent=checkIns.findTop90ByUserIdOrderByCreatedAtDesc(current(auth).getId()).stream().filter(c->!c.getCreatedAt().isBefore(cutoff)).toList();
        if(recent.isEmpty()) return new Trend(0,null,null,null);
        return new Trend(recent.size(),recent.stream().mapToInt(CheckIn::getMood).average().orElseThrow(),recent.stream().mapToInt(CheckIn::getEnergy).average().orElseThrow(),recent.stream().mapToInt(CheckIn::getStress).average().orElseThrow());
    }
    private AppUser current(Authentication auth){try{return users.findById(Long.parseLong(auth.getName())).orElseThrow();}catch(Exception e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session is no longer valid");}}
    private static Item item(CheckIn c){return new Item(c.getId(),c.getCreatedAt(),c.getMood(),c.getEnergy(),c.getSleepQuality(),c.getWorkload(),c.getConcentration(),c.getStress(),c.getSocialConnection(),c.getStudyHours(),c.getBreakFrequency());}
}
