package com.daniel.dailyView.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.connectivity.health")
public class ConnectivityHealthProperties {

    private int timeoutSeconds = 15;
    private String chainbaseSampleChainId = "56";
    private String chainbaseSampleContractAddress = "0x92aa03137385f18539301349dcfc9ebc923ffb10";

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getChainbaseSampleChainId() {
        return chainbaseSampleChainId;
    }

    public void setChainbaseSampleChainId(String chainbaseSampleChainId) {
        this.chainbaseSampleChainId = chainbaseSampleChainId;
    }

    public String getChainbaseSampleContractAddress() {
        return chainbaseSampleContractAddress;
    }

    public void setChainbaseSampleContractAddress(String chainbaseSampleContractAddress) {
        this.chainbaseSampleContractAddress = chainbaseSampleContractAddress;
    }
}
