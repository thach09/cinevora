package com.cinevora.service;

import com.cinevora.dto.ProfileDtos;
import com.cinevora.entity.Profile;
import com.cinevora.entity.User;
import com.cinevora.exception.BusinessException;
import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.ProfileRepository;
import com.cinevora.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProfileService {
    private final ProfileRepository profiles;
    private final UserRepository users;
    public ProfileService(ProfileRepository profiles, UserRepository users) { this.profiles = profiles; this.users = users; }

    @Transactional(readOnly = true) public List<ProfileDtos.Response> list(String username) { return profiles.findByUser_IdOrderByCreatedAtAsc(current(username).getId()).stream().map(ProfileDtos.Response::from).toList(); }
    @Transactional public ProfileDtos.Response create(String username, ProfileDtos.Request request) {
        User user = current(username);
        if (profiles.countByUser_Id(user.getId()) >= 5) throw new BusinessException("Mỗi tài khoản chỉ có tối đa 5 profile");
        if (profiles.findByUser_IdOrderByCreatedAtAsc(user.getId()).stream().anyMatch(p -> p.getName().equalsIgnoreCase(request.name().trim()))) throw new BusinessException("Tên profile đã tồn tại");
        Profile profile = new Profile(user, request.name().trim(), false); profile.setAvatarUrl(clean(request.avatarUrl()));
        return ProfileDtos.Response.from(profiles.save(profile));
    }
    @Transactional public ProfileDtos.Response update(String username, Long id, ProfileDtos.Request request) {
        Profile profile = owned(username, id); profile.setName(request.name().trim()); profile.setAvatarUrl(clean(request.avatarUrl())); return ProfileDtos.Response.from(profile);
    }
    @Transactional public void delete(String username, Long id) {
        Profile profile = owned(username, id);
        if (profile.isDefaultProfile()) throw new BusinessException("Không thể xóa profile mặc định");
        profiles.delete(profile);
    }
    @Transactional(readOnly = true) public Profile resolve(String username, Long requestedId) {
        User user = current(username);
        return requestedId == null ? profiles.findByUser_IdAndDefaultProfileTrue(user.getId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy profile mặc định")) : profiles.findByIdAndUser_Id(requestedId, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Profile không thuộc tài khoản hiện tại"));
    }
    @Transactional(readOnly = true) public Profile defaultProfile(String username) { return resolve(username, null); }
    private Profile owned(String username, Long id) { return profiles.findByIdAndUser_Id(id, current(username).getId()).orElseThrow(() -> new ResourceNotFoundException("Profile không thuộc tài khoản hiện tại")); }
    private User current(String username) { return users.findByUsernameIgnoreCase(username).filter(User::isActive).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản hiện tại")); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
