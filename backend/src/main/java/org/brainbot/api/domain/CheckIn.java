package org.brainbot.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="check_ins", indexes=@Index(name="ix_checkin_user_time", columnList="user_id,created_at"))
public class CheckIn {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) private AppUser user;
    @Column(nullable=false) private int mood, energy, sleepQuality, workload, concentration, stress, socialConnection;
    @Column(nullable=false) private double studyHours;
    @Column(nullable=false) private int breakFrequency;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt=Instant.now();
    protected CheckIn() { }
    public CheckIn(AppUser u, int mood,int energy,int sleep,int workload,int concentration,int stress,int social,double hours,int breaks){
        this.user=u;this.mood=mood;this.energy=energy;this.sleepQuality=sleep;this.workload=workload;this.concentration=concentration;this.stress=stress;this.socialConnection=social;this.studyHours=hours;this.breakFrequency=breaks;
    }
    public Long getId(){return id;} public Instant getCreatedAt(){return createdAt;}
    public int getMood(){return mood;} public int getEnergy(){return energy;} public int getSleepQuality(){return sleepQuality;} public int getWorkload(){return workload;} public int getConcentration(){return concentration;} public int getStress(){return stress;} public int getSocialConnection(){return socialConnection;} public double getStudyHours(){return studyHours;} public int getBreakFrequency(){return breakFrequency;}
}
