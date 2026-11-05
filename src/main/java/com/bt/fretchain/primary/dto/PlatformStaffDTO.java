package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.PlatformActivity;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Platform admin staff (Accountant, Order Manager...) and their roles. */
public class PlatformStaffDTO {

    @Getter
    @Setter
    public static class RoleSaveOrUpdate {
        @NotBlank
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        private Set<PlatformActivity> activities = new LinkedHashSet<>();
    }

    @Getter
    @Setter
    public static class RoleDetail {
        private String uuid;
        private String name;
        private String description;
        private List<PlatformActivity> activities;
        private Boolean active;
    }

    @Getter
    @Setter
    public static class ActivityOption {
        private PlatformActivity code;
        private String group;
        private String action;
    }

    @Getter
    @Setter
    public static class StaffSaveOrUpdate {
        @NotBlank
        @Size(max = 150)
        private String fullName;

        @NotBlank
        @Email
        @Size(max = 128)
        private String email;

        @Size(max = 15)
        private String mobile;

        /** Required when creating; blank on update keeps the password. */
        @Size(min = 6, max = 60)
        private String password;

        @NotBlank
        private String roleUuid;
    }

    @Getter
    @Setter
    public static class StaffDetail {
        private String uuid;
        private String fullName;
        private String email;
        private String mobile;
        private String roleUuid;
        private String roleName;
        private Boolean active;
        private LocalDateTime lastLogin;
    }

    @Getter
    @Setter
    public static class ListOrSearchData {
        private Integer pageNumber = 0;
        private Integer pageSize = 10;
        private String search;
        private String roleUuid;
        private SortData sort;
        private FilterData filters;
    }

    @Getter
    @Setter
    public static class SortData {
        private String field;
        private String direction;
    }

    @Getter
    @Setter
    public static class FilterData {
        private Boolean active;
    }
}
