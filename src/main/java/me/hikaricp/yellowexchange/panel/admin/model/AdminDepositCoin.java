package me.hikaricp.yellowexchange.panel.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.hikaricp.yellowexchange.panel.common.model.DepositCoin;

@Entity
@Table(name = "admin_deposit_coins")
public class AdminDepositCoin extends DepositCoin {
}
