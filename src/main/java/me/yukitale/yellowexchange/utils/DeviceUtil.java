package me.yukitale.yellowexchange.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DeviceUtil {

    public DeviceType getDeviceType(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("android") || ua.contains("iphone") || ua.contains("ipad") || ua.contains("mobile")) {
            return DeviceType.MOBILE;
        } else {
            return DeviceType.PC;
        }
    }

    @AllArgsConstructor
    @Getter
    public enum DeviceType {

        PC("PC"),
        MOBILE("Mobile");

        private final String title;
    }
}
