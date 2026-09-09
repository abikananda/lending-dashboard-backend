package com.techconsulting.lending.domain;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter; import java.time.Instant;
@Entity @Table(name="users") @Getter @Setter
public class User { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String username; @Column(nullable=false,unique=true) private String email; @Column(name="lender_id",unique=true,length=64) private String lenderId; @Column(name="password_hash",nullable=false) private String passwordHash; private boolean enabled=true; @Column(name="created_at",insertable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",insertable=false,updatable=false) private Instant updatedAt; }
