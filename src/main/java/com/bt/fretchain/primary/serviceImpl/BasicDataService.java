package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.enums.DeviceTypeEnum;
import com.bt.fretchain.primary.pojo.enums.GenderTypeEnum;
import com.bt.fretchain.primary.pojo.enums.UserTypeEnum;
import com.bt.fretchain.utils.TextUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Component
@Service("basicDataService")
@RequiredArgsConstructor
public class BasicDataService  {

    protected final EntityManagerFactory entityManagerFactory;
    public void crateBasicData() {

        SystemUser admin = new SystemUser();
        admin.setFullName("Admin");
        admin.setPhotoImageUrl("https://cdn1.iconfinder.com/data/icons/rcons-user-action/512/user-512.png");
        admin.setMobile("82019165015");
        admin.setMobileVerified(true);
        admin.setEmail("ajitrathore@admin.com");
        admin.setPassword(TextUtils.getEncodedPassword("12"));
        admin.setPwdText("12");
        admin.setEmailVerified(true);
        admin.setUniqueKey(UUID.randomUUID().toString());
        admin.setUserType(UserTypeEnum.ADMIN);
        admin.setGender(GenderTypeEnum.MALE);
        admin.setDeviceType(DeviceTypeEnum.WEB);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.persist(admin);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transaction.rollback();
        } finally {
            entityManager.close();
        }
    }
}