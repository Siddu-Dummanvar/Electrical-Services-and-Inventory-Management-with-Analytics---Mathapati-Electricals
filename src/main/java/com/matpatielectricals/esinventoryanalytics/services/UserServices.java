package com.matpatielectricals.esinventoryanalytics.services;


import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.matpatielectricals.esinventoryanalytics.entities.User;
import com.matpatielectricals.esinventoryanalytics.repositories.UserRepository;

@Service
public class UserServices {

    @Autowired
    private UserRepository userRepository;

    /**
     * Registers a new user.
     * @param user The user entity to register.
     * @throws DataIntegrityViolationException if the email already exists.
     */
    public void registerUserService(User user) {
        
        // Check if email already exists
        if (userRepository.existsById(user.getEmailid())) {
            throw new DataIntegrityViolationException("This email ID is already registered!");
        }

        
        
        
        user.setRole("USER"); // Set default role for all new users
        user.setRegistrationDate(LocalDateTime.now()); // Set the registration timestamp
        
        // Save new user to DB
        userRepository.save(user);
    }
    
    /**
     * Authenticates a user.
     * @param emailid The user's email.
     * @param password The user's password.
     * @return true if login is successful, false otherwise.
     */
    public boolean loginUserService(String emailid, String password) {
        User user = userRepository.findById(emailid).orElse(null);
        
        if (user != null && user.getPassword().equals(password)) {
        	return "USER".equals(user.getRole());
        }
        
        return false; // Login failed
    }

}
