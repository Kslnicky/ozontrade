package me.hikaricp.yellowexchange.exchange.controller.api;

import me.hikaricp.yellowexchange.exchange.repository.CoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/api/exchange")
public class ExchangeGetApiController {

    @Autowired
    private CoinRepository coinRepository;

    //start swap
    @GetMapping(value = "swap")
    public ResponseEntity<String> swapController(@RequestParam(name = "action", defaultValue = "null") String action) {
        switch (action) {
            case "GET_PRICES": {
                return getCoinPrices();
            }
            default: {
                return ResponseEntity.ok("user.api.error.null");
            }
        }
    }

    public ResponseEntity<String> getCoinPrices() {
        return ResponseEntity.ok(coinRepository.findCoinsAsJson());
    }
    //end swap
}
