package me.hikaricp.yellowexchange.security.xss.utils;

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

        // Сначала нормализуем строку и убираем нули
        value = ESAPI.encoder()
                .canonicalize(value)
                .replaceAll("\0", "");

        // Очищаем от потенциально опасных HTML, но сохраняем переносы строк
        // Jsoup не удаляет \n, если они не идут внутри HTML-тегов
        value = Jsoup.clean(value, "", Safelist.none(), new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false));

        return value;
    }

    public String makeLinksClickable(String text) {
        if (text == null) return null;

        // Простой regex, который ловит http/https ссылки
        String urlRegex = "(https?://\\S+)";
        return text.replaceAll(urlRegex, "<a href=\"$1\" target=\"_blank\">$1</a>");
    }
}
