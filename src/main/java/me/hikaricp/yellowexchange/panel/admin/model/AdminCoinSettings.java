package me.hikaricp.yellowexchange.panel.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.hikaricp.yellowexchange.panel.common.model.CoinSettings;

@Entity
@Table(name = "admin_coin_settings")
public class AdminCoinSettings extends CoinSettings {
}
