package me.hikaricp.yellowexchange.panel.manager.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/manager")
@PreAuthorize("hasRole('ROLE_MANAGER')")
public class ManagerController {
}
