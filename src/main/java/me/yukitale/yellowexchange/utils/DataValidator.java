package me.yukitale.yellowexchange.utils;

import lombok.experimental.UtilityClass;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Pattern;

@UtilityClass
public class DataValidator {

    private final Pattern DOMAIN_REGEX = Pattern.compile("^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private final Pattern EMAIL_PATTERN = Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d_!@#$%^&*(),.?\":{}|<>]{8,30}$", Pattern.CASE_INSENSITIVE);
    private final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9-_.]{6,32}$", Pattern.CASE_INSENSITIVE);

    private final Pattern PROMOCODE_PATTERN = Pattern.compile("^[a-z0-9_]{4,16}$", Pattern.CASE_INSENSITIVE);

    private final Pattern REF_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{8}$", Pattern.CASE_INSENSITIVE);

    private final Pattern ANTIPHISHING_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,16}$", Pattern.CASE_INSENSITIVE);

    private final Pattern BIRTH_DATE_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$");

    private final char[] ALLOWED_SYMBOLS = new char[] { '"', '-', '_', '.', '!', '$', '(', ')', ',', '+',
            '=', '&', '*', '@', '#', ';', ':', '%', '/', '\\' };

    private final char[] NAME_NOT_ALLOWED_SYMBOLS = new char[] { '"', '_', '!', '$', ',', '+',
            '=', '&', '*', '@', '#', ';', ':', '%', '/', '\\' };

    private final Tika TIKA = new Tika();

    public boolean isNameNotAllowedSymbols(String name) {
        for (char nameNotAllowedSymbol : NAME_NOT_ALLOWED_SYMBOLS) {
            if (name.contains(String.valueOf(nameNotAllowedSymbol))) {
                return true;
            }
        }

        return false;
    }

    public boolean isBirthDateValided(String birthDate) {
        return BIRTH_DATE_PATTERN.matcher(birthDate).matches();
    }

    public boolean isRefCodeValided(String refCode) {
        return REF_CODE_PATTERN.matcher(refCode).matches();
    }

    private boolean isAllowedSymbol(char c) {
        for (char allowedSymbol : ALLOWED_SYMBOLS) {
            if (c == allowedSymbol) {
                return true;
            }
        }
        return false;
    }

    public boolean isTextValided(String text) {
        return text.chars().noneMatch(c -> !Character.isLetter(c) && !Character.isDigit(c) && c != ' ' && !isAllowedSymbol((char) c));
    }

    public boolean isTextValidedWithoutSymbols(String text) {
        return text.chars().noneMatch(c -> !Character.isLetter(c) && !Character.isDigit(c));
    }

    public boolean isAddressValided(String text) {
        return text.chars().noneMatch(c -> !Character.isLetter(c) && !Character.isDigit(c) && c != '_' && c != '-');
    }

    public boolean isTextValidedLowest(String text) {
        return text.chars().noneMatch(c -> !Character.isLetter(c) && !Character.isDigit(c) && c != ' ' && c != '.' && c != ',' && c != '(' && c != ')' && c != '|' && c != '[' && c != ']' && c != '&' && c != '#' && c != '!');
    }

    public boolean isDomainValided(String domain) {
        return DOMAIN_REGEX.matcher(domain).matches();
    }

    public boolean isEmailValided(String email) {
        return EMAIL_PATTERN.matcher(email.toLowerCase()).matches();
    }

    public boolean isUsernameValided(String username) {
        return USERNAME_PATTERN.matcher(username.toLowerCase()).matches();
    }

    public boolean isPasswordValided(String password) {
        return true;
    }

    public boolean isPromocodeValided(String promocode) {
        return PROMOCODE_PATTERN.matcher(promocode).matches();
    }

    public boolean isAntiphishingCodeValided(String antiphishingCode) {
        return ANTIPHISHING_CODE_PATTERN.matcher(antiphishingCode).matches();
    }

    public boolean isValidImage(MultipartFile file) {
        if (!FileUploadUtil.isAllowedContentType(file)) {
            return false;
        }

        String detectedType = null;
        try {
            detectedType = TIKA.detect(file.getInputStream());
            return detectedType != null && detectedType.startsWith("image/");
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isOnlyLetters(String str) {
        if (str == null) {
            return false;
        }

        return str.matches("\\p{L}+");
    }
}
