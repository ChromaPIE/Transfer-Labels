package com.buuz135.transfer_labels.util;

import java.text.DecimalFormat;

public class NumberUtils {

    private static final String[] suffixes = { "", "K", "M", "B", "T", "Q", "Qi", "Sx", "Sp", "O" };

    public static String getFormatedBigNumber(double value) {
        if (value < 1000) {
            return String.valueOf((int) Math.ceil(value));
        }

        int exp = (int) (Math.log(value) / Math.log(1000));
        if (exp >= suffixes.length) {
            return "Err";
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        return decimalFormat.format(value / Math.pow(1000, exp)) + suffixes[exp];
    }
}
