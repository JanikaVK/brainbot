package org.brainbot.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="assessments", indexes=@Index(name="ix_assessment_user_time", columnList="user_id,created_at"))
public class Assessment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
    @Column(nullable=false,length=120) private String prediction;
    @Column(nullable=false,length=64) private String modelVersion;
    @Lob @Column(nullable=false) private String probabilitiesJson;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected Assessment() { }
    public Assessment(AppUser user,String prediction,String modelVersion,String probabilitiesJson){this.user=user;this.prediction=prediction;this.modelVersion=modelVersion;this.probabilitiesJson=probabilitiesJson;}
    public Long getId(){return id;} public String getPrediction(){return prediction;} public String getModelVersion(){return modelVersion;} public String getProbabilitiesJson(){return probabilitiesJson;} public Instant getCreatedAt(){return createdAt;}
}
