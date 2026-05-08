package com.voltconnect.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 *
 * <p>Provides standard CRUD operations plus phone-based lookup methods
 * used during OTP verification and profile upsert.
 *
 * <p>Satisfies Requirements 1.2, 2.2.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * Finds a user by their phone number.
     *
     * @param phone the phone number to search for
     * @return an {@link Optional} containing the user if found
     */
    Optional<UserEntity> findByPhone(String phone);

    /**
     * Returns {@code true} if a user with the given phone number exists.
     *
     * @param phone the phone number to check
     * @return {@code true} if a user with this phone exists
     */
    boolean existsByPhone(String phone);
}
