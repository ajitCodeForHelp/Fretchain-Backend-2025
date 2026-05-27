package com.bt.fretchain.security;

import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    protected SystemUserRepository systemUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("JwtUserDetailsService.loadUserByUsername");
        return null;
    }

    public UserDetails loadUserByUsername(String userId, String userType, String uniqueKey) throws UsernameNotFoundException {
        // we use the user record id as user-name across the board
        User user = null;
        SystemUser systemUser = getAppUser(userId, userType, uniqueKey);
        if (systemUser != null) {
            user = new User(systemUser.getUsername(), "", new ArrayList<>());
        }
        return user;
    }

    public SystemUser getLoggedInUser() throws UsernameNotFoundException {
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String username = jwtAuthenticationToken.getTokenAttributes().get("username").toString();
        String userId = jwtAuthenticationToken.getTokenAttributes().get("u-id").toString();
        String userType = jwtAuthenticationToken.getTokenAttributes().get("u-ty").toString();
        String deviceId = jwtAuthenticationToken.getTokenAttributes().get("d-id").toString();
        String userToken = jwtAuthenticationToken.getTokenAttributes().get("token").toString();
        return getAppUser(userId, userType, userToken);
    }

    private SystemUser getAppUser(String userId, String userType, String uniqueKey) throws UsernameNotFoundException {
        SystemUser user = systemUserRepository
                .findByUuid(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Unauthorized Access"));
        return user;
    }
}