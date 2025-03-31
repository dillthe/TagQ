package com.greenpastures.tagq.service;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeZoneConverter {
    public static String convertToUserTimeZone(Instant utcTime, String userTimeZone) {
        if (utcTime == null || userTimeZone == null) return null;

        ZoneId zoneId = ZoneId.of(userTimeZone);
        ZonedDateTime userTime = utcTime.atZone(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

        return userTime.format(formatter);
    }
}