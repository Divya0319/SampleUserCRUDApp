package com.fastturtle.simpleUserCrudApp.controllers;

import com.fastturtle.simpleUserCrudApp.models.AppUser;
import com.fastturtle.simpleUserCrudApp.models.UserDTO;
import com.fastturtle.simpleUserCrudApp.services.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create")
    public UserDTO createUser(@RequestBody UserDTO userDTO) {
        AppUser savedUser = userService.createUser(from(userDTO));

        return from(savedUser);
    }

    @GetMapping("/fetchall")
    public List<UserDTO> fetchAllUsers() {
        List<AppUser> users = userService.findAll();

        if(users.isEmpty()) {
            return null;
        }

        List<UserDTO> userDTOS = new ArrayList<>();

        for(AppUser user : users) {
            userDTOS.add(from(user));
        }

        return userDTOS;
    }

    private UserDTO from(AppUser appUser) {
        UserDTO userDTO = new UserDTO();
        userDTO.setName(appUser.getName());
        userDTO.setEmail(appUser.getEmail());

        return userDTO;
    }

    private AppUser from(UserDTO userDTO) {
        AppUser appUser = new AppUser();
        appUser.setName(userDTO.getName());
        appUser.setEmail(userDTO.getEmail());

        return appUser;
    }
}
