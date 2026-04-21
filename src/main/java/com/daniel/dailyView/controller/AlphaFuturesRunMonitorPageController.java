package com.daniel.dailyView.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlphaFuturesRunMonitorPageController {

    @GetMapping({"/monitor/alpha-futures", "/monitor/alpha-futures/"})
    public String alphaFuturesMonitorPage() {
        return "forward:/alpha-futures-monitor.html";
    }
}
