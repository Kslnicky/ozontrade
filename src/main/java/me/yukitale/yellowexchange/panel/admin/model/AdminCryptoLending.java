package me.yukitale.yellowexchange.panel.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import me.yukitale.yellowexchange.panel.common.model.CryptoLending;

@Entity
@Table(name = "admin_crypto_lendings",
uniqueConstraints = {
        @UniqueConstraint(columnNames = {"coin_symbol"})
})
public class AdminCryptoLending extends CryptoLending {
}
