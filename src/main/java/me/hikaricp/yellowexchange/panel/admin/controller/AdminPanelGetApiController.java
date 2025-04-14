package me.hikaricp.yellowexchange.panel.admin.controller;

import me.hikaricp.yellowexchange.exchange.model.user.User;
import me.hikaricp.yellowexchange.exchange.model.user.UserSupportDialog;
import me.hikaricp.yellowexchange.exchange.model.user.UserSupportMessage;
import me.hikaricp.yellowexchange.exchange.repository.user.UserRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.UserSupportDialogRepository;
import me.hikaricp.yellowexchange.exchange.repository.user.UserSupportMessageRepository;
import me.hikaricp.yellowexchange.panel.common.data.Stats;
import me.hikaricp.yellowexchange.panel.common.data.WorkerTopStats;
import me.hikaricp.yellowexchange.panel.common.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping(value = "/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminPanelGetApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private StatsService statsService;

    //start support
    @GetMapping(value = "support/get")
    public String supportGetController(Model model, @RequestParam(name = "user_id") long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "500";
        }

        List<UserSupportMessage> supportMessages = userSupportMessageRepository.findByUserIdOrderByIdAsc(user.getId());

        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(userId).orElse(null);

        model.addAttribute("user", user);

        model.addAttribute("support_messages", supportMessages);

        if (userSupportDialog != null && userSupportDialog.getSupportUnviewedMessages() > 0) {
            userSupportDialog.setSupportUnviewedMessages(0);
            userSupportDialogRepository.save(userSupportDialog);
        }

        userSupportMessageRepository.markSupportViewedToTrueByUserId(user.getId());

        return "panel/admin/get_admin_support";
    }
    //end support

    //start stats
    @GetMapping(value = "stats")
    public String statsController(Model model, @RequestParam(name = "type", required = false, defaultValue = "TODAY") String typeName) {
        WorkerTopStats.Type type = Arrays.stream(WorkerTopStats.Type.values())
                .filter(type1 -> type1.name().equalsIgnoreCase(typeName))
                .findFirst()
                .orElse(WorkerTopStats.Type.TODAY);

        model.addAttribute("all_stats", statsService.getAllWorkerStats(type));

        return "panel/admin/get_statistic";
    }

    @GetMapping(value = "detailed-stats")
    public String detailedStatsController(Model model, @RequestParam(name = "type", required = false, defaultValue = "ALL") String typeName, @RequestParam(name = "stats_type", required = false, defaultValue = "userCountries") String statsTypeName) {
        Stats.StatsType type = Arrays.stream(Stats.StatsType.values())
                .filter(type1 -> type1.name().equalsIgnoreCase(typeName))
                .findFirst()
                .orElse(Stats.StatsType.ALL);

        String statsType = statsTypeName == null || (!statsTypeName.equals("depositCountries") && !statsTypeName.equals("depositCoins") && !statsTypeName.equals("userRefers")) ? "userCountries" : statsTypeName;

        model.addAttribute("detailed_stats", statsService.getAdminDetailedStats(type));

        model.addAttribute("stats_type", statsType);

        return "panel/admin/get_detailed_statistic";
    }
    //end stats
}
