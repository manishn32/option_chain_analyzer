package com.nse.analyser.enums;

public enum OptionRank {

    ITM,
    ATM,
    OTM;

    public static OptionRank from(
            double strikePrice,
            double atmStrike,
            OptionType optionType
    ) {

        if (Double.compare(strikePrice, atmStrike) == 0) {
            return ATM;
        }

        if (optionType == OptionType.CE) {

            return strikePrice < atmStrike
                    ? ITM
                    : OTM;
        }

        if (optionType == OptionType.PE) {

            return strikePrice > atmStrike
                    ? ITM
                    : OTM;
        }

        throw new IllegalArgumentException(
                "Unsupported option type: " + optionType
        );
    }
}
