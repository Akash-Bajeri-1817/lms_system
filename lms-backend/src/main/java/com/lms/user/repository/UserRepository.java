package com.lms.user.repository;

import com.lms.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository   // marks this as a Spring-managed repository bean
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring reads this method name and generates:
    // SELECT * FROM users WHERE email = ?
    // You don't write any SQL. Spring parses the method name itself.
    Optional<User> findByEmail(String email);

    // generates: SELECT COUNT(*) > 0 FROM users WHERE email = ?
    boolean existsByEmail(String email);
}