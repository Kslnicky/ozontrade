package me.hikaricp.yellowexchange.panel.supporter.service;

import me.hikaricp.yellowexchange.exchange.model.user.User;
import me.hikaricp.yellowexchange.exchange.model.user.UserRole;
import me.hikaricp.yellowexchange.exchange.repository.user.UserRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.UserRoleRepository;
import me.hikaricp.yellowexchange.exchange.service.UserDetailsServiceImpl;
import me.hikaricp.yellowexchange.panel.admin.model.AdminSupportPreset;
import me.hikaricp.yellowexchange.panel.admin.repository.AdminSupportPresetRepository;
import me.hikaricp.yellowexchange.panel.supporter.model.Supporter;
import me.hikaricp.yellowexchange.panel.supporter.model.SupporterSupportPreset;
import me.hikaricp.yellowexchange.panel.supporter.repository.SupporterRepository;
import me.hikaricp.yellowexchange.panel.supporter.repository.SupporterSupportPresetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SupporterService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository roleRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private SupporterSupportPresetsRepository supporterSupportPresetsRepository;

    @Autowired
    private AdminSupportPresetRepository adminSupportPresetRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Transactional
    public Supporter createSupporter(User user) {
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(roleRepository.findByName(UserRole.UserRoleType.ROLE_USER).orElseThrow());
        userRoles.add(roleRepository.findByName(UserRole.UserRoleType.ROLE_SUPPORTER).orElseThrow());

        user.setUserRoles(userRoles);
        user.setRoleType(UserRole.UserRoleType.ROLE_SUPPORTER);

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        Supporter supporter = new Supporter();
        supporter.setSupportPresetsEnabled(true);
        supporter.setUser(user);

        supporterRepository.save(supporter);

        List<AdminSupportPreset> adminSupportPresets = adminSupportPresetRepository.findAll();
        for (AdminSupportPreset adminSupportPreset : adminSupportPresets) {
            SupporterSupportPreset supporterSupportPreset = new SupporterSupportPreset();
            supporterSupportPreset.setTitle(adminSupportPreset.getTitle());
            supporterSupportPreset.setMessage(adminSupportPreset.getMessage());
            supporterSupportPreset.setSupporter(supporter);

            supporterSupportPresetsRepository.save(supporterSupportPreset);
        }

        return supporter;
    }

    @Transactional
    public void deleteSupporter(Supporter supporter) {
        User user = supporter.getUser();

        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(roleRepository.findByName(UserRole.UserRoleType.ROLE_USER).orElseThrow());

        user.setUserRoles(userRoles);
        user.setRoleType(UserRole.UserRoleType.ROLE_USER);

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        supporterSupportPresetsRepository.deleteAllBySupporterId(supporter.getId());

        supporterRepository.deleteByUserId(user.getId());
    }
}
