package com.tripplanner.data.repo;

import com.tripplanner.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for User entity operations with JPA.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
