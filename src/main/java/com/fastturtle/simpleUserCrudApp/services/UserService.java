package com.fastturtle.simpleUserCrudApp.services;

import com.fastturtle.simpleUserCrudApp.models.AppUser;
import com.fastturtle.simpleUserCrudApp.repos.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public AppUser createUser(AppUser appUser) {

        return userRepo.save(appUser);
    }

    public List<AppUser> findAll() {
        return userRepo.findAll();
    }
}
