package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.AdminAuthDto;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import com.bt.fretchain.primary.service.AdminAuthService;
import com.bt.fretchain.security.JwtTokenUtil;
import com.bt.fretchain.utils.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    protected final JwtTokenUtil jwtTokenUtil;

    protected final SystemUserRepository systemUserRepository;

    @Override
    public AdminAuthDto.UserDetails loginAdmin(String userName, String password) {
        SystemUser user = findByUsername(userName);
        validateUser(user, password);
        user.setLastLogin(LocalDateTime.now());
        systemUserRepository.save(user);
        return generateAuthTokenAndGetUserDetails(user);
    }

    public SystemUser findByUsername(String userName) {
        SystemUser user = systemUserRepository.findByEmailAndDeletedFalse(userName).orElseThrow(() -> new ResourceNotFoundException(
                "record.notfound"
        ));
        return user;
    }

    private void validateUser(SystemUser user, String password) {

        if (!TextUtils.matchPassword(password, user.getPassword())) {
            throw new BadRequestException("Invalid Credential");
        }
        // We need to implement Multi Device Login as well.
        if (TextUtils.isEmpty(user.getUniqueKey())) {
            user.setUniqueKey(TextUtils.getUniqueKey());
        }
    }

    private AdminAuthDto.UserDetails generateAuthTokenAndGetUserDetails(SystemUser user) {
        // Replace With Mapper.
        AdminAuthDto.UserDetails adminDetail = new AdminAuthDto.UserDetails();
        adminDetail.setFullName(user.getFullName());
        adminDetail.setUserType(user.getUserType());
//        String token = TextUtils.md5encryption(user.getUniqueKey()) + TextUtils.md5encryption(ipAddress);
        String token = UUID.randomUUID().toString();
        Map<String, Object> bodyPart = new LinkedHashMap<>();
        bodyPart.put("username", user.getUsername());
        bodyPart.put("u-id", user.getUuid());
        bodyPart.put("u-ty", user.getUserType().toString());
        bodyPart.put("roles", List.of(user.getUserType().toString()));
        bodyPart.put("d-id", user.getUuid());
        bodyPart.put("token", token);
        adminDetail.setSecretKey(jwtTokenUtil.generateToken(bodyPart, user));
        return adminDetail;
    }
}
