package com.matpatielectricals.esinventoryanalytics.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.Admin;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {
    // Basic login check
    Admin findByEmail(String email);
}