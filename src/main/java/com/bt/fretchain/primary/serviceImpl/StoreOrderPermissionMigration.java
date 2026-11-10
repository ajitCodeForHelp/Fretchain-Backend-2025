package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.pojo.RestaurantRole;
import com.bt.fretchain.primary.pojo.ShopRole;
import com.bt.fretchain.primary.pojo.enums.RestaurantActivity;
import com.bt.fretchain.primary.pojo.enums.ShopActivity;
import com.bt.fretchain.primary.repository.RestaurantRoleRepository;
import com.bt.fretchain.primary.repository.ShopRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Roles saved before shop / restaurant orders existed have no ORDER permissions, so their staff would not see
 * the Orders section. At startup, every role that already has full access (all other activities, e.g. the default
 * "Shop Admin" / "Restaurant Admin") also gets ORDER_VIEW and ORDER_STATUS. Roles an owner trimmed on purpose are left alone.
 * Safe to run on every start: it only adds what is missing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreOrderPermissionMigration implements ApplicationRunner {

    private final Migrator migrator;

    @Override
    public void run(ApplicationArguments args) {
        try {
            migrator.migrate();
        } catch (Exception e) {
            // never block the application from starting
            log.warn("Could not add order permissions to existing store roles: {}", e.getMessage());
        }
    }

    @Component
    @RequiredArgsConstructor
    static class Migrator {

        private final ShopRoleRepository shopRoles;
        private final RestaurantRoleRepository restaurantRoles;

        @Transactional
        public void migrate() {
            Set<ShopActivity> shopOthers = Arrays.stream(ShopActivity.values())
                    .filter(a -> !a.isLegacy() && !a.getGroup().equals("ORDER")).collect(Collectors.toSet());
            Set<RestaurantActivity> restOthers = Arrays.stream(RestaurantActivity.values())
                    .filter(a -> !a.isLegacy() && !a.getGroup().equals("ORDER")).collect(Collectors.toSet());

            int shops = 0;
            for (ShopRole r : (List<ShopRole>) shopRoles.findAll()) {
                if (!r.isDeleted() && !r.getActivities().contains(ShopActivity.ORDER_VIEW) && r.getActivities().containsAll(shopOthers)) {
                    r.getActivities().add(ShopActivity.ORDER_VIEW);
                    r.getActivities().add(ShopActivity.ORDER_STATUS);
                    shopRoles.save(r);
                    shops++;
                }
            }
            int restaurants = 0;
            for (RestaurantRole r : (List<RestaurantRole>) restaurantRoles.findAll()) {
                if (!r.isDeleted() && !r.getActivities().contains(RestaurantActivity.ORDER_VIEW) && r.getActivities().containsAll(restOthers)) {
                    r.getActivities().add(RestaurantActivity.ORDER_VIEW);
                    r.getActivities().add(RestaurantActivity.ORDER_STATUS);
                    restaurantRoles.save(r);
                    restaurants++;
                }
            }
            if (shops + restaurants > 0) {
                log.info("Added order permissions to {} shop role(s) and {} restaurant role(s)", shops, restaurants);
            }
        }
    }
}
