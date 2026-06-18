package kuan.fintech.fintech_api_gateway.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayInfoController {

    @GetMapping("/gateway/info")
    public Map<String, Object> info() {
        return Map.of(
                "service", "fintech-api-gateway",
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}
