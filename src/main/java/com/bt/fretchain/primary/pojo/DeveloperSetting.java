package com.bt.fretchain.primary.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "developer_settings")
@Getter
@Setter
public class DeveloperSetting extends BaseEntity {

    @Column(name = "customer_android_min_version", length = 20)
    private String customerAndroidMinVersion;

    @Column(name = "customer_ios_min_version", length = 20)
    private String customerIosMinVersion;

    @Column(name = "driver_android_min_version", length = 20)
    private String driverAndroidMinVersion;

    @Column(name = "driver_ios_min_version", length = 20)
    private String driverIosMinVersion;

    @Column(name = "force_update_enabled")
    private Boolean forceUpdateEnabled = false;

    @Column(name = "test_mode")
    private Boolean testMode = false;

    @Column(name = "test_otp", length = 10)
    private String testOtp = "123456";

}