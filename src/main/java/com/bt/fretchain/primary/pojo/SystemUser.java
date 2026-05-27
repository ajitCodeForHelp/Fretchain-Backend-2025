package com.bt.fretchain.primary.pojo;

import com.bt.fretchain.primary.pojo.enums.DeviceTypeEnum;
import com.bt.fretchain.primary.pojo.enums.GenderTypeEnum;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.utils.TextUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "system_user")
@Setter
@Getter
public class SystemUser  extends BaseEntity implements UserDetails {

    @Column(name = "full_name", length = 500)
    private String fullName;

    @Column(name = "email", unique = true, length = 128)
    private String email;

    @Column(name = "mobile", unique = true, length = 15)
    private String mobile;

    @Basic(optional = false)
    @Column(name = "mobile_verified")
    private boolean mobileVerified = false;

    @Basic(optional = false)
    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "password", length = 128)
    private String password;

    @Column(name = "pwd_text", length = 60)
    private String pwdText;

    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    private UserTypeEnum userType;

    @Column(name = "device_type")
    @Enumerated(EnumType.STRING)
    private DeviceTypeEnum deviceType;

    @Column(name = "fcm_device_token")
    private String fcmDeviceToken;

    @Column(name = "unique_key", unique = true)
    private String uniqueKey;

    @Column(name = "photo_image_url")
    private String photoImageUrl;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private GenderTypeEnum gender;

    @Column(name = "last_login_at")
    protected LocalDateTime lastLogin;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + userType)
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return isActive();
    }

    @Override
    public String getUsername() {
        if (!TextUtils.isEmpty(fullName)) {
            return fullName;
        } else if (!TextUtils.isEmpty(email)) {
            return email;
        } else if (!TextUtils.isEmpty(mobile)) {
            return mobile;
        }
        return null;
    }
}