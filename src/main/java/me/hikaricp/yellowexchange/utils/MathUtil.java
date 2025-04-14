package me.hikaricp.yellowexchange.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtil {

    public double round(double number, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(number * factor) / factor;
    }
}
