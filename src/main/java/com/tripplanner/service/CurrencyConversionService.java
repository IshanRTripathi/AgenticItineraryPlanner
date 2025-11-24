package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * CurrencyConversionService - Provides currency conversion utilities for cost estimation.
 * 
 * This service helps convert budget amounts from user's currency to destination's local currency
 * for accurate cost estimation by LLMs.
 * 
 * IMPORTANT: Exchange rates are approximate and should be updated periodically.
 * For production, integrate with a real-time exchange rate API (e.g., exchangerate-api.com).
 */
@Service
public class CurrencyConversionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CurrencyConversionService.class);
    
    // Base currency for conversions (USD - US Dollar)
    private static final String BASE_CURRENCY = "USD";
    
    // Exchange rates relative to USD (1 USD = X foreign currency)
    // Synced with BigQuery analytics.exchange_rates table
    // Updated: November 2025 (165 currencies)
    private static final Map<String, Double> EXCHANGE_RATES = new HashMap<>();
    
    static {
        // Base currency
        EXCHANGE_RATES.put("USD", 1.0);           // US Dollar (base)
        
        // Major currencies
        EXCHANGE_RATES.put("INR", 0.012);         // Indian Rupee
        EXCHANGE_RATES.put("EUR", 0.92);          // Euro
        EXCHANGE_RATES.put("GBP", 0.79);          // British Pound
        EXCHANGE_RATES.put("JPY", 154.17);        // Japanese Yen
        EXCHANGE_RATES.put("CNY", 7.17);          // Chinese Yuan
        EXCHANGE_RATES.put("AUD", 1.50);          // Australian Dollar
        EXCHANGE_RATES.put("CAD", 1.42);          // Canadian Dollar
        
        // Southeast Asian currencies
        EXCHANGE_RATES.put("SGD", 1.33);          // Singapore Dollar
        EXCHANGE_RATES.put("MYR", 4.67);          // Malaysian Ringgit
        EXCHANGE_RATES.put("THB", 35.0);          // Thai Baht
        EXCHANGE_RATES.put("IDR", 16000.0);       // Indonesian Rupiah
        EXCHANGE_RATES.put("PHP", 58.33);         // Philippine Peso
        EXCHANGE_RATES.put("VND", 25417.0);       // Vietnamese Dong
        EXCHANGE_RATES.put("KRW", 1375.0);        // South Korean Won
        
        // Middle East currencies
        EXCHANGE_RATES.put("AED", 3.67);          // UAE Dirham
        EXCHANGE_RATES.put("SAR", 3.75);          // Saudi Riyal
        EXCHANGE_RATES.put("QAR", 3.64);          // Qatari Riyal
        EXCHANGE_RATES.put("KWD", 0.31);          // Kuwaiti Dinar
        EXCHANGE_RATES.put("OMR", 0.38);          // Omani Rial
        EXCHANGE_RATES.put("BHD", 0.38);          // Bahraini Dinar
        EXCHANGE_RATES.put("JOD", 0.71);          // Jordanian Dinar
        EXCHANGE_RATES.put("ILS", 3.7);           // Israeli Shekel (if needed)
        
        // European currencies
        EXCHANGE_RATES.put("CHF", 0.92);          // Swiss Franc
        EXCHANGE_RATES.put("SEK", 10.83);         // Swedish Krona
        EXCHANGE_RATES.put("NOK", 10.83);         // Norwegian Krone
        EXCHANGE_RATES.put("DKK", 6.92);          // Danish Krone
        EXCHANGE_RATES.put("PLN", 4.0);           // Polish Zloty
        EXCHANGE_RATES.put("CZK", 23.0);          // Czech Koruna
        EXCHANGE_RATES.put("HUF", 360.0);         // Hungarian Forint
        EXCHANGE_RATES.put("RON", 4.6);           // Romanian Leu
        EXCHANGE_RATES.put("BGN", 1.8);           // Bulgarian Lev
        EXCHANGE_RATES.put("ISK", 138.0);         // Icelandic Krona
        EXCHANGE_RATES.put("HRK", 6.9);           // Croatian Kuna
        EXCHANGE_RATES.put("RSD", 110.0);         // Serbian Dinar
        EXCHANGE_RATES.put("MKD", 58.0);          // Macedonian Denar
        EXCHANGE_RATES.put("MDL", 17.7);          // Moldovan Leu
        EXCHANGE_RATES.put("BYN", 3.4);           // Belarusian Ruble
        EXCHANGE_RATES.put("BAM", 1.8);           // Bosnia Convertible Mark
        
        // Other major currencies
        EXCHANGE_RATES.put("NZD", 1.67);          // New Zealand Dollar
        EXCHANGE_RATES.put("ZAR", 18.33);         // South African Rand
        EXCHANGE_RATES.put("BRL", 5.83);          // Brazilian Real
        EXCHANGE_RATES.put("MXN", 20.0);          // Mexican Peso
        EXCHANGE_RATES.put("RUB", 100.0);         // Russian Ruble
        EXCHANGE_RATES.put("TRY", 34.17);         // Turkish Lira
        EXCHANGE_RATES.put("HKD", 7.75);          // Hong Kong Dollar
        EXCHANGE_RATES.put("TWD", 31.67);         // Taiwan Dollar
        
        // South Asian currencies
        EXCHANGE_RATES.put("PKR", 279.17);        // Pakistani Rupee
        EXCHANGE_RATES.put("BDT", 110.0);         // Bangladeshi Taka
        EXCHANGE_RATES.put("LKR", 291.67);        // Sri Lankan Rupee
        EXCHANGE_RATES.put("NPR", 133.33);        // Nepalese Rupee
        EXCHANGE_RATES.put("MVR", 15.0);          // Maldivian Rufiyaa
        EXCHANGE_RATES.put("BTN", 0.012);         // Bhutanese Ngultrum
        
        // Latin American currencies
        EXCHANGE_RATES.put("ARS", 950.0);         // Argentine Peso
        EXCHANGE_RATES.put("CLP", 950.0);         // Chilean Peso
        EXCHANGE_RATES.put("COP", 4200.0);        // Colombian Peso
        EXCHANGE_RATES.put("PEN", 3.7);           // Peruvian Sol
        EXCHANGE_RATES.put("UYU", 40.0);          // Uruguayan Peso
        EXCHANGE_RATES.put("BOB", 6.9);           // Bolivian Boliviano
        EXCHANGE_RATES.put("PYG", 7500.0);        // Paraguayan Guarani
        EXCHANGE_RATES.put("CRC", 540.0);         // Costa Rican Colon
        EXCHANGE_RATES.put("GTQ", 7.8);           // Guatemalan Quetzal
        EXCHANGE_RATES.put("HNL", 24.5);          // Honduran Lempira
        EXCHANGE_RATES.put("NIO", 36.8);          // Nicaraguan Cordoba
        EXCHANGE_RATES.put("DOP", 59.0);          // Dominican Peso
        EXCHANGE_RATES.put("JMD", 155.0);         // Jamaican Dollar
        EXCHANGE_RATES.put("BBD", 2.0);           // Barbadian Dollar
        EXCHANGE_RATES.put("TTD", 6.8);           // Trinidad & Tobago Dollar
        
        // African currencies
        EXCHANGE_RATES.put("EGP", 49.17);         // Egyptian Pound
        EXCHANGE_RATES.put("MAD", 10.2);          // Moroccan Dirham
        EXCHANGE_RATES.put("TND", 3.1);           // Tunisian Dinar
        EXCHANGE_RATES.put("KES", 160.0);         // Kenyan Shilling
        EXCHANGE_RATES.put("UGX", 3830.0);        // Ugandan Shilling
        EXCHANGE_RATES.put("TZS", 2600.0);        // Tanzanian Shilling
        EXCHANGE_RATES.put("ETB", 56.0);          // Ethiopian Birr
        EXCHANGE_RATES.put("GHS", 15.5);          // Ghanaian Cedi
        EXCHANGE_RATES.put("NGN", 1650.0);        // Nigerian Naira
        EXCHANGE_RATES.put("ZMW", 28.0);          // Zambian Kwacha
        EXCHANGE_RATES.put("BWP", 13.5);          // Botswana Pula
        EXCHANGE_RATES.put("MZN", 63.0);          // Mozambican Metical
        EXCHANGE_RATES.put("NAD", 18.5);          // Namibian Dollar
        EXCHANGE_RATES.put("AOA", 890.0);         // Angolan Kwanza
        EXCHANGE_RATES.put("XAF", 610.0);         // Central African CFA Franc
        EXCHANGE_RATES.put("XOF", 610.0);         // West African CFA Franc
        EXCHANGE_RATES.put("XPF", 111.0);         // CFP Franc
        EXCHANGE_RATES.put("BIF", 2850.0);        // Burundian Franc
        EXCHANGE_RATES.put("DJF", 177.0);         // Djiboutian Franc
        EXCHANGE_RATES.put("ERN", 15.0);          // Eritrean Nakfa
        EXCHANGE_RATES.put("GMD", 66.0);          // Gambian Dalasi
        EXCHANGE_RATES.put("GNF", 8600.0);        // Guinean Franc
        EXCHANGE_RATES.put("LSL", 18.5);          // Lesotho Loti
        EXCHANGE_RATES.put("LRD", 190.0);         // Liberian Dollar
        EXCHANGE_RATES.put("MWK", 1740.0);        // Malawian Kwacha
        EXCHANGE_RATES.put("MGA", 4500.0);        // Malagasy Ariary
        EXCHANGE_RATES.put("MRU", 10.3);          // Mauritanian Ouguiya
        EXCHANGE_RATES.put("MUR", 46.0);          // Mauritian Rupee (if needed)
        EXCHANGE_RATES.put("RWF", 1350.0);        // Rwandan Franc
        EXCHANGE_RATES.put("SCR", 14.5);          // Seychellois Rupee
        EXCHANGE_RATES.put("SLL", 22500.0);       // Sierra Leonean Leone
        EXCHANGE_RATES.put("SOS", 58000.0);       // Somali Shilling
        EXCHANGE_RATES.put("SZL", 18.5);          // Eswatini Lilangeni
        EXCHANGE_RATES.put("STN", 25.0);          // São Tomé Dobra
        EXCHANGE_RATES.put("CVE", 103.0);         // Cape Verdean Escudo
        EXCHANGE_RATES.put("KMF", 461.0);         // Comorian Franc
        EXCHANGE_RATES.put("CDF", 2800.0);        // Congolese Franc
        EXCHANGE_RATES.put("LYD", 4.9);           // Libyan Dinar
        EXCHANGE_RATES.put("SDG", 650.0);         // Sudanese Pound
        
        // Central Asian currencies
        EXCHANGE_RATES.put("KZT", 480.0);         // Kazakhstani Tenge
        EXCHANGE_RATES.put("UZS", 12500.0);       // Uzbekistani Som
        EXCHANGE_RATES.put("AZN", 1.7);           // Azerbaijani Manat
        EXCHANGE_RATES.put("GEL", 2.7);           // Georgian Lari
        EXCHANGE_RATES.put("AMD", 390.0);         // Armenian Dram
        EXCHANGE_RATES.put("KGS", 89.0);          // Kyrgyzstani Som
        EXCHANGE_RATES.put("TMT", 3.5);           // Turkmenistani Manat
        EXCHANGE_RATES.put("TJS", 10.9);          // Tajikistani Somoni
        
        // Southeast Asian additional
        EXCHANGE_RATES.put("MMK", 2100.0);        // Myanmar Kyat
        EXCHANGE_RATES.put("KHR", 4100.0);        // Cambodian Riel
        EXCHANGE_RATES.put("LAK", 21000.0);       // Lao Kip
        EXCHANGE_RATES.put("MNT", 3440.0);        // Mongolian Tugrik
        EXCHANGE_RATES.put("BND", 1.33);          // Brunei Dollar
        EXCHANGE_RATES.put("MOP", 8.1);           // Macanese Pataca
        
        // Caribbean currencies
        EXCHANGE_RATES.put("XCD", 2.7);           // East Caribbean Dollar
        EXCHANGE_RATES.put("BSD", 1.0);           // Bahamian Dollar
        EXCHANGE_RATES.put("BMD", 1.0);           // Bermudian Dollar
        EXCHANGE_RATES.put("KYD", 0.83);          // Cayman Islands Dollar (if needed)
        EXCHANGE_RATES.put("AWG", 1.79);          // Aruban Florin
        EXCHANGE_RATES.put("ANG", 1.79);          // Netherlands Antillean Guilder
        EXCHANGE_RATES.put("HTG", 132.0);         // Haitian Gourde
        EXCHANGE_RATES.put("CUP", 24.0);          // Cuban Peso
        EXCHANGE_RATES.put("CUC", 1.0);           // Convertible Cuban Peso
        
        // South American additional
        EXCHANGE_RATES.put("SRD", 36.0);          // Surinamese Dollar
        EXCHANGE_RATES.put("GYD", 210.0);         // Guyanese Dollar
        EXCHANGE_RATES.put("BZD", 2.0);           // Belize Dollar
        EXCHANGE_RATES.put("FKP", 1.22);          // Falkland Islands Pound
        
        // Pacific Islands
        EXCHANGE_RATES.put("FJD", 2.26);          // Fijian Dollar
        EXCHANGE_RATES.put("PGK", 3.7);           // Papua New Guinea Kina
        EXCHANGE_RATES.put("WST", 2.8);           // Samoan Tala
        EXCHANGE_RATES.put("TOP", 2.4);           // Tongan Paʻanga
        EXCHANGE_RATES.put("SBD", 8.5);           // Solomon Islands Dollar
        EXCHANGE_RATES.put("VUV", 120.0);         // Vanuatu Vatu
        
        // Middle East additional
        EXCHANGE_RATES.put("SYP", 13000.0);       // Syrian Pound
        EXCHANGE_RATES.put("YER", 250.0);         // Yemeni Rial
        EXCHANGE_RATES.put("LBP", 89500.0);       // Lebanese Pound
        EXCHANGE_RATES.put("IRR", 42000.0);       // Iranian Rial
        
        // European microstates
        EXCHANGE_RATES.put("GIP", 0.79);          // Gibraltar Pound
        
        // Special/Commodity (optional)
        EXCHANGE_RATES.put("XAU", 0.00042);       // Gold (troy ounce)
        EXCHANGE_RATES.put("XAG", 0.04);          // Silver (troy ounce)
        EXCHANGE_RATES.put("XPT", 0.001);         // Platinum
        EXCHANGE_RATES.put("XPD", 0.0007);        // Palladium
        EXCHANGE_RATES.put("SDR", 1.32);          // IMF Special Drawing Rights
        
        // Historic/Legacy (for completeness)
        EXCHANGE_RATES.put("ZWL", 6500.0);        // Zimbabwean Dollar
        EXCHANGE_RATES.put("KPW", 900.0);         // North Korean Won
    }
    
    // Currency symbols for display (165+ currencies)
    private static final Map<String, String> CURRENCY_SYMBOLS = new HashMap<>();
    
    static {
        // Major currencies
        CURRENCY_SYMBOLS.put("USD", "$");
        CURRENCY_SYMBOLS.put("EUR", "€");
        CURRENCY_SYMBOLS.put("GBP", "£");
        CURRENCY_SYMBOLS.put("JPY", "¥");
        CURRENCY_SYMBOLS.put("CNY", "¥");
        CURRENCY_SYMBOLS.put("INR", "₹");
        CURRENCY_SYMBOLS.put("AUD", "A$");
        CURRENCY_SYMBOLS.put("CAD", "C$");
        CURRENCY_SYMBOLS.put("CHF", "CHF");
        
        // Southeast Asian
        CURRENCY_SYMBOLS.put("SGD", "S$");
        CURRENCY_SYMBOLS.put("MYR", "RM");
        CURRENCY_SYMBOLS.put("THB", "฿");
        CURRENCY_SYMBOLS.put("IDR", "Rp");
        CURRENCY_SYMBOLS.put("PHP", "₱");
        CURRENCY_SYMBOLS.put("VND", "₫");
        CURRENCY_SYMBOLS.put("KRW", "₩");
        CURRENCY_SYMBOLS.put("MMK", "K");
        CURRENCY_SYMBOLS.put("KHR", "៛");
        CURRENCY_SYMBOLS.put("LAK", "₭");
        CURRENCY_SYMBOLS.put("BND", "B$");
        
        // Middle East
        CURRENCY_SYMBOLS.put("AED", "د.إ");
        CURRENCY_SYMBOLS.put("SAR", "﷼");
        CURRENCY_SYMBOLS.put("QAR", "ر.ق");
        CURRENCY_SYMBOLS.put("KWD", "د.ك");
        CURRENCY_SYMBOLS.put("OMR", "ر.ع.");
        CURRENCY_SYMBOLS.put("BHD", "د.ب");
        CURRENCY_SYMBOLS.put("JOD", "د.ا");
        CURRENCY_SYMBOLS.put("ILS", "₪");
        
        // European
        CURRENCY_SYMBOLS.put("SEK", "kr");
        CURRENCY_SYMBOLS.put("NOK", "kr");
        CURRENCY_SYMBOLS.put("DKK", "kr");
        CURRENCY_SYMBOLS.put("PLN", "zł");
        CURRENCY_SYMBOLS.put("CZK", "Kč");
        CURRENCY_SYMBOLS.put("HUF", "Ft");
        CURRENCY_SYMBOLS.put("RON", "lei");
        CURRENCY_SYMBOLS.put("BGN", "лв");
        CURRENCY_SYMBOLS.put("ISK", "kr");
        CURRENCY_SYMBOLS.put("HRK", "kn");
        CURRENCY_SYMBOLS.put("RSD", "дин");
        
        // Americas
        CURRENCY_SYMBOLS.put("NZD", "NZ$");
        CURRENCY_SYMBOLS.put("BRL", "R$");
        CURRENCY_SYMBOLS.put("MXN", "Mex$");
        CURRENCY_SYMBOLS.put("ARS", "$");
        CURRENCY_SYMBOLS.put("CLP", "$");
        CURRENCY_SYMBOLS.put("COP", "$");
        CURRENCY_SYMBOLS.put("PEN", "S/");
        CURRENCY_SYMBOLS.put("UYU", "$U");
        CURRENCY_SYMBOLS.put("BOB", "Bs");
        CURRENCY_SYMBOLS.put("PYG", "₲");
        
        // Africa
        CURRENCY_SYMBOLS.put("ZAR", "R");
        CURRENCY_SYMBOLS.put("EGP", "E£");
        CURRENCY_SYMBOLS.put("NGN", "₦");
        CURRENCY_SYMBOLS.put("KES", "KSh");
        CURRENCY_SYMBOLS.put("GHS", "₵");
        CURRENCY_SYMBOLS.put("MAD", "د.م.");
        CURRENCY_SYMBOLS.put("TND", "د.ت");
        CURRENCY_SYMBOLS.put("XAF", "FCFA");
        CURRENCY_SYMBOLS.put("XOF", "CFA");
        
        // South Asian
        CURRENCY_SYMBOLS.put("PKR", "₨");
        CURRENCY_SYMBOLS.put("BDT", "৳");
        CURRENCY_SYMBOLS.put("LKR", "Rs");
        CURRENCY_SYMBOLS.put("NPR", "Rs");
        CURRENCY_SYMBOLS.put("MVR", "Rf");
        CURRENCY_SYMBOLS.put("BTN", "Nu");
        
        // Other major
        CURRENCY_SYMBOLS.put("RUB", "₽");
        CURRENCY_SYMBOLS.put("TRY", "₺");
        CURRENCY_SYMBOLS.put("HKD", "HK$");
        CURRENCY_SYMBOLS.put("TWD", "NT$");
        CURRENCY_SYMBOLS.put("MOP", "MOP$");
        
        // Caribbean
        CURRENCY_SYMBOLS.put("XCD", "EC$");
        CURRENCY_SYMBOLS.put("JMD", "J$");
        CURRENCY_SYMBOLS.put("TTD", "TT$");
        CURRENCY_SYMBOLS.put("BBD", "Bds$");
        
        // Pacific
        CURRENCY_SYMBOLS.put("FJD", "FJ$");
        CURRENCY_SYMBOLS.put("PGK", "K");
        CURRENCY_SYMBOLS.put("WST", "WS$");
        CURRENCY_SYMBOLS.put("TOP", "T$");
        
        // Special
        CURRENCY_SYMBOLS.put("XAU", "XAU");
        CURRENCY_SYMBOLS.put("XAG", "XAG");
        CURRENCY_SYMBOLS.put("SDR", "SDR");
    }
    
    /**
     * Convert amount from one currency to another.
     * 
     * @param amount Amount to convert
     * @param fromCurrency Source currency code (e.g., "INR")
     * @param toCurrency Target currency code (e.g., "MYR")
     * @return Converted amount
     */
    public double convert(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            logger.warn("Null currency provided, returning original amount");
            return amount;
        }
        
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount; // Same currency, no conversion needed
        }
        
        Double fromRate = EXCHANGE_RATES.get(fromCurrency.toUpperCase());
        Double toRate = EXCHANGE_RATES.get(toCurrency.toUpperCase());
        
        if (fromRate == null || toRate == null) {
            logger.warn("Exchange rate not found for {} or {}, returning original amount", 
                       fromCurrency, toCurrency);
            return amount;
        }
        
        // Convert: amount in fromCurrency -> USD -> toCurrency
        double usdAmount = amount / fromRate;
        double convertedAmount = usdAmount * toRate;
        
        logger.debug("Converted {} {} to {} {} (rate: {} -> {})", 
                    amount, fromCurrency, 
                    String.format("%.2f", convertedAmount), toCurrency,
                    fromRate, toRate);
        
        return convertedAmount;
    }
    
    /**
     * Get currency symbol for display.
     * 
     * @param currencyCode Currency code (e.g., "INR")
     * @return Currency symbol (e.g., "₹") or currency code if symbol not found
     */
    public String getCurrencySymbol(String currencyCode) {
        if (currencyCode == null) {
            return "";
        }
        return CURRENCY_SYMBOLS.getOrDefault(currencyCode.toUpperCase(), currencyCode);
    }
    
    /**
     * Format amount with currency symbol.
     * 
     * @param amount Amount to format
     * @param currencyCode Currency code
     * @return Formatted string (e.g., "₹1,500" or "$25")
     */
    public String formatAmount(double amount, String currencyCode) {
        String symbol = getCurrencySymbol(currencyCode);
        return String.format("%s%.0f", symbol, amount);
    }
    
    /**
     * Build currency conversion context for LLM prompts.
     * This helps LLMs understand the budget in local currency terms.
     * 
     * @param budgetMin Minimum budget in user's currency
     * @param budgetMax Maximum budget in user's currency
     * @param userCurrency User's currency code (e.g., "INR")
     * @param destinationCurrency Destination's currency code (e.g., "MYR")
     * @return Formatted context string for LLM prompts
     */
    public String buildCurrencyContextForLLM(Double budgetMin, Double budgetMax, 
                                             String userCurrency, String destinationCurrency) {
        if (budgetMin == null && budgetMax == null) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("\n=== BUDGET CONVERSION ===\n");
        
        if (userCurrency != null && destinationCurrency != null && 
            !userCurrency.equalsIgnoreCase(destinationCurrency)) {
            
            context.append("User's budget: ");
            if (budgetMin != null) {
                context.append(formatAmount(budgetMin, userCurrency));
            }
            if (budgetMax != null) {
                if (budgetMin != null) {
                    context.append(" - ");
                }
                context.append(formatAmount(budgetMax, userCurrency));
            }
            context.append(" per person per day\n");
            
            context.append("Converted to local currency (").append(destinationCurrency).append("): ");
            if (budgetMin != null) {
                double convertedMin = convert(budgetMin, userCurrency, destinationCurrency);
                context.append(formatAmount(convertedMin, destinationCurrency));
            }
            if (budgetMax != null) {
                double convertedMax = convert(budgetMax, userCurrency, destinationCurrency);
                if (budgetMin != null) {
                    context.append(" - ");
                }
                context.append(formatAmount(convertedMax, destinationCurrency));
            }
            context.append(" per person per day\n");
            
            context.append("\nIMPORTANT: Use ").append(destinationCurrency)
                   .append(" for all cost estimates. The converted budget above is your reference.\n");
        } else {
            context.append("Budget: ");
            if (budgetMin != null) {
                context.append(formatAmount(budgetMin, userCurrency != null ? userCurrency : "USD"));
            }
            if (budgetMax != null) {
                if (budgetMin != null) {
                    context.append(" - ");
                }
                context.append(formatAmount(budgetMax, userCurrency != null ? userCurrency : "USD"));
            }
            context.append(" per person per day\n");
        }
        
        return context.toString();
    }
    
    /**
     * Detect destination currency from destination name.
     * This is a simple heuristic - for production, use a proper location-to-currency mapping service.
     * 
     * @param destination Destination name (e.g., "Malaysia", "Tokyo, Japan")
     * @return Currency code (e.g., "MYR", "JPY") or "INR" as default
     */
    public String detectDestinationCurrency(String destination) {
        if (destination == null || destination.isEmpty()) {
            return "USD"; // Default to USD
        }
        
        String dest = destination.toLowerCase();
        
        // Country-based detection
        if (dest.contains("malaysia")) return "MYR";
        if (dest.contains("singapore")) return "SGD";
        if (dest.contains("thailand")) return "THB";
        if (dest.contains("indonesia") || dest.contains("bali")) return "IDR";
        if (dest.contains("philippines")) return "PHP";
        if (dest.contains("vietnam")) return "VND";
        if (dest.contains("japan") || dest.contains("tokyo") || dest.contains("osaka") || dest.contains("kyoto")) return "JPY";
        if (dest.contains("china") || dest.contains("beijing") || dest.contains("shanghai")) return "CNY";
        if (dest.contains("korea") || dest.contains("seoul")) return "KRW";
        if (dest.contains("hong kong")) return "HKD";
        if (dest.contains("taiwan")) return "TWD";
        if (dest.contains("india") || dest.contains("delhi") || dest.contains("mumbai") || dest.contains("bangalore")) return "INR";
        if (dest.contains("pakistan")) return "PKR";
        if (dest.contains("bangladesh")) return "BDT";
        if (dest.contains("sri lanka")) return "LKR";
        if (dest.contains("nepal")) return "NPR";
        if (dest.contains("maldives")) return "MVR";
        if (dest.contains("uae") || dest.contains("dubai") || dest.contains("abu dhabi")) return "AED";
        if (dest.contains("saudi")) return "SAR";
        if (dest.contains("egypt")) return "EGP";
        if (dest.contains("turkey") || dest.contains("istanbul")) return "TRY";
        if (dest.contains("russia") || dest.contains("moscow")) return "RUB";
        if (dest.contains("australia") || dest.contains("sydney") || dest.contains("melbourne")) return "AUD";
        if (dest.contains("new zealand")) return "NZD";
        if (dest.contains("south africa")) return "ZAR";
        if (dest.contains("brazil")) return "BRL";
        if (dest.contains("mexico")) return "MXN";
        if (dest.contains("canada") || dest.contains("toronto") || dest.contains("vancouver")) return "CAD";
        if (dest.contains("usa") || dest.contains("united states") || dest.contains("new york") || 
            dest.contains("los angeles") || dest.contains("san francisco") || dest.contains("chicago")) return "USD";
        if (dest.contains("uk") || dest.contains("united kingdom") || dest.contains("london") || 
            dest.contains("england") || dest.contains("scotland")) return "GBP";
        if (dest.contains("france") || dest.contains("paris")) return "EUR";
        if (dest.contains("germany") || dest.contains("berlin") || dest.contains("munich")) return "EUR";
        if (dest.contains("italy") || dest.contains("rome") || dest.contains("venice") || dest.contains("milan")) return "EUR";
        if (dest.contains("spain") || dest.contains("barcelona") || dest.contains("madrid")) return "EUR";
        if (dest.contains("portugal") || dest.contains("lisbon")) return "EUR";
        if (dest.contains("netherlands") || dest.contains("amsterdam")) return "EUR";
        if (dest.contains("belgium") || dest.contains("brussels")) return "EUR";
        if (dest.contains("austria") || dest.contains("vienna")) return "EUR";
        if (dest.contains("greece") || dest.contains("athens")) return "EUR";
        if (dest.contains("switzerland") || dest.contains("zurich")) return "CHF";
        if (dest.contains("sweden") || dest.contains("stockholm")) return "SEK";
        if (dest.contains("norway") || dest.contains("oslo")) return "NOK";
        if (dest.contains("denmark") || dest.contains("copenhagen")) return "DKK";
        if (dest.contains("poland") || dest.contains("warsaw")) return "PLN";
        
        // Additional European countries
        if (dest.contains("czech") || dest.contains("prague")) return "CZK";
        if (dest.contains("hungary") || dest.contains("budapest")) return "HUF";
        if (dest.contains("romania") || dest.contains("bucharest")) return "RON";
        if (dest.contains("bulgaria") || dest.contains("sofia")) return "BGN";
        if (dest.contains("iceland") || dest.contains("reykjavik")) return "ISK";
        if (dest.contains("croatia") || dest.contains("zagreb")) return "HRK";
        if (dest.contains("serbia") || dest.contains("belgrade")) return "RSD";
        
        // Latin America additional
        if (dest.contains("argentina") || dest.contains("buenos aires")) return "ARS";
        if (dest.contains("chile") || dest.contains("santiago")) return "CLP";
        if (dest.contains("colombia") || dest.contains("bogota")) return "COP";
        if (dest.contains("peru") || dest.contains("lima")) return "PEN";
        if (dest.contains("uruguay") || dest.contains("montevideo")) return "UYU";
        if (dest.contains("bolivia") || dest.contains("la paz")) return "BOB";
        if (dest.contains("paraguay") || dest.contains("asuncion")) return "PYG";
        
        // Central America
        if (dest.contains("costa rica") || dest.contains("san jose")) return "CRC";
        if (dest.contains("guatemala")) return "GTQ";
        if (dest.contains("honduras")) return "HNL";
        if (dest.contains("nicaragua")) return "NIO";
        if (dest.contains("dominican") || dest.contains("santo domingo")) return "DOP";
        if (dest.contains("jamaica") || dest.contains("kingston")) return "JMD";
        
        // Africa additional
        if (dest.contains("morocco") || dest.contains("marrakech") || dest.contains("casablanca")) return "MAD";
        if (dest.contains("tunisia") || dest.contains("tunis")) return "TND";
        if (dest.contains("kenya") || dest.contains("nairobi")) return "KES";
        if (dest.contains("tanzania") || dest.contains("dar es salaam")) return "TZS";
        if (dest.contains("uganda") || dest.contains("kampala")) return "UGX";
        if (dest.contains("ethiopia") || dest.contains("addis ababa")) return "ETB";
        if (dest.contains("ghana") || dest.contains("accra")) return "GHS";
        if (dest.contains("nigeria") || dest.contains("lagos") || dest.contains("abuja")) return "NGN";
        
        // Middle East additional
        if (dest.contains("qatar") || dest.contains("doha")) return "QAR";
        if (dest.contains("kuwait")) return "KWD";
        if (dest.contains("oman") || dest.contains("muscat")) return "OMR";
        if (dest.contains("bahrain") || dest.contains("manama")) return "BHD";
        if (dest.contains("jordan") || dest.contains("amman")) return "JOD";
        if (dest.contains("lebanon") || dest.contains("beirut")) return "LBP";
        if (dest.contains("israel") || dest.contains("tel aviv") || dest.contains("jerusalem")) return "ILS";
        
        // Central Asia
        if (dest.contains("kazakhstan") || dest.contains("almaty")) return "KZT";
        if (dest.contains("uzbekistan") || dest.contains("tashkent")) return "UZS";
        if (dest.contains("azerbaijan") || dest.contains("baku")) return "AZN";
        if (dest.contains("georgia") || dest.contains("tbilisi")) return "GEL";
        if (dest.contains("armenia") || dest.contains("yerevan")) return "AMD";
        
        // Southeast Asia additional
        if (dest.contains("myanmar") || dest.contains("burma") || dest.contains("yangon")) return "MMK";
        if (dest.contains("cambodia") || dest.contains("phnom penh")) return "KHR";
        if (dest.contains("laos") || dest.contains("vientiane")) return "LAK";
        if (dest.contains("mongolia") || dest.contains("ulaanbaatar")) return "MNT";
        if (dest.contains("brunei")) return "BND";
        if (dest.contains("macau") || dest.contains("macao")) return "MOP";
        
        // Pacific Islands
        if (dest.contains("fiji")) return "FJD";
        if (dest.contains("papua new guinea")) return "PGK";
        if (dest.contains("samoa")) return "WST";
        if (dest.contains("tonga")) return "TOP";
        
        // Default to USD if no match
        logger.debug("Could not detect currency for destination: {}, defaulting to USD", destination);
        return "USD";
    }
    
    /**
     * Check if currency conversion is supported.
     * 
     * @param currencyCode Currency code to check
     * @return true if supported, false otherwise
     */
    public boolean isCurrencySupported(String currencyCode) {
        return currencyCode != null && EXCHANGE_RATES.containsKey(currencyCode.toUpperCase());
    }
    
    /**
     * Get all supported currency codes.
     * 
     * @return Set of supported currency codes
     */
    public java.util.Set<String> getSupportedCurrencies() {
        return EXCHANGE_RATES.keySet();
    }
}
