package com.daniel.dailyView.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daniel.dailyView.dto.ConnectivityHealthResponse;
import com.daniel.dailyView.service.ConnectivityHealthService;

@RestController
@RequestMapping("/actuator/health")
public class ConnectivityHealthController {

    private final ConnectivityHealthService connectivityHealthService;

    public ConnectivityHealthController(ConnectivityHealthService connectivityHealthService) {
        this.connectivityHealthService = connectivityHealthService;
    }

    @GetMapping("/connectivity")
    public ResponseEntity<ConnectivityHealthResponse> connectivity() {
        ConnectivityHealthResponse response = connectivityHealthService.checkAll();
        HttpStatus status = "UP".equals(response.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }
}
