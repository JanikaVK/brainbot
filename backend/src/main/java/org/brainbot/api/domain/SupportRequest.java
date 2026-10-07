package org.brainbot.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="support_requests", indexes=@Index(name="ix_support_user_time", columnList="user_id,created_at"))
public class SupportRequest {
    public enum Kind { FACULTY_SUPPORT, THERAPIST_SUPPORT }
    public enum Status { PENDING_ROUTING }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) private AppUser user;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private Kind kind;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=24) private Status status=Status.PENDING_ROUTING;
    @Column(length=1000) private String message;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt=Instant.now();
    protected SupportRequest() { }
    public SupportRequest(AppUser user, Kind kind, String message) { this.user=user; this.kind=kind; this.message=message; }
    public Long getId(){return id;} public Kind getKind(){return kind;} public Status getStatus(){return status;} public String getMessage(){return message;} public Instant getCreatedAt(){return createdAt;}
}
