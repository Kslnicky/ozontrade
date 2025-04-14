package me.hikaricp.yellowexchange.panel.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.hikaricp.yellowexchange.panel.common.model.ErrorMessages;

@Entity
@Table(name = "admin_error_messages")
public class AdminErrorMessages extends ErrorMessages {
}
