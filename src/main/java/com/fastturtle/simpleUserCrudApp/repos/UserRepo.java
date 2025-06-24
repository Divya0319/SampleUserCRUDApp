package com.fastturtle.simpleUserCrudApp.repos;

import com.fastturtle.simpleUserCrudApp.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<AppUser, Integer> {

    List<AppUser> findAll();
}
