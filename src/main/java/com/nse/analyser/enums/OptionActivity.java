package com.nse.analyser.enums;

public enum OptionActivity {
    LONG_BUILDUP,
    SHORT_BUILDUP,
    LONG_UNWINDING,
    SHORT_COVERING,
    UNKNOWN;

    public static OptionActivity from(
            Double underlyingChangePercent,
            Long changeInOi,
            Long currentOi
    ) {

        if (underlyingChangePercent == null ||
                changeInOi == null ||
                currentOi == null ||
                currentOi == 0) {

            return UNKNOWN;
        }

        double oiChangePct =
                (changeInOi * 100.0) / currentOi;

        if (Math.abs(underlyingChangePercent) < 0.25 &&
                Math.abs(oiChangePct) < 2.0) {

            return UNKNOWN;
        }

        if (underlyingChangePercent > 0 && oiChangePct > 0) {
            return LONG_BUILDUP;
        }

        if (underlyingChangePercent < 0 && oiChangePct > 0) {
            return SHORT_BUILDUP;
        }

        if (underlyingChangePercent > 0 && oiChangePct < 0) {
            return SHORT_COVERING;
        }

        return LONG_UNWINDING;
    }
}
