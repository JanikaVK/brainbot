package org.brainbot.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="app_users", uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=80) private String name;
    @Column(nullable=false, length=254) private String email;
    @Column(nullable=false, length=100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Role role;
    @Column(nullable=false, length=32) private String consentVersion;
    @Column(nullable=false) private Instant consentedAt;
    @Column(nullable=false, updatable=false) private Instant createdAt = Instant.now();
    protected AppUser() { }
    public AppUser(String name, String email, String passwordHash, Role role, String consentVersion) { this.name=name; this.email=email; this.passwordHash=passwordHash; this.role=role; this.consentVersion=consentVersion; this.consentedAt=Instant.now(); }
    public Long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public Role getRole(){return role;}
}
