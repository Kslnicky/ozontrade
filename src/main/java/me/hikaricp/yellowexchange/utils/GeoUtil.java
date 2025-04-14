package me.hikaricp.yellowexchange.utils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.InetAddress;

@UtilityClass
public class GeoUtil {

    private DatabaseReader databaseReader;

    static {
        try {
            databaseReader = new DatabaseReader.Builder(GeoUtil.class.getResourceAsStream("/GeoLite2-City.mmdb")).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GeoData getGeo(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse cityResponse = databaseReader.city(ipAddress);
            if (cityResponse == null) {
                return new GeoData();
            }
            return new GeoData(cityResponse.getCountry().getIsoCode(), cityResponse.getCountry().getName(), cityResponse.getCity().getName());
        } catch (Exception ex) {
            return new GeoData();
        }
    }

    @Embeddable
    @AllArgsConstructor
    @Getter
    public static class GeoData {

        private final String countryCode;
        private final String countryName;
        private final String cityName;

        public GeoData() {
            this.countryCode = "N/A";
            this.countryName = null;
            this.cityName = null;
        }

        @Override
        public String toString() {
            return this.countryName == null ? "N/A" : this.countryCode + ", " + this.countryName + ", " + this.cityName;
        }
    }
}
