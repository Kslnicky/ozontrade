package me.hikaricp.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.util.*;

@Entity
@Table(name = "user_favorite_coins")
@Getter
@Setter
@NoArgsConstructor
public class UserFavoriteCoins {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(max = 512)
    @Column(columnDefinition = "TEXT", length = 512)
    private String favorites;

    @Transient
    private List<String> favoriteCoins;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public List<String> getFavoriteCoins() {
        if (this.favoriteCoins == null) {
            this.favoriteCoins = new ArrayList<>();
            if (StringUtils.isNotBlank(this.favorites)) {
                this.favoriteCoins.addAll(Arrays.asList(this.favorites.split(",")));
            }
        }

        return this.favoriteCoins;
    }

    public void setFavorites(String favorites) {
        this.favorites = favorites;
        this.favoriteCoins = null;
    }
}
