package me.hikaricp.yellowexchange.panel.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.hikaricp.yellowexchange.panel.common.model.SupportPreset;

@Entity
@Table(name = "admin_support_presets")
public class AdminSupportPreset extends SupportPreset {
}
