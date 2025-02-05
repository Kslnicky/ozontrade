package me.yukitale.yellowexchange.panel.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.yukitale.yellowexchange.panel.common.model.SupportPreset;

@Entity
@Table(name = "admin_support_presets")
public class AdminSupportPreset extends SupportPreset {
}
