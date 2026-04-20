package com.daniel.dailyView.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.screener.alpha-futures")
public class AlphaFuturesProperties {

    private String binanceAlphaBaseUrl = "https://www.binance.com";
    private String binanceFuturesBaseUrl = "https://fapi.binance.com";
    private String chainbaseBaseUrl = "https://api.chainbase.online";
    private String chainbaseApiKey = "demo";
    private BigDecimal minOpenInterestUsdt = new BigDecimal("5000000");
    private BigDecimal maxMonthlyPumpPct = new BigDecimal("500");
    private BigDecimal minTop10HoldingRatioPct = new BigDecimal("90");
    private BigDecimal minWeeklyAverageOiRatio = new BigDecimal("1.5");
    private BigDecimal minWeeklyPeakOiRatio = new BigDecimal("2.0");
    private String currentOiPeriod = "5m";
    private String oiHistoryPeriod = "1d";
    private String priceKlineInterval = "1d";
    private int monthlyKlineLimit = 30;
    private int oiHistoryLimit = 30;
    private int recentWindowDays = 7;
    private int topHolderLimit = 10;
    private boolean excludeFullyDelisted = true;
    private boolean excludeOffsell = true;
    private boolean refreshEnabled = true;
    private String refreshCron = "0 0 * * * *";
    private String refreshZone = "Asia/Shanghai";
    private boolean refreshRunOnStartup = true;

    public String getBinanceAlphaBaseUrl() {
        return binanceAlphaBaseUrl;
    }

    public void setBinanceAlphaBaseUrl(String binanceAlphaBaseUrl) {
        this.binanceAlphaBaseUrl = binanceAlphaBaseUrl;
    }

    public String getBinanceFuturesBaseUrl() {
        return binanceFuturesBaseUrl;
    }

    public void setBinanceFuturesBaseUrl(String binanceFuturesBaseUrl) {
        this.binanceFuturesBaseUrl = binanceFuturesBaseUrl;
    }

    public String getChainbaseBaseUrl() {
        return chainbaseBaseUrl;
    }

    public void setChainbaseBaseUrl(String chainbaseBaseUrl) {
        this.chainbaseBaseUrl = chainbaseBaseUrl;
    }

    public String getChainbaseApiKey() {
        return chainbaseApiKey;
    }

    public void setChainbaseApiKey(String chainbaseApiKey) {
        this.chainbaseApiKey = chainbaseApiKey;
    }

    public BigDecimal getMinOpenInterestUsdt() {
        return minOpenInterestUsdt;
    }

    public void setMinOpenInterestUsdt(BigDecimal minOpenInterestUsdt) {
        this.minOpenInterestUsdt = minOpenInterestUsdt;
    }

    public BigDecimal getMaxMonthlyPumpPct() {
        return maxMonthlyPumpPct;
    }

    public void setMaxMonthlyPumpPct(BigDecimal maxMonthlyPumpPct) {
        this.maxMonthlyPumpPct = maxMonthlyPumpPct;
    }

    public BigDecimal getMinTop10HoldingRatioPct() {
        return minTop10HoldingRatioPct;
    }

    public void setMinTop10HoldingRatioPct(BigDecimal minTop10HoldingRatioPct) {
        this.minTop10HoldingRatioPct = minTop10HoldingRatioPct;
    }

    public BigDecimal getMinWeeklyAverageOiRatio() {
        return minWeeklyAverageOiRatio;
    }

    public void setMinWeeklyAverageOiRatio(BigDecimal minWeeklyAverageOiRatio) {
        this.minWeeklyAverageOiRatio = minWeeklyAverageOiRatio;
    }

    public BigDecimal getMinWeeklyPeakOiRatio() {
        return minWeeklyPeakOiRatio;
    }

    public void setMinWeeklyPeakOiRatio(BigDecimal minWeeklyPeakOiRatio) {
        this.minWeeklyPeakOiRatio = minWeeklyPeakOiRatio;
    }

    public String getCurrentOiPeriod() {
        return currentOiPeriod;
    }

    public void setCurrentOiPeriod(String currentOiPeriod) {
        this.currentOiPeriod = currentOiPeriod;
    }

    public String getOiHistoryPeriod() {
        return oiHistoryPeriod;
    }

    public void setOiHistoryPeriod(String oiHistoryPeriod) {
        this.oiHistoryPeriod = oiHistoryPeriod;
    }

    public String getPriceKlineInterval() {
        return priceKlineInterval;
    }

    public void setPriceKlineInterval(String priceKlineInterval) {
        this.priceKlineInterval = priceKlineInterval;
    }

    public int getMonthlyKlineLimit() {
        return monthlyKlineLimit;
    }

    public void setMonthlyKlineLimit(int monthlyKlineLimit) {
        this.monthlyKlineLimit = monthlyKlineLimit;
    }

    public int getOiHistoryLimit() {
        return oiHistoryLimit;
    }

    public void setOiHistoryLimit(int oiHistoryLimit) {
        this.oiHistoryLimit = oiHistoryLimit;
    }

    public int getRecentWindowDays() {
        return recentWindowDays;
    }

    public void setRecentWindowDays(int recentWindowDays) {
        this.recentWindowDays = recentWindowDays;
    }

    public int getTopHolderLimit() {
        return topHolderLimit;
    }

    public void setTopHolderLimit(int topHolderLimit) {
        this.topHolderLimit = topHolderLimit;
    }

    public boolean isExcludeFullyDelisted() {
        return excludeFullyDelisted;
    }

    public void setExcludeFullyDelisted(boolean excludeFullyDelisted) {
        this.excludeFullyDelisted = excludeFullyDelisted;
    }

    public boolean isExcludeOffsell() {
        return excludeOffsell;
    }

    public void setExcludeOffsell(boolean excludeOffsell) {
        this.excludeOffsell = excludeOffsell;
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
    }

    public String getRefreshCron() {
        return refreshCron;
    }

    public void setRefreshCron(String refreshCron) {
        this.refreshCron = refreshCron;
    }

    public String getRefreshZone() {
        return refreshZone;
    }

    public void setRefreshZone(String refreshZone) {
        this.refreshZone = refreshZone;
    }

    public boolean isRefreshRunOnStartup() {
        return refreshRunOnStartup;
    }

    public void setRefreshRunOnStartup(boolean refreshRunOnStartup) {
        this.refreshRunOnStartup = refreshRunOnStartup;
    }
}
