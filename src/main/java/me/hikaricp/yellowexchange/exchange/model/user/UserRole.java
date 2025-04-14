package me.hikaricp.yellowexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class UserRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.ORDINAL)
  private UserRoleType name;

  public UserRole(UserRoleType name) {
    this.name = name;
  }

  public enum UserRoleType {

    ROLE_USER,
    ROLE_WORKER,
    ROLE_ADMIN,
    ROLE_SUPPORTER,
    ROLE_MANAGER;
  }
}