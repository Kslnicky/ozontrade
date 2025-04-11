package me.yukitale.yellowexchange.exchange.controller.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.yellowexchange.exchange.service.UserService;
import me.yukitale.yellowexchange.panel.common.service.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class CustomErrorController implements ErrorController {

    @Autowired
    private UserService userService;

    @Autowired
    private DomainService domainService;

    @GetMapping("/error")
    public String handleError(Authentication authentication, HttpServletRequest request, Model model, @RequestHeader("host") String host) {
        userService.addUserAttribute(authentication, model);

        domainService.addDomainAttribute(model, host);

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "exchange/error_pages/404";
            } else {
                return "exchange/error_pages/500";
            }
        }

        return "exchange/error_pages/500";
    }
}
