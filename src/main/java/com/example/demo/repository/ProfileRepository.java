package com.example.demo.repository;

import com.example.demo.model.Profile;
import com.example.demo.model.ProfileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUuid(String uuid);
    Optional<Profile> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);
    boolean existsByUuid(String uuid);

    List<Profile> findByType(ProfileType type);
    List<Profile> findByDepartment(String department);

    // Search by name (partial match)
    List<Profile> findByFullNameContainingIgnoreCase(String fullName);
}
