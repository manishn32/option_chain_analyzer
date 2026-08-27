package com.nse.analyser.utils;

import java.util.List;

public class DataTransformUtil {
    public static List<Double> convertToIntegerList(List<String> strikePrices) {
        return strikePrices.stream().map(Double::parseDouble).toList();
    }
}
