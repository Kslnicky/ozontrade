package me.hikaricp.yellowexchange.exchange.controller.api;

import me.hikaricp.yellowexchange.utils.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
public class LocaleApiController {

    private final Map<String, String> localeMessagesJson = new ConcurrentHashMap<>();

    private final Map<String, String> panelLocaleMessagesJson = new ConcurrentHashMap<>();

    @GetMapping(value = "/api/getLocale", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getData(@RequestParam(value = "lang", required = false) String lang) {
        Locale locale = resolveLocale(lang);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(180, TimeUnit.MINUTES).cachePublic().getHeaderValue())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(getAllMessages(locale));
    }

    @GetMapping(value = "/api/getPanelLocale", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPanelData(@RequestParam(value = "lang", required = false) String lang) {
        Locale locale = resolveLocale(lang);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(180, TimeUnit.MINUTES).cachePublic().getHeaderValue())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(getPanelAllMessages(locale));
    }

    public String getAllMessages(Locale locale) {
        String localeKey = locale.toLanguageTag();
        if (this.localeMessagesJson.containsKey(localeKey)) {
            return this.localeMessagesJson.get(localeKey);
        }

        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", locale);
        Map<String, String> messages = new HashMap<>();

        Enumeration<String> keys = resourceBundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String value = resourceBundle.getString(key);
            messages.put(key, value);
        }

        String json = JsonUtil.writeJson(messages);
        this.localeMessagesJson.put(localeKey, json);
        return json;
    }

    public String getPanelAllMessages(Locale locale) {
        String localeKey = locale.toLanguageTag();
        if (this.panelLocaleMessagesJson.containsKey(localeKey)) {
            return this.panelLocaleMessagesJson.get(localeKey);
        }

        ResourceBundle resourceBundle = ResourceBundle.getBundle("panel_messages", locale);
        Map<String, String> messages = new HashMap<>();

        Enumeration<String> keys = resourceBundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String value = resourceBundle.getString(key);
            messages.put(key, value);
        }

        String json = JsonUtil.writeJson(messages);
        this.panelLocaleMessagesJson.put(localeKey, json);
        return json;
    }

    private Locale resolveLocale(String lang) {
        if (StringUtils.isBlank(lang)) {
            return Locale.ENGLISH;
        }
        Locale locale = Locale.forLanguageTag(lang);
        return (locale == null) ? Locale.ENGLISH : locale;
    }
}
