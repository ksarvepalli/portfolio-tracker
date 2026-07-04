package com.portfolio.tracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.market")
public class MarketApiProperties {
    private YahooFinance yahooFinance = new YahooFinance();
    private MfApi mfapi = new MfApi();
    private GoldApi goldApi = new GoldApi();

    @Data
    public static class YahooFinance {
        private String apiKey;
        private String baseUrl;
    }

    @Data
    public static class MfApi {
        private String baseUrl;
    }

    @Data
    public static class GoldApi {
        private String baseUrl;
    }
}