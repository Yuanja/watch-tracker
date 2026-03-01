package com.tradeintel.processing;

import com.tradeintel.config.CacheConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * Converts prices to USD using Frankfurter.app (ECB-backed, free, no API key).
 *
 * <p>AED is hardcoded at the long-standing peg of 3.6725 AED per USD.
 * All other supported currencies are fetched via the Frankfurter API.
 * Results are cached for 24 hours keyed on currency+date.</p>
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LogManager.getLogger(ExchangeRateService.class);

    private static final BigDecimal AED_PER_USD = new BigDecimal("3.6725");
    private static final BigDecimal AED_TO_USD = BigDecimal.ONE.divide(AED_PER_USD, 10, RoundingMode.HALF_UP);

    private final RestTemplate restTemplate;

    public ExchangeRateService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Returns the exchange rate to convert 1 unit of the given currency to USD.
     *
     * @param currency ISO 4217 currency code (e.g. "EUR", "GBP")
     * @param date     the date for the historical rate
     * @return the rate such that {@code 1 currency * rate = X USD}
     */
    @Cacheable(value = CacheConfig.CACHE_EXCHANGE_RATES, key = "#currency + '_' + #date")
    public BigDecimal getRateToUsd(String currency, LocalDate date) {
        if (currency == null || currency.isBlank()) {
            return null;
        }

        String code = currency.trim().toUpperCase();

        if ("USD".equals(code)) {
            return BigDecimal.ONE;
        }

        if ("AED".equals(code)) {
            return AED_TO_USD;
        }

        try {
            String url = String.format("https://api.frankfurter.dev/v1/%s?from=%s&to=USD", date, code);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                Object usdRate = rates.get("USD");
                if (usdRate != null) {
                    return new BigDecimal(usdRate.toString());
                }
            }

            log.warn("No USD rate in Frankfurter response for {} on {}", code, date);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rate for {} on {}: {}", code, date, e.getMessage());
            return null;
        }
    }

    /**
     * Computes the USD equivalent price.
     *
     * @param price    the original price
     * @param currency the original currency code
     * @param date     the date for the exchange rate
     * @return the price in USD, or null if the rate cannot be determined
     */
    public BigDecimal computeUsdPrice(BigDecimal price, String currency, LocalDate date) {
        if (price == null || currency == null) {
            return null;
        }

        BigDecimal rate = getRateToUsd(currency, date);
        if (rate == null) {
            return null;
        }

        return price.multiply(rate, new MathContext(10)).setScale(4, RoundingMode.HALF_UP);
    }
}
