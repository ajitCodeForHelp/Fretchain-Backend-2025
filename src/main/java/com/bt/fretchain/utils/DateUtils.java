package com.bt.fretchain.utils;

import java.time.*;
import java.util.Date;

public class DateUtils {

    // ================================================================================================================
    public static Long getTimeStamp(Date date) {
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

    public static Long getTimeStamp(LocalDate localDate) {
        return getDate(localDate).getTime();
    }

    public static Long getTimeStamp(LocalTime localTime) {
        return getDate(localTime).getTime();
    }

    public static Long getTimeStamp(LocalDateTime localDateTime) {
        return getDate(localDateTime).getTime();
    }

    // ================================================================================================================
    public static Date getDate(Long timeStamp) {
        return new Date(timeStamp);
    }

    public static Date getDate(LocalDate localDate) {
        return new Date(localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000);
    }

    public static Date getDate(LocalTime localTime) {
        Instant timeOnEpochDayInDefaultTimeZone = LocalDate.ofEpochDay(0)
                .atTime(localTime)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        return Date.from(timeOnEpochDayInDefaultTimeZone);
    }

    public static Date getDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // ================================================================================================================
    public static LocalDate getLocalDate(Long timeStamp) {
        if (TextUtils.isEmpty(timeStamp)) {
            return LocalDate.now();
        }
        return Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDate getLocalDate(Date date) {
        if (date == null) {
            return LocalDate.now();
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
    // LocalTime  // LocalDateTime

    // ================================================================================================================
    public static LocalTime getLocalTime(Long timeStamp) {
        if (TextUtils.isEmpty(timeStamp)) {
            return LocalTime.now();
        }
        return LocalDateTime.ofInstant(new Date(timeStamp).toInstant(), ZoneId.systemDefault()).toLocalTime();
    }

    public static LocalTime getLocalTime(Date date) {
        if (date == null) {
            return LocalTime.now();
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalTime();
    }
    // LocalDate  // LocalDateTime

    // ================================================================================================================
    public static LocalDateTime getLocalDateTime(Long timeStamp) {
        if (TextUtils.isEmpty(timeStamp)) {
            return LocalDateTime.now();
        }
        return Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDateTime getLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    // LocalDate // LocalTime

    // ================================================================================================================

    public static String getFormattedDate(Long timeStamp) {
        return getFormattedDate(getLocalDateTime(timeStamp));
    }

    public static String getFormattedDate(Date date) {
        return getFormattedDate(getLocalDateTime(date));
    }

    public static String getFormattedDate(LocalDateTime localDateTime) {
        return getFormattedDate(localDateTime.getDayOfMonth(), localDateTime.getMonthValue(), localDateTime.getYear());
    }

    private static String getFormattedDate(int day, int month, int year) {
        return (day < 10 ? "0" : "") + day + "/"
                + ((month) < 10 ? "0" : "") + (month) + "/"
                + year;
    }

    // ================================================================================================================

    public static String getFormattedTime(Long timeStamp) {
        return getFormattedTime(getLocalTime(timeStamp));
    }

    public static String getFormattedTime(Date date) {
        return getFormattedTime(getLocalTime(date));
    }

    public static String getFormattedTime(LocalDateTime localDateTime) {
        return getFormattedTime(localDateTime.getHour(), localDateTime.getMinute());
    }

    public static String getFormattedTime(LocalTime localTime) {
        return getFormattedTime(localTime.getHour(), localTime.getMinute());
    }

    private static String getFormattedTime(int hours, int minute) {
        int mod = (hours % 12);
        return (mod < 10 && mod != 0 ? "0" : "") + ((mod == 0) ? "12" : mod) + ":"
                + (minute < 10 ? "0" : "") + minute
                + " " + (((hours / 12) > 0) ? "PM" : "AM");
    }

    // ================================================================================================================

    public static String getFormattedDateTime(Long timeStamp) {
        return getFormattedDateTime(getLocalDateTime(timeStamp));
    }

    public static String getFormattedDateTime(Date date) {
        return getFormattedDateTime(getLocalDateTime(date));
    }

    // public static String getFormattedDateTime(LocalTime localTime) {
    //     LocalDateTime localDateTime = LocalDateTime.now();
    //     return getFormattedDate(localDateTime.getDayOfMonth(), localDateTime.getMonthValue() - 1, localDateTime.getYear()) + " " +
    //             getFormattedTime(localTime.getHour(), localTime.getMinute());
    // }

    public static String getFormattedDateTime(LocalDateTime localDateTime) {
        return getFormattedDate(localDateTime.getDayOfMonth(), localDateTime.getMonthValue(), localDateTime.getYear()) + " " +
                getFormattedTime(localDateTime.getHour(), localDateTime.getMinute());
    }
}