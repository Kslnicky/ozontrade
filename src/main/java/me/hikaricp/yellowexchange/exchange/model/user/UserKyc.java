package me.hikaricp.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hikaricp.yellowexchange.utils.StringUtil;

import java.util.Date;

@Entity
@Table(name = "user_kyc")
@Getter
@Setter
@NoArgsConstructor
public class UserKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String country;

    private String name;

    private String family;

    private String lastName;

    private String birthDate;

    private String gender;

    private String city;

    private String street;

    private long houseNumber;

    private long apartNumber;

    private long postalCode;

    private String documentCountry;

    private String documentType;

    private String documentPhoto1;

    private String documentPhoto2;

    private String selfie;

    private int level;

    private boolean acceptedLv1;

    private long autoAccept;

    private String addressPhoto;

    private boolean acceptedLv2;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean viewed;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lv1Date;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lv2Date;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public boolean isAcceptedLv1() {
        return this.acceptedLv1 || (this.autoAccept > 0 && this.autoAccept <= System.currentTimeMillis());
    }

    public String getFormattedLv1Date() {
        return StringUtil.formatDateToTimeAgo(this.lv1Date.getTime());
    }

    public String getFormattedLv2Date() {
        return StringUtil.formatDateToTimeAgo(this.lv2Date.getTime());
    }
}
