package com.matpatielectricals.esinventoryanalytics.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matpatielectricals.esinventoryanalytics.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User,String>
{
	User findByEmailid(String emailid);
}




