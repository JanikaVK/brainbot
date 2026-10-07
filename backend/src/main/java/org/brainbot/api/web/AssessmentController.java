package org.brainbot.api.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.brainbot.api.domain.AppUser;
import org.brainbot.api.domain.Assessment;
import org.brainbot.api.repo.AssessmentRepository;
import org.brainbot.api.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api")
public class AssessmentController {
    private final RestClient ml; private final UserRepository users; private final AssessmentRepository assessments; private final ObjectMapper json;
    public AssessmentController(RestClient mlRestClient,UserRepository users,AssessmentRepository assessments,ObjectMapper json){this.ml=mlRestClient;this.users=users;this.assessments=assessments;this.json=json;}

    @GetMapping("/model/status")
    public Map<?,?> status() {
        try { return ml.get().uri("/model/version").retrieve().body(Map.class); }
        catch(Exception ex) { return Map.of("available",false,"version","","message","Model service is currently unavailable."); }
    }

    @PostMapping("/assessment/predict") @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> predict(Authentication auth,@Valid @RequestBody CheckInController.Input input) {
        Map<String,Object> features=Map.of("mood",input.mood(),"energy",input.energy(),"sleep_quality",input.sleepQuality(),"workload",input.workload(),"concentration",input.concentration(),"stress",input.stress(),"social_connection",input.socialConnection(),"study_hours",input.studyHours(),"break_frequency",input.breakFrequency());
        try {
            Map<?,?> result=ml.post().uri("/predict").body(features).retrieve().body(Map.class);
            if(result==null || !"available".equals(result.get("status"))) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status","unavailable","message","Prediction is unavailable until a verified dataset has been used to train and install a model."));
            String label=String.valueOf(result.get("prediction")); String version=String.valueOf(result.get("model_version")); Object probabilities=result.get("probabilities");
            if(probabilities==null || !(probabilities instanceof Map<?,?>)) return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("status","unavailable","message","The model returned an incomplete result."));
            long userId=parseUserId(auth);
            String encoded=json.writeValueAsString(probabilities);
            Assessment saved=assessments.save(new Assessment(users.findById(userId).orElseThrow(),label,version,encoded));
            return ResponseEntity.ok(Map.of("id",saved.getId(),"status","available","prediction",label,"model_version",version,"probabilities",probabilities,"message","Model output is an estimate, not a medical diagnosis.","created_at",saved.getCreatedAt().toString()));
        } catch(RestClientResponseException ex) {
            if(ex.getStatusCode().value()==503) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status","unavailable","message","Prediction is unavailable until a verified dataset has been used to train and install a model."));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("status","unavailable","message","The model service could not process this request."));
        } catch(ResourceAccessException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status","unavailable","message","The model service is currently unavailable."));
        } catch(Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("status","unavailable","message","The prediction could not be saved."));
        }
    }

    @GetMapping("/assessments") @PreAuthorize("hasRole('STUDENT')")
    public List<Item> history(Authentication auth){
        long userId=parseUserId(auth);
        return assessments.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream().map(a->{
            try { Map<String,Double> probabilities=json.readValue(a.getProbabilitiesJson(),new TypeReference<>(){}); return new Item(a.getId(),a.getPrediction(),a.getModelVersion(),probabilities,a.getCreatedAt().toString()); }
            catch(Exception ex){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Assessment history could not be read");}
        }).toList();
    }

    @PostMapping("/assessment/explain") @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> explain(@Valid @RequestBody CheckInController.Input input){
        Map<String,Object> features=Map.of("mood",input.mood(),"energy",input.energy(),"sleep_quality",input.sleepQuality(),"workload",input.workload(),"concentration",input.concentration(),"stress",input.stress(),"social_connection",input.socialConnection(),"study_hours",input.studyHours(),"break_frequency",input.breakFrequency());
        try { return ResponseEntity.ok(ml.post().uri("/explain").body(features).retrieve().body(Map.class)); }
        catch(Exception ex){return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status","unavailable","message","An explanation is unavailable for the installed model."));}
    }

    private long parseUserId(Authentication auth){try{return Long.parseLong(auth.getName());}catch(Exception ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session is no longer valid");}}
    public record Item(long id,String prediction,String modelVersion,Map<String,Double> probabilities,String createdAt){}
}
