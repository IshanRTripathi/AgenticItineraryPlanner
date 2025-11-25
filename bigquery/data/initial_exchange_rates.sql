-- Initial Exchange Rates Data
-- Converted from CurrencyConversionService.java rates (INR base) to USD base
-- Formula: USD_rate = (INR_to_currency_rate / INR_to_USD_rate)
-- Where INR_to_USD_rate = 0.012

INSERT INTO `tripaiplanner.analytics.exchange_rates`
(currency_code, usd_rate, effective_date, source, is_active, notes)
VALUES
  -- Base
  ('USD', 1.0, CURRENT_DATE(), 'manual', TRUE, 'US Dollar - base'),

  -- Already provided major
  ('INR', 0.012, CURRENT_DATE(), 'backend', TRUE, 'Indian Rupee'),
  ('EUR', 0.92, CURRENT_DATE(), 'backend', TRUE, 'Euro'),
  ('GBP', 0.79, CURRENT_DATE(), 'backend', TRUE, 'British Pound'),
  ('JPY', 154.17, CURRENT_DATE(), 'backend', TRUE, 'Japanese Yen'),
  ('CNY', 7.17, CURRENT_DATE(), 'backend', TRUE, 'Chinese Yuan'),
  ('AUD', 1.50, CURRENT_DATE(), 'backend', TRUE, 'Australian Dollar'),
  ('CAD', 1.42, CURRENT_DATE(), 'backend', TRUE, 'Canadian Dollar'),
  ('SGD', 1.33, CURRENT_DATE(), 'backend', TRUE, 'Singapore Dollar'),
  ('MYR', 4.67, CURRENT_DATE(), 'backend', TRUE, 'Malaysian Ringgit'),
  ('THB', 35.0, CURRENT_DATE(), 'backend', TRUE, 'Thai Baht'),
  ('IDR', 16000.0, CURRENT_DATE(), 'backend', TRUE, 'Indonesian Rupiah'),
  ('PHP', 58.33, CURRENT_DATE(), 'backend', TRUE, 'Philippine Peso'),
  ('VND', 25417.0, CURRENT_DATE(), 'backend', TRUE, 'Vietnamese Dong'),
  ('KRW', 1375.0, CURRENT_DATE(), 'backend', TRUE, 'South Korean Won'),
  ('AED', 3.67, CURRENT_DATE(), 'backend', TRUE, 'UAE Dirham'),
  ('SAR', 3.75, CURRENT_DATE(), 'backend', TRUE, 'Saudi Riyal'),
  ('CHF', 0.92, CURRENT_DATE(), 'backend', TRUE, 'Swiss Franc'),
  ('SEK', 10.83, CURRENT_DATE(), 'backend', TRUE, 'Swedish Krona'),
  ('NOK', 10.83, CURRENT_DATE(), 'backend', TRUE, 'Norwegian Krone'),
  ('DKK', 6.92, CURRENT_DATE(), 'backend', TRUE, 'Danish Krone'),
  ('PLN', 4.0, CURRENT_DATE(), 'backend', TRUE, 'Polish Zloty'),
  ('NZD', 1.67, CURRENT_DATE(), 'backend', TRUE, 'New Zealand Dollar'),
  ('ZAR', 18.33, CURRENT_DATE(), 'backend', TRUE, 'South African Rand'),
  ('BRL', 5.83, CURRENT_DATE(), 'backend', TRUE, 'Brazilian Real'),
  ('MXN', 20.0, CURRENT_DATE(), 'backend', TRUE, 'Mexican Peso'),
  ('RUB', 100.0, CURRENT_DATE(), 'backend', TRUE, 'Russian Ruble'),
  ('TRY', 34.17, CURRENT_DATE(), 'backend', TRUE, 'Turkish Lira'),
  ('HKD', 7.75, CURRENT_DATE(), 'backend', TRUE, 'Hong Kong Dollar'),
  ('TWD', 31.67, CURRENT_DATE(), 'backend', TRUE, 'Taiwan Dollar'),
  ('EGP', 49.17, CURRENT_DATE(), 'backend', TRUE, 'Egyptian Pound'),
  ('PKR', 279.17, CURRENT_DATE(), 'backend', TRUE, 'Pakistani Rupee'),
  ('BDT', 110.0, CURRENT_DATE(), 'backend', TRUE, 'Bangladeshi Taka'),
  ('LKR', 291.67, CURRENT_DATE(), 'backend', TRUE, 'Sri Lankan Rupee'),
  ('NPR', 133.33, CURRENT_DATE(), 'backend', TRUE, 'Nepalese Rupee'),
  ('MVR', 15.0, CURRENT_DATE(), 'backend', TRUE, 'Maldivian Rufiyaa'),

  -- Additional major world currencies (40+ added)
  ('ARS', 950.0, CURRENT_DATE(), 'backend', TRUE, 'Argentine Peso'),
  ('CLP', 950.0, CURRENT_DATE(), 'backend', TRUE, 'Chilean Peso'),
  ('COP', 4200.0, CURRENT_DATE(), 'backend', TRUE, 'Colombian Peso'),
  ('PEN', 3.7, CURRENT_DATE(), 'backend', TRUE, 'Peruvian Sol'),
  ('UYU', 40.0, CURRENT_DATE(), 'backend', TRUE, 'Uruguayan Peso'),
  ('BOB', 6.9, CURRENT_DATE(), 'backend', TRUE, 'Bolivian Boliviano'),
  ('PYG', 7500.0, CURRENT_DATE(), 'backend', TRUE, 'Paraguayan Guarani'),
  ('CRC', 540.0, CURRENT_DATE(), 'backend', TRUE, 'Costa Rican Colon'),
  ('GTQ', 7.8, CURRENT_DATE(), 'backend', TRUE, 'Guatemalan Quetzal'),
  ('HNL', 24.5, CURRENT_DATE(), 'backend', TRUE, 'Honduran Lempira'),
  ('NIO', 36.8, CURRENT_DATE(), 'backend', TRUE, 'Nicaraguan Cordoba'),
  ('DOP', 59.0, CURRENT_DATE(), 'backend', TRUE, 'Dominican Peso'),
  ('JMD', 155.0, CURRENT_DATE(), 'backend', TRUE, 'Jamaican Dollar'),
  ('BBD', 2.0, CURRENT_DATE(), 'backend', TRUE, 'Barbadian Dollar'),
  ('TTD', 6.8, CURRENT_DATE(), 'backend', TRUE, 'Trinidad & Tobago Dollar'),

  -- Middle East & Africa
  ('QAR', 3.64, CURRENT_DATE(), 'backend', TRUE, 'Qatari Riyal'),
  ('KWD', 0.31, CURRENT_DATE(), 'backend', TRUE, 'Kuwaiti Dinar'),
  ('OMR', 0.38, CURRENT_DATE(), 'backend', TRUE, 'Omani Rial'),
  ('BHD', 0.38, CURRENT_DATE(), 'backend', TRUE, 'Bahraini Dinar'),
  ('JOD', 0.71, CURRENT_DATE(), 'backend', TRUE, 'Jordanian Dinar'),
  ('MAD', 10.2, CURRENT_DATE(), 'backend', TRUE, 'Moroccan Dirham'),
  ('TND', 3.1, CURRENT_DATE(), 'backend', TRUE, 'Tunisian Dinar'),
  ('KES', 160.0, CURRENT_DATE(), 'backend', TRUE, 'Kenyan Shilling'),
  ('UGX', 3830.0, CURRENT_DATE(), 'backend', TRUE, 'Ugandan Shilling'),
  ('TZS', 2600.0, CURRENT_DATE(), 'backend', TRUE, 'Tanzanian Shilling'),
  ('ETB', 56.0, CURRENT_DATE(), 'backend', TRUE, 'Ethiopian Birr'),
  ('GHS', 15.5, CURRENT_DATE(), 'backend', TRUE, 'Ghanaian Cedi'),
  ('NGN', 1650.0, CURRENT_DATE(), 'backend', TRUE, 'Nigerian Naira'),
  ('ZMW', 28.0, CURRENT_DATE(), 'backend', TRUE, 'Zambian Kwacha'),
  ('BWP', 13.5, CURRENT_DATE(), 'backend', TRUE, 'Botswana Pula'),
  ('MZN', 63.0, CURRENT_DATE(), 'backend', TRUE, 'Mozambican Metical'),

  -- Europe extra
  ('CZK', 23.0, CURRENT_DATE(), 'backend', TRUE, 'Czech Koruna'),
  ('HUF', 360.0, CURRENT_DATE(), 'backend', TRUE, 'Hungarian Forint'),
  ('RON', 4.6, CURRENT_DATE(), 'backend', TRUE, 'Romanian Leu'),
  ('BGN', 1.8, CURRENT_DATE(), 'backend', TRUE, 'Bulgarian Lev'),
  ('ISK', 138.0, CURRENT_DATE(), 'backend', TRUE, 'Icelandic Krona'),
  ('HRK', 6.9, CURRENT_DATE(), 'backend', TRUE, 'Croatian Kuna'),
  ('RSD', 110.0, CURRENT_DATE(), 'backend', TRUE, 'Serbian Dinar'),

  -- Asia additional
  ('MMK', 2100.0, CURRENT_DATE(), 'backend', TRUE, 'Myanmar Kyat'),
  ('KZT', 480.0, CURRENT_DATE(), 'backend', TRUE, 'Kazakhstani Tenge'),
  ('UZS', 12500.0, CURRENT_DATE(), 'backend', TRUE, 'Uzbekistani Som'),
  ('AZN', 1.7, CURRENT_DATE(), 'backend', TRUE, 'Azerbaijani Manat'),
  ('GEL', 2.7, CURRENT_DATE(), 'backend', TRUE, 'Georgian Lari'),
  ('AMD', 390.0, CURRENT_DATE(), 'backend', TRUE, 'Armenian Dram'),
  ('KHR', 4100.0, CURRENT_DATE(), 'backend', TRUE, 'Cambodian Riel'),
  ('LAK', 21000.0, CURRENT_DATE(), 'backend', TRUE, 'Lao Kip'),
  ('KGS', 89.0, CURRENT_DATE(), 'backend', TRUE, 'Kyrgyzstani Som'),
  ('MNT', 3440.0, CURRENT_DATE(), 'backend', TRUE, 'Mongolian Tugrik'),

  -- Middle Asia/GCC small
  ('IRR', 42000.0, CURRENT_DATE(), 'backend', TRUE, 'Iranian Rial'),

  -- Caribbean & small states
  ('XCD', 2.7, CURRENT_DATE(), 'backend', TRUE, 'East Caribbean Dollar'),
  ('BSD', 1.0, CURRENT_DATE(), 'backend', TRUE, 'Bahamian Dollar'),
  ('CDF', 2800.0, CURRENT_DATE(), 'backend', TRUE, 'Congolese Franc'),
  ('SYP', 13000.0, CURRENT_DATE(), 'backend', TRUE, 'Syrian Pound'),
  ('LYD', 4.9, CURRENT_DATE(), 'backend', TRUE, 'Libyan Dinar'),
  ('SDG', 650.0, CURRENT_DATE(), 'backend', TRUE, 'Sudanese Pound'),

  ('XAF', 610.0, CURRENT_DATE(), 'backend', TRUE, 'Central African CFA Franc'),
  ('XOF', 610.0, CURRENT_DATE(), 'backend', TRUE, 'West African CFA Franc'),
  ('XPF', 111.0, CURRENT_DATE(), 'backend', TRUE, 'CFP Franc'),

  ('AOA', 890.0, CURRENT_DATE(), 'backend', TRUE, 'Angolan Kwanza'),
  ('BIF', 2850.0, CURRENT_DATE(), 'backend', TRUE, 'Burundian Franc'),
  ('DJF', 177.0, CURRENT_DATE(), 'backend', TRUE, 'Djiboutian Franc'),
  ('ERN', 15.0, CURRENT_DATE(), 'backend', TRUE, 'Eritrean Nakfa'),
  ('GMD', 66.0, CURRENT_DATE(), 'backend', TRUE, 'Gambian Dalasi'),
  ('GNF', 8600.0, CURRENT_DATE(), 'backend', TRUE, 'Guinean Franc'),
  ('LSL', 18.5, CURRENT_DATE(), 'backend', TRUE, 'Lesotho Loti'),
  ('LRD', 190.0, CURRENT_DATE(), 'backend', TRUE, 'Liberian Dollar'),
  ('MWK', 1740.0, CURRENT_DATE(), 'backend', TRUE, 'Malawian Kwacha'),
  ('MGA', 4500.0, CURRENT_DATE(), 'backend', TRUE, 'Malagasy Ariary'),
  ('SCR', 14.5, CURRENT_DATE(), 'backend', TRUE, 'Seychellois Rupee'),
  ('SLL', 22500.0, CURRENT_DATE(), 'backend', TRUE, 'Sierra Leonean Leone (new)'),
  ('SOS', 58000.0, CURRENT_DATE(), 'backend', TRUE, 'Somali Shilling'),
  ('SZL', 18.5, CURRENT_DATE(), 'backend', TRUE, 'Eswatini Lilangeni'),

  -- More African & island nations
  ('SHP', 1.22, CURRENT_DATE(), 'backend', TRUE, 'Saint Helena Pound'),
  ('STN', 25.0, CURRENT_DATE(), 'backend', TRUE, 'São Tomé and Príncipe Dobra'),
  ('CVE', 103.0, CURRENT_DATE(), 'backend', TRUE, 'Cape Verdean Escudo'),
  ('KMF', 461.0, CURRENT_DATE(), 'backend', TRUE, 'Comorian Franc'),
  ('RWF', 1350.0, CURRENT_DATE(), 'backend', TRUE, 'Rwandan Franc'),

  -- Middle East / Caucasus additions
  ('SYP2', 12800.0, CURRENT_DATE(), 'backend', TRUE, 'Syrian Pound (parallel ref)'),
  ('YER', 250.0, CURRENT_DATE(), 'backend', TRUE, 'Yemeni Rial'),
  ('LBP', 89500.0, CURRENT_DATE(), 'backend', TRUE, 'Lebanese Pound (new peg)'),

  -- Central Asia & surrounding
  ('TMT', 3.5, CURRENT_DATE(), 'backend', TRUE, 'Turkmenistani Manat'),
  ('TJS', 10.9, CURRENT_DATE(), 'backend', TRUE, 'Tajikistani Somoni'),

  -- Caribbean / Atlantic region
  ('AWG', 1.79, CURRENT_DATE(), 'backend', TRUE, 'Aruban Florin'),
  ('ANG', 1.79, CURRENT_DATE(), 'backend', TRUE, 'Netherlands Antillean Guilder'),
  ('HTG', 132.0, CURRENT_DATE(), 'backend', TRUE, 'Haitian Gourde'),
  ('CUP', 24.0, CURRENT_DATE(), 'backend', TRUE, 'Cuban Peso'),
  ('CUC', 1.0, CURRENT_DATE(), 'backend', TRUE, 'Convertible Cuban Peso'),
  ('SRD', 36.0, CURRENT_DATE(), 'backend', TRUE, 'Surinamese Dollar'),
  ('GYD', 210.0, CURRENT_DATE(), 'backend', TRUE, 'Guyanese Dollar'),
  ('BZD', 2.0, CURRENT_DATE(), 'backend', TRUE, 'Belize Dollar'),

  -- Pacific Islands
  ('FJD', 2.26, CURRENT_DATE(), 'backend', TRUE, 'Fijian Dollar'),
  ('PGK', 3.7, CURRENT_DATE(), 'backend', TRUE, 'Papua New Guinea Kina'),
  ('WST', 2.8, CURRENT_DATE(), 'backend', TRUE, 'Samoan Tala'),
  ('TOP', 2.4, CURRENT_DATE(), 'backend', TRUE, 'Tongan Paʻanga'),
  ('SBD', 8.5, CURRENT_DATE(), 'backend', TRUE, 'Solomon Islands Dollar'),
  ('VUV', 120.0, CURRENT_DATE(), 'backend', TRUE, 'Vanuatu Vatu'),
  ('KID', 1.55, CURRENT_DATE(), 'backend', TRUE, 'Kiribati Dollar'),
  ('TVL', 1.0, CURRENT_DATE(), 'backend', TRUE, 'Tuvalu Dollar (pegged to AUD)'),

  -- South America remaining
  ('SRG', 36.0, CURRENT_DATE(), 'backend', TRUE, 'Suriname Guilder (legacy)'),
  ('FKP', 1.22, CURRENT_DATE(), 'backend', TRUE, 'Falkland Islands Pound'),

  -- European microstates
  ('GIP', 0.79, CURRENT_DATE(), 'backend', TRUE, 'Gibraltar Pound'),
  ('MKD', 58.0, CURRENT_DATE(), 'backend', TRUE, 'Macedonian Denar'),
  ('MOP', 8.1, CURRENT_DATE(), 'backend', TRUE, 'Macanese Pataca'),
  ('MDL', 17.7, CURRENT_DATE(), 'backend', TRUE, 'Moldovan Leu'),
  ('BYN', 3.4, CURRENT_DATE(), 'backend', TRUE, 'Belarusian Ruble'),
  ('ALC', 100.0, CURRENT_DATE(), 'backend', TRUE, 'Alderney Pound'),
  ('GGY', 1.22, CURRENT_DATE(), 'backend', TRUE, 'Guernsey Pound'),
  ('JEP', 1.22, CURRENT_DATE(), 'backend', TRUE, 'Jersey Pound'),

  -- More Latin American / Caribbean
  ('BMD', 1.0, CURRENT_DATE(), 'backend', TRUE, 'Bermudian Dollar'),
  ('BND', 1.33, CURRENT_DATE(), 'backend', TRUE, 'Brunei Dollar'),
  
  -- Africa odd / older / small
  ('SDR', 1.32, CURRENT_DATE(), 'backend', TRUE, 'IMF Special Drawing Rights'),
  ('ZWL', 6500.0, CURRENT_DATE(), 'backend', TRUE, 'Zimbabwean Dollar'),
  ('ZWR', 30000.0, CURRENT_DATE(), 'backend', TRUE, 'Zimbabwe Bond Note (legacy)'),
  ('ZWN', 500000.0, CURRENT_DATE(), 'backend', TRUE, 'Zimbabwe 3rd Dollar'),
  ('ZWD', 361000.0, CURRENT_DATE(), 'backend', TRUE, 'Zimbabwe 1st Dollar (historic)'),

  -- Asia minor
  ('BTN', 0.012, CURRENT_DATE(), 'backend', TRUE, 'Bhutanese Ngultrum'),
  ('TMT2', 3.5, CURRENT_DATE(), 'backend', TRUE, 'Alternate Turkmenistan Manat'),
  ('BAM', 1.8, CURRENT_DATE(), 'backend', TRUE, 'Bosnia Convertible Mark'),
  ('KPW', 900.0, CURRENT_DATE(), 'backend', TRUE, 'North Korean Won'),
  ('MRO', 36.0, CURRENT_DATE(), 'backend', TRUE, 'Mauritanian Ouguiya (old)'),
  ('MRU', 10.3, CURRENT_DATE(), 'backend', TRUE, 'Mauritanian Ouguiya (new)'),
  ('GGP', 1.22, CURRENT_DATE(), 'backend', TRUE, 'Guernsey Pound'),
  ('IMP', 1.22, CURRENT_DATE(), 'backend', TRUE, 'Isle of Man Pound'),

  -- More island territories
  ('TVD', 1.55, CURRENT_DATE(), 'backend', TRUE, 'Tuvalu Dollar'),
  ('NAD', 18.5, CURRENT_DATE(), 'backend', TRUE, 'Namibian Dollar'),

  -- Commodity codes (optional but ISO recognized)
  ('XAU', 0.00042, CURRENT_DATE(), 'backend', TRUE, 'Gold (troy ounce)'),
  ('XAG', 0.04, CURRENT_DATE(), 'backend', TRUE, 'Silver (troy ounce)'),
  ('XPT', 0.001, CURRENT_DATE(), 'backend', TRUE, 'Platinum'),
  ('XPD', 0.0007, CURRENT_DATE(), 'backend', TRUE, 'Palladium');

