package me.xchro.neon.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalDouble;

public final class NumberValue {
    public static OptionalDouble parse(String text, double min, double max, boolean integer) {
        try {
            double value = Double.parseDouble(text.trim());
            if (!Double.isFinite(value) || value < min || value > max || integer && value != Math.rint(value)) return OptionalDouble.empty();
            return OptionalDouble.of(value);
        } catch (NumberFormatException invalid) { return OptionalDouble.empty(); }
    }
    public static String format(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
