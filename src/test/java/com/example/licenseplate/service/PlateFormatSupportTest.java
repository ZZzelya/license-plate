package com.example.licenseplate.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlateFormatSupportTest {

    @Test
    void normalizeHandlesNullAndUppercases() {
        assertThat(PlateFormatSupport.normalize(null)).isNull();
        assertThat(PlateFormatSupport.normalize(" ab ")).isEqualTo("AB");
    }

    @Test
    void parseReturnsPartsForValidPlate() {
        PlateFormatSupport.PlateParts parts = PlateFormatSupport.parse("1234 ab-7");

        assertThat(parts.numberPart()).isEqualTo("1234");
        assertThat(parts.series()).isEqualTo("AB");
        assertThat(parts.regionCode()).isEqualTo("7");
        assertThat(parts.fullPlateNumber()).isEqualTo("1234 AB-7");
    }

    @Test
    void parseReturnsNullForInvalidPlate() {
        assertThat(PlateFormatSupport.parse(null)).isNull();
        assertThat(PlateFormatSupport.parse("123 AB-7")).isNull();
    }

    @Test
    void buildPlateNumberBuildsNormalizedValue() {
        assertThat(PlateFormatSupport.buildPlateNumber("1234", "ab", "7")).isEqualTo("1234 AB-7");
    }

    @Test
    void resolveAllowedRegionCodesHandlesDifferentRegions() {
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Brest")).containsExactly("1");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Vitebsk")).containsExactly("2");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Gomel")).containsExactly("3");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Grodno")).containsExactly("4");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Minsk oblast")).containsExactly("5");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Mogilev")).containsExactly("6");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Minsk")).containsExactly("7", "8");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Armed Forces")).containsExactly("0");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Unknown")).isEmpty();
    }

    @Test
    void resolveAllowedRegionCodesHandlesAdditionalAliases() {
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Minsk obl")).containsExactly("5");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Forces")).containsExactly("0");
        assertThat(PlateFormatSupport.resolveAllowedRegionCodes("Vooruzh")).containsExactly("0");
    }

    @Test
    void transliterateToAsciiConvertsCyrillicAndPassesAsciiThrough() {
        assertThat(PlateFormatSupport.transliterateToAscii("РњРёРЅСЃРє")).isEqualTo("MINSK");
        assertThat(PlateFormatSupport.transliterateToAscii("Minsk")).isEqualTo("MINSK");
        assertThat(PlateFormatSupport.transliterateToAscii("   ")).isEmpty();
    }

    @Test
    void transliterateToAsciiCoversAllMappedLettersAndDefaultBranch() {
        String input = "Р°Р±РІРіРґРµС‘Р¶Р·РёР№РєР»РјРЅРѕРїСЂСЃС‚СѓС„С…С†С‡С€С‰С‹СЌСЋСЏСЊСЉ-19";
        assertThat(PlateFormatSupport.transliterateToAscii(input))
            .isEqualTo("ABVGDEEZHZIIKLMNOPRSTUFHCCHSHSCHYEYUYA-19");
    }

    @Test
    void normalizeRegionHandlesNullAndTrimming() {
        assertThat(PlateFormatSupport.normalizeRegion(null)).isEmpty();
        assertThat(PlateFormatSupport.normalizeRegion(" minsk ")).isEqualTo("MINSK");
    }
}
