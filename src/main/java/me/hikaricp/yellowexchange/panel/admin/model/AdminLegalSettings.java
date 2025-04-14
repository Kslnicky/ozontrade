package me.hikaricp.yellowexchange.panel.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.hikaricp.yellowexchange.panel.common.model.LegalSettings;

@Entity
@Table(name = "admin_legal_settings")
public class AdminLegalSettings extends LegalSettings {
}
