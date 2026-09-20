package com.cinevora.service;

import com.cinevora.dto.AccountDtos;
import com.cinevora.dto.UserResponse;
import com.cinevora.entity.User;
import com.cinevora.exception.BusinessException;
import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AccountService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SessionService sessions;

    public AccountService(UserRepository users, PasswordEncoder encoder, SessionService sessions) { this.users = users; this.encoder = encoder; this.sessions = sessions; }
    @Transactional(readOnly = true) public UserResponse get(String username) { return UserResponse.from(current(username)); }
    @Transactional public UserResponse update(String username, AccountDtos.UpdateProfileRequest request) {
        User user = current(username); String email = request.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(user.getEmail()) && users.existsByEmailIgnoreCase(email)) throw new BusinessException("Email đã được sử dụng");
        user.setFullName(request.fullName().trim());
        if (!email.equalsIgnoreCase(user.getEmail())) { user.setEmail(email); user.setEmailVerified(false); }
        return UserResponse.from(user);
    }
    @Transactional public void changePassword(String username, AccountDtos.ChangePasswordRequest request) {
        User user = current(username);
        if (!encoder.matches(request.currentPassword(), user.getPassword())) throw new BusinessException("Mật khẩu hiện tại không đúng");
        if (encoder.matches(request.newPassword(), user.getPassword())) throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại");
        user.setPassword(encoder.encode(request.newPassword())); sessions.revokeAll(user);
    }
    @Transactional(readOnly = true) public List<AccountDtos.SessionResponse> sessions(String username) { return this.sessions.list(current(username)); }
    @Transactional public void revokeSession(String username, Long id) { this.sessions.revoke(current(username), id); }
    private User current(String username) { return users.findByUsernameIgnoreCase(username).filter(User::isActive).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản hiện tại")); }
}
