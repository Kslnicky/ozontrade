package me.yukitale.yellowexchange.panel.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.yukitale.yellowexchange.panel.common.model.LegalSettings;

@Entity
@Table(name = "admin_legal_settings")
public class AdminLegalSettings extends LegalSettings {
}
