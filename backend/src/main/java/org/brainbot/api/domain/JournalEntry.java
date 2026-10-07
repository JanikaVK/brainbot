package org.brainbot.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="journal_entries", indexes=@Index(name="ix_journal_user_time", columnList="user_id,created_at"))
public class JournalEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private AppUser user;
    @Lob @Column(nullable=false) private byte[] iv;
    @Lob @Column(nullable=false) private byte[] ciphertext;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    protected JournalEntry() { }
    public JournalEntry(AppUser user,byte[] iv,byte[] ciphertext){this.user=user;this.iv=iv;this.ciphertext=ciphertext;}
    public Long getId(){return id;} public byte[] getIv(){return iv;} public byte[] getCiphertext(){return ciphertext;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void replace(byte[] iv,byte[] ciphertext){this.iv=iv;this.ciphertext=ciphertext;this.updatedAt=Instant.now();}
}
