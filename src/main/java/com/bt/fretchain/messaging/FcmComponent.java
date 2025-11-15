package com.bt.cloudLibrary.messaging;

import com.bt.cloudLibrary.utils.TextUtils;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.gson.Gson;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class FcmComponent {

    public void sendNotificationToUser(String deviceType, String deviceToken,
                                       FcmNotificationBean.Notification notification, FcmNotificationBean.Data data) {
        new Thread(() -> {
            if (!TextUtils.isEmpty(deviceToken)) {
                pushFCMNotification(deviceType, deviceToken, notification ,data);
            }
        }).start();
    }

    private String getAccessToken() throws IOException {
        InputStream in = new ClassPathResource("service-account.json").getInputStream();
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(in);
        credentials = credentials.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
        AccessToken accessToken = credentials.refreshAccessToken();
        return accessToken.getTokenValue();
    }

    private boolean pushFCMNotification(String deviceType, String deviceToken,FcmNotificationBean.Notification notification, FcmNotificationBean.Data data) {
        try {
            String FMCurl = "https://fcm.googleapis.com/v1/projects/thebooks24-84fe6/messages:send";
            URL url = new URL(FMCurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
            conn.setRequestProperty("Content-Type", "application/json");

            Map<String, String> dataToSent = new HashMap<>();
            dataToSent.put("title", data.getTitle());
            dataToSent.put("message", data.getMessage());
            if (data.getType() != null) {
                dataToSent.put("type", data.getType());
            }
            if (data.getTimeStamp() != null) {
                dataToSent.put("timeStamp", data.getTimeStamp());
            }
            if (data.getImageUrl() != null) {
                dataToSent.put("imageUrl", data.getImageUrl());
            }
            if (data.getStatus() != null) {
                dataToSent.put("status", data.getStatus());
            }
            if (data.getOrderId() != null) {
                dataToSent.put("orderId", data.getOrderId());
            }
            Notification notificationToSent = Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .build();

            Message message = null;
            if (deviceType != null) {
                if (deviceType.equalsIgnoreCase("android"))
                    message = Message.builder()
                            .setNotification(notificationToSent)
                            .setToken(deviceToken).build();
                else
                    message = Message.builder()
                            .setNotification(notificationToSent)
                            .putAllData(dataToSent)
                            .setToken(deviceToken).build();
            }

            FcmNotificationBean fcmNotificationBean = new FcmNotificationBean();
            fcmNotificationBean.setMessage(message);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(new Gson().toJson(fcmNotificationBean)
            );
            wr.flush();
            conn.getInputStream();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}