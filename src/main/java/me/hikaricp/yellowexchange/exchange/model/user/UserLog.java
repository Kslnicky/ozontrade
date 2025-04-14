package me.hikaricp.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hikaricp.yellowexchange.utils.DeviceUtil;
import me.hikaricp.yellowexchange.utils.GeoUtil;
import me.hikaricp.yellowexchange.utils.StringUtil;

import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "user_logs")
@NoArgsConstructor
@Getter
@Setter
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    @Size(max = 128)
    private String action;

    @NotBlank
    @Size(max = 64)
    private String ip;

    @Size(max = 128)
    private String platform;

    @Column()
    private GeoUtil.GeoData geolocation;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean forUser;

    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public UserLog(String action, String ip, User user, String platform, long time, boolean forUser) {
        this.action = action;
        this.ip = ip;
        this.date = new Timestamp(time);
        this.platform = platform;
        this.geolocation = GeoUtil.getGeo(this.ip);
        this.user = user;
        this.forUser = forUser;
    }

    public String getFormattedDate() {
        return StringUtil.formatDateToTimeAgo(this.date.getTime());
    }

    public DeviceUtil.DeviceType getDeviceType() {
        return DeviceUtil.getDeviceType(this.platform);
    }
}
