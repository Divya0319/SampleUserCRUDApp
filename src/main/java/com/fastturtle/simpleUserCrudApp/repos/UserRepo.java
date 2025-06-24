package com.fastturtle.simpleUserCrudApp.repos;

import com.fastturtle.simpleUserCrudApp.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<AppUser, Integer> {
}
