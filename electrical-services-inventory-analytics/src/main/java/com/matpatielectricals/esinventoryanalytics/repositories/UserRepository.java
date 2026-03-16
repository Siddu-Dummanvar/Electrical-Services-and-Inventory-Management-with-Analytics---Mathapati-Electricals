package com.matpatielectricals.esinventoryanalytics.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.User;
import java.time.LocalDateTime; // ✅ Changed from LocalDate
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByEmailid(String emailid);
    
    // ✅ FIX: Find by timestamp range (Start of Day -> End of Day)
    List<User> findByRegistrationDateBetween(LocalDateTime start, LocalDateTime end);
}