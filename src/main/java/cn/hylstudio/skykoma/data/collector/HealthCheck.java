package cn.hylstudio.skykoma.data.collector;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HealthCheck {
    @RequestMapping({""})
    public String health() {
        return "ok";
    }
}
