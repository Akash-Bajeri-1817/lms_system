package com.lms.user.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data                        // Lombok: generates getters, setters, toString, equals, hashCode
@Builder                     // Lombok: lets you do User.builder().email("x").build()
@NoArgsConstructor           // Lombok: generates empty constructor — JPA requires this
@AllArgsConstructor          // Lombok: generates constructor with all fields
@Entity                      // JPA: this class maps to a database table
@Table(name = "users")       // JPA: specifically the "users" table
public class User implements UserDetails
{
    @Id                                                    // this field is the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // database auto-increments it (BIGSERIAL)
    private Long id;

    @Column(nullable = false, unique = true)   // maps to the email column — cannot be null, must be unique
    private String email;

    @Column(nullable = false)
    private String password;                   // will store BCrypt hash, never plain text

    @Column(name = "first_name", nullable = false)
    private String firstName;                  // Java = camelCase, DB = snake_case, @Column bridges that

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)    // store "STUDENT" as text in DB, not 0/1/2 as a number
    @Column(nullable = false)
    private Role role;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp                          // Hibernate sets this automatically when row is inserted
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp                            // Hibernate updates this automatically on every save
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;  // our "username" is the email
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
