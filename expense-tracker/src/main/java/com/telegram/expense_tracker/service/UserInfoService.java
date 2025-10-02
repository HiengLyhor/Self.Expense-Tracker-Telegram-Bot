package com.telegram.expense_tracker.service;

import com.telegram.expense_tracker.model.UserInfo;
import com.telegram.expense_tracker.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;

    public void createUser(String username, Long userId) {

        UserInfo newUser = new UserInfo(username, userId);
        userInfoRepository.save(newUser);

    }

    public UserInfo getUserInfoByUsername(String username) {
        return userInfoRepository.findByUsername(username);
    }

    public List<UserInfo> getAllUsers() {
        return userInfoRepository.findAll();
    }

}
