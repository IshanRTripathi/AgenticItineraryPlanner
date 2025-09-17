package com.tripplanner.data.repo;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.data.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Repository for User entity operations with Firestore.
 */
@Repository
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(Firestore.class)
public class UserRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private static final String COLLECTION_NAME = "users";
    
    private final Firestore firestore;
    
    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Save a user to Firestore.
     */
    public User save(User user) throws ExecutionException, InterruptedException {
        logger.debug("Saving user: {}", user.getId());
        
        if (user.getId() == null) {
            // Generate ID for new user
            user.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        
        user.setUpdatedAt(Instant.now());
        
        firestore.collection(COLLECTION_NAME)
                .document(user.getId())
                .set(user)
                .get();
        
        logger.debug("User saved successfully: {}", user.getId());
        return user;
    }
    
    /**
     * Find user by ID.
     */
    public Optional<User> findById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Finding user by ID: {}", id);
        
        DocumentSnapshot document = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get()
                .get();
        
        if (document.exists()) {
            User user = document.toObject(User.class);
            if (user != null) {
                user.setId(document.getId());
            }
            logger.debug("User found: {}", id);
            return Optional.ofNullable(user);
        }
        
        logger.debug("User not found: {}", id);
        return Optional.empty();
    }
    
    /**
     * Find user by email.
     */
    public Optional<User> findByEmail(String email) throws ExecutionException, InterruptedException {
        logger.debug("Finding user by email: {}", email);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .get();
        
        if (!querySnapshot.isEmpty()) {
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            User user = document.toObject(User.class);
            if (user != null) {
                user.setId(document.getId());
            }
            logger.debug("User found by email: {}", email);
            return Optional.ofNullable(user);
        }
        
        logger.debug("User not found by email: {}", email);
        return Optional.empty();
    }
    
    /**
     * Update user's last login timestamp.
     */
    public void updateLastLogin(String userId) throws ExecutionException, InterruptedException {
        logger.debug("Updating last login for user: {}", userId);
        
        firestore.collection(COLLECTION_NAME)
                .document(userId)
                .update("lastLoginAt", Instant.now(), "updatedAt", Instant.now())
                .get();
        
        logger.debug("Last login updated for user: {}", userId);
    }
    
    /**
     * Delete user by ID.
     */
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Deleting user: {}", id);
        
        firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete()
                .get();
        
        logger.debug("User deleted: {}", id);
    }
    
    /**
     * Check if user exists by ID.
     */
    public boolean existsById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get()
                .get();
        
        return document.exists();
    }
    
    /**
     * Check if user exists by email.
     */
    public boolean existsByEmail(String email) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .get();
        
        return !querySnapshot.isEmpty();
    }
    
    /**
     * Find users created after a specific timestamp.
     */
    public List<User> findUsersCreatedAfter(Instant timestamp) throws ExecutionException, InterruptedException {
        logger.debug("Finding users created after: {}", timestamp);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereGreaterThan("createdAt", timestamp)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .get();
        
        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        user.setId(doc.getId());
                    }
                    return user;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Count total users.
     */
    public long count() throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .get()
                .get();
        
        return querySnapshot.size();
    }
}
