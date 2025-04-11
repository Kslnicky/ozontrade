package me.yukitale.yellowexchange.exchange.controller.api;

import me.yukitale.yellowexchange.exchange.model.user.User;
import me.yukitale.yellowexchange.exchange.model.user.UserSupportDialog;
import me.yukitale.yellowexchange.exchange.model.user.UserSupportMessage;
import me.yukitale.yellowexchange.exchange.model.user.UserTradeOrder;
import me.yukitale.yellowexchange.exchange.repository.CoinRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserSupportDialogRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserSupportMessageRepository;
import me.yukitale.yellowexchange.exchange.repository.user.UserTradeOrderRepository;
import me.yukitale.yellowexchange.exchange.service.CoinService;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.common.model.Domain;
import me.yukitale.yellowexchange.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping(value = "/api/user")
@PreAuthorize("hasRole('ROLE_USER') || hasRole('ROLE_WORKER') || hasRole('ROLE_ADMIN') || hasRole('ROLE_SUPPORTER') || hasRole('ROLE_MANAGER')")
public class UserGetApiController {

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserTradeOrderRepository userTradeOrderRepository;

    @Autowired
    private UserService userService;

    //start swap
    @GetMapping(value = "balances")
    public ResponseEntity<String> swapController(Authentication authentication, @RequestParam(name = "action", defaultValue = "null") String action) {
        User user = userService.getUser(authentication);

        return ResponseEntity.ok(JsonUtil.writeJson(userService.getBalances(user)));
    }
    //end swap

    //start support
    @GetMapping(value = "support/get")
    public String supportController(Authentication authentication, Model model) {
        User user = userService.getUser(authentication);

        List<UserSupportMessage> supportMessages = userSupportMessageRepository.findByUserIdOrderByIdAsc(user.getId());

        model.addAttribute("user", user);

        model.addAttribute("support_messages", supportMessages);

        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);

        if (userSupportDialog != null && userSupportDialog.getUserUnviewedMessages() > 0) {
            userSupportDialog.setUserUnviewedMessages(0);
            userSupportDialogRepository.save(userSupportDialog);
        }

        userSupportMessageRepository.markUserViewedToTrueByUserId(user.getId());

        return "exchange/profile/get_user_support";
    }
    //end support

    //start trading
    @GetMapping(value = "trading")
    public String tradingController(Authentication authentication, Model model, @RequestParam(name = "action", defaultValue = "GET_OPEN_ORDERS") String action) {
        switch (action) {
            case "GET_OPEN_ORDERS": {
                return getOpenOrders(authentication, model);
            }
            case "GET_HISTORY_ORDERS": {
                return getHistoryOrders(authentication, model);
            }
            default: {
                return "500";
            }
        }
    }

    public String getOpenOrders(Authentication authentication, Model model) {
        User user = userService.getUser(authentication);

        List<UserTradeOrder> tradeOrders = userTradeOrderRepository.findByUserIdAndClosedOrderByCreatedDesc(user.getId(), false);

        model.addAttribute("trade_orders", tradeOrders);

        return "exchange/profile/get_open_orders";
    }

    public String getHistoryOrders(Authentication authentication, Model model) {
        User user = userService.getUser(authentication);

        List<UserTradeOrder> tradeOrders = userTradeOrderRepository.findByUserIdAndClosedOrderByCreatedDesc(user.getId(), true);

        model.addAttribute("trade_orders", tradeOrders);

        return "exchange/profile/get_history_orders";
    }
    //end trading
}
