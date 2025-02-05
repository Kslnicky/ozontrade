package me.yukitale.yellowexchange.exchange.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "coins")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Coin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long position;

    @Size(min = 1, max = 10)
    @Column(unique = true, nullable = false)
    private String symbol;

    @Size(max = 64)
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String icon;

    private String networks;

    private boolean memo;

    @Transient
    private List<String> networkList;

    public List<String> getNetworkList() {
        if (this.networkList == null) {
            this.networkList = StringUtils.isBlank(this.networks) ? Collections.emptyList() : Arrays.asList(networks.split(";"));
        }

        return this.networkList;
    }
}
