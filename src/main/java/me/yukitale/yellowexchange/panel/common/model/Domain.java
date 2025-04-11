package me.yukitale.yellowexchange.panel.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import me.yukitale.yellowexchange.utils.MyDecimal;
import me.yukitale.yellowexchange.utils.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.*;

@Entity
@Table(name = "domains")
@NoArgsConstructor
@Getter
@Setter
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long usersCount;

    private long depositsCount;

    private double depositsPrice;

    @Size(min = 4, max = 128)
    @Column(unique = true, nullable = false)
    private String name;

    @Size(min = 1, max = 64)
    private String exchangeName;

    @Size(max = 128)
    private String icon;

    @Column(columnDefinition = "VARCHAR(128) DEFAULT ''")
    private String note;

    @Column(columnDefinition = "VARCHAR(128) DEFAULT ''")
    private String title;

    @Column(columnDefinition = "BIGINT DEFAULT -1")
    private long fbpixel;

    @Size(max = 512)
    @Column(columnDefinition = "TEXT", length = 512)
    private String description;

    @Size(max = 512)
    @Column(columnDefinition = "TEXT", length = 512)
    private String keywords;

    @Size(max = 512)
    @Column(columnDefinition = "TEXT", length = 512)
    private String robotsTxt;

    private boolean promoEnabled;

    private boolean buyCryptoEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean promoPopupEnabled;

    private boolean verif2Enabled;

    @Column(columnDefinition = "DOUBLE DEFAULT 10000")
    private double verif2Balance;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailRequiredEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean signupPromoEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean signupRefEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fiatWithdrawEnabled;

    @Column(columnDefinition = "INT DEFAULT 0")
    private int homeDesign;

    @Size(max = 64)
    private String server;

    private int port;

    @Size(max = 64)
    private String email;

    @Size(max = 64)
    private String password;

    private String facebook;

    private String x;

    private String instagram;

    private String youtube;

    private String linkedin;

    private String telegram;

    private String tiktok;

    private String reddit;

    private String discord;

    private String medium;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    public HomePageDesign getHomePageDesign() {
        return HomePageDesign.values()[this.homeDesign];
    }

    public String getFormattedCreated() {
        return StringUtil.formatDateToTimeAgo(this.created.getTime());
    }

    public MyDecimal formattedDeposits() {
        return new MyDecimal(this.depositsPrice, true);
    }

    public boolean isEmailValid() {
        return StringUtils.isNotBlank(this.server) && port > 0 && port < 65535 && StringUtils.isNotBlank(this.email) && StringUtils.isNotBlank(this.password);
    }

    public Map<String, String> getSocialNetworks() {
        Map<String, String> nets = new LinkedHashMap<>();
        add(nets, "facebook", facebook);
        add(nets, "x", x);
        add(nets, "instagram", instagram);
        add(nets, "youtube", youtube);
        add(nets, "linkedin", linkedin);
        add(nets, "telegram", telegram);
        add(nets, "tiktok", tiktok);
        add(nets, "reddit", reddit);
        add(nets, "discord", discord);
        add(nets, "medium", medium);

        return nets;
    }
    
    private void add(Map<String, String> nets, String net, String url) {
        if (StringUtils.isNotBlank(url)) {
            nets.put(net, url);
        }
    }

    @AllArgsConstructor
    @Getter
    public enum HomePageDesign {

        DESIGN_1("index"),
        DESIGN_2("index_2");

        private final String fileName;
    }
}
