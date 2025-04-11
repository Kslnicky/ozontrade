package me.yukitale.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.yellowexchange.config.Resources;
import me.yukitale.yellowexchange.config.Variables;
import me.yukitale.yellowexchange.panel.worker.model.Worker;
import me.yukitale.yellowexchange.utils.GeoUtil;
import me.yukitale.yellowexchange.utils.MyDecimal;
import me.yukitale.yellowexchange.utils.StringUtil;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @Size(max = 64)
    private String domain;

    @Column(columnDefinition = "VARCHAR(128) DEFAULT ''")
    private String referrer;

    private String profilePhoto;

    private boolean twoFactorEnabled;

    @Size(min = 4, max = 20)
    private String antiPhishingCode;

    @NotBlank
    @Size(max = 32)
    private String twoFactorCode;

    @NotBlank
    @Size(min = 8, max = 8)
    private String ownRefCode;

    @Size(min = 8, max = 8)
    private String invitedRefCode;

    @NotBlank
    @Size(max = 64)
    private String regIp;

    @Size(max = 64)
    private String lastIp;

    @Column(columnDefinition = "VARCHAR(128) DEFAULT 'N/A'")
    @Size(max = 128)
    private String platform;

    @Column(columnDefinition = "VARCHAR(8) DEFAULT 'NO'")
    private String regCountryCode;

    @Column(columnDefinition = "VARCHAR(8) DEFAULT 'NO'")
    private String lastCountryCode;

    private long lastActivity;

    private long lastOnline;

    private long lastLogin;

    private int authCount;

    @Column(columnDefinition = "INT(2) DEFAULT 0")
    private int verificationLvl;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeKycLv1;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeKycLv2;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean vip;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean emailConfirmed;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean promoActivatedShowed;

    private long depositsCount;

    private double depositsPrice;

    private UserRole.UserRoleType roleType;

    private String promocode;

    @Temporal(TemporalType.TIMESTAMP)
    private Date registered;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<UserRole> userRoles = new HashSet<>();

    public String getShortedEmail() {
        String[] splitted = this.email.split("@");
        return (splitted[0].length() >= 3 ? splitted[0].substring(0, 3) : "") + "***@****";
    }

    public String getShortEmail() {
        String[] splitted = this.email.split("@");
        return (splitted[0].length() >= 6 ? splitted[0].substring(0, 6) : "") + "***@****";
    }

    public long getFakeId() {
        return this.id + Variables.FAKE_ID_ADDER;
    }

    public String getFormattedLastLogin() {
        return StringUtil.formatDate(new Date(this.lastLogin));
    }

    public GeoUtil.GeoData getGeolocation() {
        return GeoUtil.getGeo(this.lastIp);
    }

    public String getProfilePhoto() {
        return this.profilePhoto == null ? null : "../" + Resources.USER_PROFILES_PHOTO_DIR + "/" + this.profilePhoto;
    }

    @Transient
    public boolean isOnline() {
        return this.lastOnline >= System.currentTimeMillis() - (10 * 1000);
    }

    @Transient
    public MyDecimal formattedDeposits() {
        return new MyDecimal(this.depositsPrice, true);
    }

    @Transient
    public String getFormattedLastActivity() {
        long diff = (System.currentTimeMillis() - this.lastActivity) / 1000L;
        if (diff < 60) {
            return diff + " sec.";
        } else if (diff > 86400) {
            return StringUtil.formatDate(new Date(this.lastActivity));
        } else if (diff > 3600) {
            return diff / 3600 + " h.";
        } else {
            return diff / 60 + " min.";
        }
    }

    @Transient
    public String getFormattedRegistered() {
        return StringUtil.formatDate(this.registered);
    }

    @Transient
    public String formattedLastActivityEng() {
        return StringUtil.formatDate(new Date(this.lastActivity));
    }

    public int getSecurityLevel() {
        int level = 0;
        if (this.twoFactorEnabled) {
            level += 1;
        }
        if (this.antiPhishingCode != null) {
            level += 1;
        }

        return level;
    }
}
