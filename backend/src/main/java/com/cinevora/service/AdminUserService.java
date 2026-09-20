package com.cinevora.service;

import com.cinevora.common.PageResponse;
import com.cinevora.dto.AdminDtos;
import com.cinevora.dto.UserResponse;
import com.cinevora.entity.User;
import com.cinevora.exception.BusinessException;
import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final SessionService sessions;

    public AdminUserService(UserRepository users, SessionService sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String query, String status, int page, int size) {
        if (page < 0 || size < 1 || size > 50) throw new BusinessException("Tham số phân trang không hợp lệ");
        String cleanQuery = query == null || query.isBlank() ? "" : query.trim();
        Boolean active = switch (status == null ? "all" : status.trim().toLowerCase()) {
            case "active" -> true;
            case "inactive", "disabled", "archived" -> false;
            case "", "all" -> null;
            default -> throw new BusinessException("Trạng thái user không được hỗ trợ");
        };
        return PageResponse.from(users.searchAdmin(cleanQuery, active,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))))
                .map(UserResponse::from));
    }

    @Transactional
    public UserResponse setStatus(String actorUsername, Long id, AdminDtos.ActiveRequest request) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user " + id));
        if (user.getUsername().equalsIgnoreCase(actorUsername) && !request.active()) {
            throw new BusinessException("Không thể tự vô hiệu hóa tài khoản admin hiện tại");
        }
        user.setActive(request.active());
        if (!request.active()) sessions.revokeAll(user);
        return UserResponse.from(user);
    }
}
