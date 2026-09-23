package com.matpatielectricals.esinventoryanalytics;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.matpatielectricals.esinventoryanalytics.entities.Category;
import com.matpatielectricals.esinventoryanalytics.entities.User;
import com.matpatielectricals.esinventoryanalytics.repositories.CategoryRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.UserRepository;
import java.time.LocalDateTime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ElectricalServicesInventoryAnalyticsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElectricalServicesInventoryAnalyticsApplication.class, args);
	}
	
	/* This bean runs on startup to populate the database with
    * default categories and the default adminnn user.
    */
   @Bean
   CommandLineRunner initDatabase(CategoryRepository categoryRepository, UserRepository userRepository) {
       return args -> {
           
           // --- Create Default Categories ---
           if (categoryRepository.count() == 0) {
               System.out.println("Creating default categories...");
               categoryRepository.save(new Category("Fan"));
               categoryRepository.save(new Category("LED Bulb"));
               categoryRepository.save(new Category("Switchboard"));
               categoryRepository.save(new Category("Screwdriver"));
               categoryRepository.save(new Category("Tester"));
               categoryRepository.save(new Category("Inverter"));
               categoryRepository.save(new Category("MCB"));
               categoryRepository.save(new Category("Extension Board"));
               categoryRepository.save(new Category("Electrical Wire"));
               System.out.println("Finished creating categories.");
           } else {
               System.out.println("Categories already exist.");
           }

           // --- Create Default aAdmin User ---
           String adminEmail = "admin@gmail.com";
           if (userRepository.findByEmailid(adminEmail) == null) {
               System.out.println("Creating default admin user...");
               User adminUser = new User();
               adminUser.setEmailid(adminEmail);
               adminUser.setFirstName("Admin");
               adminUser.setLastName("User");
               // TODO: You MUST change this default password in production!
               // We will add password hashing (encryption) later.
               adminUser.setPassword("Admin@123"); 
               adminUser.setRole("ADMIN"); // This is the most important part
               adminUser.setRegistrationDate(LocalDateTime.now());
               userRepository.save(adminUser);
               System.out.println("Default admin user created with email: " + adminEmail);
           } else {
               System.out.println("Admin user already exists.");
           }
       };
   }
}


