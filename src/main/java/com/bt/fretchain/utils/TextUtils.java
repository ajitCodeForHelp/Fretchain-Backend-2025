package com.bt.fretchain.utils;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TextUtils {
    public static String generate4DigitOTP() {
        if (Const.SystemSetting.TestMode) {
            return "9999";
        }
        Random rnd = new Random();
        int number = rnd.nextInt(9999);
        return String.format("%04d", number);
    }

    public static boolean isEmpty(String string) {
        if (null == string) return true;
        return string.length() == 0;
    }

    public static boolean isEmpty(Long value) {
        if (null == value) return true;
        return value <= 0;
    }

    public static boolean isEmpty(Integer value) {
        if (null == value) return true;
        return value <= 0;
    }

    public static boolean isEmpty(Double value) {
        if (null == value) return true;
        return value <= 0;
    }

    public static boolean isEmpty(List list) {
        return (list == null || list.isEmpty());
    }

    public static Long getValidValue(Long oldValue, Long newValue) {
        return TextUtils.isEmpty(newValue) ? oldValue : newValue;
    }

    public static Double getValidValue(Double oldValue, Double newValue) {
        return TextUtils.isEmpty(newValue) ? oldValue : newValue;
    }

    public static String getValidValue(String oldValue, String newValue) {
        return TextUtils.isEmpty(newValue) ? oldValue : newValue;
    }

    public static LocalDate getValidValue(Long newValue, LocalDate oldValue) {
        if (TextUtils.isEmpty(newValue)) {
            return oldValue;
        }
        return DateUtils.getLocalDate(newValue);
    }

    public static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Long currentSessionId = 0L;

    public static void resetCurrentSessionId() {
        currentSessionId = 0L;
    }

    public static String formatMoneyAmount(double moneyAmount) {
        DecimalFormat format = new DecimalFormat("##.00");
        return format.format(moneyAmount);
    }

    public static String getRandomKeyString(int length) {
        // String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ-abcdefghijklmnopqrstuvwxyz_0123456789";
        String alphaNumeric = "ABCDEFGHJKMNOPQRSTUVWXYZ";
        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int random = (int) ((Math.random() * 1000) % alphaNumeric.length());
            buffer.append(alphaNumeric.charAt(random));
        }
        String generatedString = buffer.toString();
        return generatedString;
    }

    public static String getEncodedPassword(String password) {
        if (isEmpty(password)) {
            return null;
        }
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    public static boolean matchPassword(String rawPassword, String encodedPassword) {
        return new BCryptPasswordEncoder().matches(rawPassword, encodedPassword);
    }

    public static String md5encryption(String text) {
        if (isEmpty(text)) {
            return null;
        }
        String hashtext = null;
        try {
            String plaintext = text;
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(plaintext.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            hashtext = bigInt.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
        } catch (Exception e1) {
        }
        return hashtext;
    }

    public static String get10CharRandomCode() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();
        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return generatedString.toUpperCase();
    }

    public static String getOrderReferenceId(Long orderId) {
        return "ORD-" + TextUtils.convertNumberTo10Digit(orderId);
    }

    public static String convertNumberTo10Digit(Long number) {
        return String.format("%010d", number);
    }

    public static String get10DigitTransactionId(Long number) {
        return "TXN-" + convertNumberTo10Digit(number);
    }

    public static String getPaymentRequestTransactionId(Long number) {
        if (Const.SystemSetting.TestMode)
            return "TEST-TXN-" + convertNumberTo10Digit(number);
        else {
            return "TXNL-" + convertNumberTo10Digit(number);
        }
    }

    public static String getListToStringCSV(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        StringBuffer csv = new StringBuffer();
        for (String image : list) {
            if (csv.length() > 0) {
                csv.append(",");
            }
            csv.append(image);
        }
        return csv.toString();
    }

    public static List<String> getStringCSVToList(String stringCSV) {
        if (isEmpty(stringCSV)) {
            return null;
        }
        return new ArrayList<>(Arrays.asList(stringCSV.split(",")));
    }

    public static String removeSpecialCharacterFromString(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", " ");
    }

    public static String getUniqueKey() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    public static String getJsonString(Object obj) {
        return new Gson().toJson(obj);
    }

    public static String generate6DigitRandomNumber() {
        if (true) {
            return "000000";
        }
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }


    public static String generateReferralCode(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        String cleanUsername = username.trim();
        String alphaPart = cleanUsername.replaceAll("[^A-Za-z]", "");
        if (alphaPart.length() >= 2) {
            alphaPart = alphaPart.substring(0, 2).toUpperCase();
        } else if (alphaPart.length() == 1) {
            alphaPart = (alphaPart + "X").toUpperCase(); // pad if only 1 letter
        } else {
            alphaPart = "XX"; // no letters at all
        }
        String numberPart = cleanUsername.replaceAll("[^0-9]", "");
        if (numberPart.isEmpty()) {
            numberPart = String.valueOf((int) (Math.random() * 9000) + 1000);
        }
        return alphaPart + numberPart;
    }
    public static BigDecimal get2DecimalPlaceValue(double value) {
        return new BigDecimal(String.format("%.2f", value));
    }

    public static String getCombinedIdsByDash(List<Long> ids) {
        // Sort If Required.
        ArrayList<Long> idList = new ArrayList<>(ids);
        Collections.sort(idList);
        StringBuffer combinedKey = new StringBuffer("");
        for (Long id : idList) {
            if (combinedKey.length() > 0) {
                combinedKey.append("-");
            }
            combinedKey.append(id);
        }
        return combinedKey.toString();
    }

    public static String validateTitle(String title) {
        if (isEmpty(title)) {
            return null;
        }
        return title.toLowerCase().replaceAll("\\s+", "_");
    }
}