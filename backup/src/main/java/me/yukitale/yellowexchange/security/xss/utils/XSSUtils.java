package me.yukitale.yellowexchange.security.xss.utils;

import lombok.experimental.UtilityClass;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.owasp.esapi.ESAPI;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

@UtilityClass
public class XSSUtils {

    //todo: maybe this sanitize
    //todo: в password sanitize + stripXss

    public String sanitize(String input) {
        PolicyFactory policyFactory = new HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                .allowStyling()
                .allowCommonBlockElements()
                .allowCommonInlineFormattingElements()
                .allowAttributes("style").globally()
                .allowElements("a")
                .allowAttributes("href").onElements("a")
                .allowAttributes("class").onElements("a")
                .toFactory();
        return policyFactory.sanitize(input);
    }

    public String stripXSS(String value) {
        if (value == null) {
            return null;
        }

        //todo: пофиксить костыль
        String newlinePlaceholder = "[NEW_LINE]";

        value = value.replace("\\n", newlinePlaceholder);

        value = ESAPI.encoder()
                .canonicalize(value)
                .replaceAll("\0", "");

        value = Jsoup.clean(value, Safelist.none());

        return value.replace(newlinePlaceholder, "\\n");
    }
}
