package com.nguyenduyhung.bookstore.user;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> AppException.notFound("User", id));
    }
}
