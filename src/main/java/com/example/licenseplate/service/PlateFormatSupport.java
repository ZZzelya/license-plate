package com.example.licenseplate.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlateFormatSupport {

    public static final String ALLOWED_SERIES_LETTERS = "ABEKMHOPCTX";
    public static final Pattern FULL_PLATE_PATTERN =
        Pattern.compile("^((?:\\d{4})|(?:E\\d{3}))\\s([A-Z]{2})-(\\d)$");
    public static final Pattern NUMBER_PART_PATTERN = Pattern.compile("^(?:\\d{4}|E\\d{3})$");
    public static final Pattern SERIES_PATTERN =
        Pattern.compile("^[" + ALLOWED_SERIES_LETTERS + "]{2}$");
    public static final Pattern ELECTRIC_SERIES_PATTERN = Pattern.compile("^[A-Z]{2}$");
    public static final Pattern VIN_PATTERN = Pattern.compile("^[A-Z0-9]{17}$");

    private PlateFormatSupport() {
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public static PlateParts parse(String fullPlateNumber) {
        String normalized = normalize(fullPlateNumber);
        if (normalized == null) {
            return null;
        }

        Matcher matcher = FULL_PLATE_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        String numberPart = matcher.group(1);
        String series = matcher.group(2);
        if (!isValidSeriesForNumberPart(numberPart, series)) {
            return null;
        }

        return new PlateParts(numberPart, series, matcher.group(3), normalized);
    }

    public static String buildPlateNumber(String numberPart, String series, String regionCode) {
        return normalize(numberPart) + " " + normalize(series) + "-" + normalize(regionCode);
    }

    public static boolean isElectricNumberPart(String numberPart) {
        String normalized = normalize(numberPart);
        return normalized != null && normalized.matches("^E\\d{3}$");
    }

    public static boolean isValidSeriesForNumberPart(String numberPart, String series) {
        String normalizedSeries = normalize(series);
        if (normalizedSeries == null) {
            return false;
        }
        return isElectricNumberPart(numberPart)
            ? ELECTRIC_SERIES_PATTERN.matcher(normalizedSeries).matches()
            : SERIES_PATTERN.matcher(normalizedSeries).matches();
    }

    public static Set<String> resolveAllowedRegionCodes(String region) {
        String normalized = transliterateToAscii(normalizeRegion(region));
        Set<String> codes = new LinkedHashSet<>();

        if (normalized.contains("BREST")) {
            codes.add("1");
        }
        if (normalized.contains("VITEB")) {
            codes.add("2");
        }
        if (normalized.contains("GOMEL")) {
            codes.add("3");
        }
        if (normalized.contains("GRODN")) {
            codes.add("4");
        }
        if (normalized.contains("MINSK") &&
            (normalized.contains("OBL") || normalized.contains("OBLAST"))) {
            codes.add("5");
        }
        if (normalized.contains("MOGIL")) {
            codes.add("6");
        }
        if (normalized.contains("MINSK") && !codes.contains("5")) {
            codes.add("7");
            codes.add("8");
        }
        if (normalized.contains("VOORUZH") || normalized.contains("ARMED") || normalized.contains("FORCES")) {
            codes.add("0");
        }

        return codes;
    }

    public static String normalizeRegion(String region) {
        return region == null ? "" : region.trim().toUpperCase(Locale.ROOT);
    }

    public static String transliterateToAscii(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (char ch : value.toCharArray()) {
            switch (Character.toLowerCase(ch)) {
                case 'а' -> builder.append('a');
                case 'б' -> builder.append('b');
                case 'в' -> builder.append('v');
                case 'г' -> builder.append('g');
                case 'д' -> builder.append('d');
                case 'е', 'ё' -> builder.append('e');
                case 'ж' -> builder.append("zh");
                case 'з' -> builder.append('z');
                case 'и', 'й' -> builder.append('i');
                case 'к' -> builder.append('k');
                case 'л' -> builder.append('l');
                case 'м' -> builder.append('m');
                case 'н' -> builder.append('n');
                case 'о' -> builder.append('o');
                case 'п' -> builder.append('p');
                case 'р' -> builder.append('r');
                case 'с' -> builder.append('s');
                case 'т' -> builder.append('t');
                case 'у' -> builder.append('u');
                case 'ф' -> builder.append('f');
                case 'х' -> builder.append('h');
                case 'ц' -> builder.append('c');
                case 'ч' -> builder.append("ch");
                case 'ш' -> builder.append("sh");
                case 'щ' -> builder.append("sch");
                case 'ы' -> builder.append('y');
                case 'э' -> builder.append('e');
                case 'ю' -> builder.append("yu");
                case 'я' -> builder.append("ya");
                case 'ь', 'ъ' -> {
                }
                default -> {
                    if (Character.isLetterOrDigit(ch)) {
                        builder.append(Character.toLowerCase(ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString().toUpperCase(Locale.ROOT);
    }

    public record PlateParts(String numberPart, String series, String regionCode, String fullPlateNumber) {
    }
}
