package me.yukitale.yellowexchange.utils;

import de.taimos.totp.TOTP;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

@UtilityClass
public class GoogleUtil {

    public String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }
}
