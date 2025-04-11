package me.yukitale.yellowexchange.panel.common.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class ErrorMessages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String tradingMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String swapMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String supportMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String transferMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String withdrawMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String withdrawVerificationMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String withdrawAmlMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String cryptoLendingMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String p2pMessage;

    @Column(columnDefinition = "TEXT", length = 1024, nullable = false)
    private String otherMessage;

    @Transient
    private Map<String, String> map;

    public Map<String, String> getAsMap() {
        if (this.map == null) {
            this.map = new LinkedHashMap<>();

            this.map.put("Trading Error", this.tradingMessage);
            this.map.put("Swap Error", this.swapMessage);
            this.map.put("Support Error", this.supportMessage);
            this.map.put("Withdraw Error", this.withdrawMessage);
            this.map.put("Withdraw Verification Error", this.withdrawVerificationMessage);
            this.map.put("Withdraw AML Error", this.withdrawAmlMessage);
            this.map.put("Other Error", this.otherMessage);
        }

        return this.map;
    }

    public static String getErrorType(String type) {
        if (StringUtils.isBlank(type) || type.equals("OTHER")) {
            return "OTHER";
        }

        return type.replace("_", " ");
    }
}
