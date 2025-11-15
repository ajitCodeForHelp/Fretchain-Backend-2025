package com.bt.fretchain.messaging;

import com.bt.fretchain.utils.Const;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Component
public class SmsComponent {
    private static final Logger logger = LogManager.getLogger(SmsComponent.class);
    private static final String USER_AGENT = "Mozilla/5.0";

    public void sendSMSByMakeMySms(String mobileNumber, String message,String templateId) {
        if (Const.SystemSetting.TestMode) {
            return;
        }
        logger.info("*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
        logger.info("SmsComponent.sendSMS : mobileNumber : " + mobileNumber + " >> message : " + message);
        try {
            String url = "https://mdssend.in/api.php?" +
                    "username=SAWARIYA&apikey=QgoE8YPYnUDO" +
                    "&senderid=TEBOOK" +
                    "&route=TRANS" +
                    "&mobile="+mobileNumber+
                    "&text=" + URLEncoder.encode(message, "UTF-8") +
                    "&TID=" + templateId +
                    "&PEID=1701173635615266378";
            logger.info(url);
            URL otpUrl = new URL(url);
            HttpURLConnection con = (HttpURLConnection) otpUrl.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = con.getResponseCode();
            logger.info("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // print result
                logger.info(response.toString());
            } else {
                logger.info("GET request not worked");
            }
            logger.info("*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
