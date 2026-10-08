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
    @Lob @Column(name="message_iv") private byte[] messageIv;
    @Lob @Column(name="message_ciphertext") private byte[] messageCiphertext;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt=Instant.now();
    protected SupportRequest() { }
    public SupportRequest(AppUser user, Kind kind, byte[] messageIv, byte[] messageCiphertext) { this.user=user; this.kind=kind; this.messageIv=messageIv; this.messageCiphertext=messageCiphertext; }
    public Long getId(){return id;} public Kind getKind(){return kind;} public Status getStatus(){return status;} public byte[] getMessageIv(){return messageIv;} public byte[] getMessageCiphertext(){return messageCiphertext;} public Long getUserId(){return user.getId();} public Instant getCreatedAt(){return createdAt;}
}
