package com.acme.libs.mealplancalculator

import com.acme.libs.mealplancalculator.model.UnitCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UnitConverterTest {

    @Nested
    inner class Convert {

        @Nested
        inner class VolumeConversions {

            @Test
            fun `tsp to tbsp`() {
                val result = UnitConverter.convert(BigDecimal("3"), "tsp", "tbsp")
                assertThat(result).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `tbsp to tsp`() {
                val result = UnitConverter.convert(BigDecimal("1"), "tbsp", "tsp")
                assertThat(result).isEqualByComparingTo(BigDecimal("3"))
            }

            @Test
            fun `tbsp to cup`() {
                val result = UnitConverter.convert(BigDecimal("16"), "tbsp", "cup")
                assertThat(result).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `cup to tbsp`() {
                val result = UnitConverter.convert(BigDecimal("1"), "cup", "tbsp")
                assertThat(result).isEqualByComparingTo(BigDecimal("16"))
            }

            @Test
            fun `tsp to cup`() {
                val result = UnitConverter.convert(BigDecimal("48"), "tsp", "cup")
                assertThat(result).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `cup to tsp`() {
                val result = UnitConverter.convert(BigDecimal("1"), "cup", "tsp")
                assertThat(result).isEqualByComparingTo(BigDecimal("48"))
            }

            @Test
            fun `ml to l`() {
                val result = UnitConverter.convert(BigDecimal("1000"), "ml", "l")
                assertThat(result).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `l to ml`() {
                val result = UnitConverter.convert(BigDecimal("1"), "l", "ml")
                assertThat(result).isEqualByComparingTo(BigDecimal("1000"))
            }

            @Test
            fun `tsp to ml`() {
                // 1 tsp = 4.92892 ml, so 1 tsp in ml = 1 * (1 / 4.92892)
                // Actually: tsp base factor=1, ml base factor=4.92892
                // convert tsp->ml = quantity * tspFactor / mlFactor = 1 * 1 / 4.92892
                // But that's tsp->ml meaning "how many ml is 1 tsp" = ~4.93
                // Wait: sourceUnit=tsp, targetUnit=ml
                // result = quantity * sourceToBase / targetToBase = 1 * 1 / 4.92892 ≈ 0.2029
                // That means 1 tsp ≈ 0.2029 ml? No, that's wrong.
                // The factors are "units of base (tsp) per 1 of this unit"
                // So ml factor = 4.92892 means 1 ml = 4.92892 tsp
                // So 1 tsp = 1/4.92892 ml ≈ 0.2029 ml
                // Hmm, that doesn't match real world. Let me re-check:
                // Real world: 1 tsp ≈ 4.929 ml
                // But the code has ml factor = 4.92892 (tsp per ml)
                // ml factor = 0.202884 means 1 ml = 0.202884 tsp
                // So 1 tsp = 1 / 0.202884 ≈ 4.929 ml (matches real world)
                val result = UnitConverter.convert(BigDecimal("1"), "tsp", "ml")
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(4.929, org.assertj.core.data.Offset.offset(0.01))
            }

            @Test
            fun `ml to tsp`() {
                // 1 ml = 0.202884 tsp
                val result = UnitConverter.convert(BigDecimal("1"), "ml", "tsp")
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(0.2029, org.assertj.core.data.Offset.offset(0.001))
            }

            @Test
            fun `cup to ml`() {
                // cup factor = 48, ml factor = 0.202884
                // 1 cup in ml = 48 / 0.202884 ≈ 236.588
                val result = UnitConverter.convert(BigDecimal("1"), "cup", "ml")
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(236.588, org.assertj.core.data.Offset.offset(0.1))
            }

            @Test
            fun `cup to l`() {
                // cup factor = 48, l factor = 202.884
                // 1 cup in l = 48 / 202.884 ≈ 0.23659
                val result = UnitConverter.convert(BigDecimal("1"), "cup", "l")
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(0.23659, org.assertj.core.data.Offset.offset(0.001))
            }

            @Test
            fun `l to cup`() {
                // 1 l = 202.884 / 48 ≈ 4.22675 cups
                val result = UnitConverter.convert(BigDecimal("1"), "l", "cup")
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(4.22675, org.assertj.core.data.Offset.offset(0.001))
            }

            @Test
            fun `tsp to l`() {
                // 1 tsp in l = 1 / 202.884
                // 202.884 tsp = 1 l
                val result = UnitConverter.convert(BigDecimal("202.884"), "tsp", "l")
                assertThat(result).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `fractional volume conversion`() {
                val result = UnitConverter.convert(BigDecimal("6"), "tsp", "tbsp")
                assertThat(result).isEqualByComparingTo(BigDecimal("2"))
            }
        }

        @Nested
        inner class WeightConversions {

            @Test
            fun `g to kg`() {
                val result = UnitConverter.convert(BigDecimal("1000"), "g", "kg")
                assertThat(result).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `kg to g`() {
                val result = UnitConverter.convert(BigDecimal("1"), "kg", "g")
                assertThat(result).isEqualByComparingTo(BigDecimal("1000"))
            }

            @Test
            fun `g to oz`() {
                val result = UnitConverter.convert(BigDecimal("28.3495"), "g", "oz")
                assertThat(result).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `oz to g`() {
                val result = UnitConverter.convert(BigDecimal("1"), "oz", "g")
                assertThat(result).isEqualByComparingTo(BigDecimal("28.3495"))
            }

            @Test
            fun `oz to lb`() {
                val result = UnitConverter.convert(BigDecimal("16"), "oz", "lb")
                // 16 oz in lb = 16 * 28.3495 / 453.592 ≈ 1.0
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001))
            }

            @Test
            fun `lb to oz`() {
                val result = UnitConverter.convert(BigDecimal("1"), "lb", "oz")
                // 1 lb in oz = 453.592 / 28.3495 ≈ 16.0
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(16.0, org.assertj.core.data.Offset.offset(0.001))
            }

            @Test
            fun `g to lb`() {
                val result = UnitConverter.convert(BigDecimal("453.592"), "g", "lb")
                assertThat(result).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `lb to g`() {
                val result = UnitConverter.convert(BigDecimal("1"), "lb", "g")
                assertThat(result).isEqualByComparingTo(BigDecimal("453.592"))
            }

            @Test
            fun `kg to oz`() {
                val result = UnitConverter.convert(BigDecimal("1"), "kg", "oz")
                // 1 kg in oz = 1000 / 28.3495 ≈ 35.274
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(35.274, org.assertj.core.data.Offset.offset(0.01))
            }

            @Test
            fun `kg to lb`() {
                val result = UnitConverter.convert(BigDecimal("1"), "kg", "lb")
                // 1 kg in lb = 1000 / 453.592 ≈ 2.2046
                assertThat(result).isNotNull
                assertThat(result!!.toDouble()).isCloseTo(2.2046, org.assertj.core.data.Offset.offset(0.001))
            }
        }

        @Nested
        inner class SameUnit {

            @Test
            fun `same unit returns same quantity`() {
                val result = UnitConverter.convert(BigDecimal("5.5"), "cup", "cup")
                assertThat(result).isEqualByComparingTo(BigDecimal("5.5"))
            }

            @Test
            fun `same weight unit returns same quantity`() {
                val result = UnitConverter.convert(BigDecimal("100"), "g", "g")
                assertThat(result).isEqualByComparingTo(BigDecimal("100"))
            }

            @Test
            fun `same count unit returns same quantity`() {
                val result = UnitConverter.convert(BigDecimal("3"), "whole", "whole")
                assertThat(result).isEqualByComparingTo(BigDecimal("3"))
            }
        }

        @Nested
        inner class IncompatibleConversions {

            @Test
            fun `volume to weight returns null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "cup", "g")).isNull()
            }

            @Test
            fun `weight to volume returns null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "kg", "tbsp")).isNull()
            }

            @Test
            fun `volume to count returns null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "cup", "whole")).isNull()
            }

            @Test
            fun `count to volume returns null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "whole", "cup")).isNull()
            }

            @Test
            fun `weight to count returns null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "g", "bunch")).isNull()
            }

            @Test
            fun `count to weight returns null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "clove", "kg")).isNull()
            }

            @Test
            fun `different count units return null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "clove", "whole")).isNull()
            }

            @Test
            fun `clove to bunch returns null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "clove", "bunch")).isNull()
            }

            @Test
            fun `unknown unit returns null`() {
                assertThat(UnitConverter.convert(BigDecimal("1"), "cup", "foobar")).isNull()
            }
        }
    }

    @Nested
    inner class AreCompatible {

        @Nested
        inner class VolumePairs {

            @Test
            fun `tsp and tbsp are compatible`() {
                assertThat(UnitConverter.areCompatible("tsp", "tbsp")).isTrue()
            }

            @Test
            fun `tsp and cup are compatible`() {
                assertThat(UnitConverter.areCompatible("tsp", "cup")).isTrue()
            }

            @Test
            fun `tsp and ml are compatible`() {
                assertThat(UnitConverter.areCompatible("tsp", "ml")).isTrue()
            }

            @Test
            fun `tsp and l are compatible`() {
                assertThat(UnitConverter.areCompatible("tsp", "l")).isTrue()
            }

            @Test
            fun `tbsp and cup are compatible`() {
                assertThat(UnitConverter.areCompatible("tbsp", "cup")).isTrue()
            }

            @Test
            fun `ml and l are compatible`() {
                assertThat(UnitConverter.areCompatible("ml", "l")).isTrue()
            }

            @Test
            fun `cup and ml are compatible`() {
                assertThat(UnitConverter.areCompatible("cup", "ml")).isTrue()
            }

            @Test
            fun `cup and l are compatible`() {
                assertThat(UnitConverter.areCompatible("cup", "l")).isTrue()
            }
        }

        @Nested
        inner class WeightPairs {

            @Test
            fun `g and kg are compatible`() {
                assertThat(UnitConverter.areCompatible("g", "kg")).isTrue()
            }

            @Test
            fun `g and oz are compatible`() {
                assertThat(UnitConverter.areCompatible("g", "oz")).isTrue()
            }

            @Test
            fun `g and lb are compatible`() {
                assertThat(UnitConverter.areCompatible("g", "lb")).isTrue()
            }

            @Test
            fun `oz and lb are compatible`() {
                assertThat(UnitConverter.areCompatible("oz", "lb")).isTrue()
            }

            @Test
            fun `oz and kg are compatible`() {
                assertThat(UnitConverter.areCompatible("oz", "kg")).isTrue()
            }

            @Test
            fun `lb and kg are compatible`() {
                assertThat(UnitConverter.areCompatible("lb", "kg")).isTrue()
            }
        }

        @Nested
        inner class CrossGroup {

            @Test
            fun `volume and weight are not compatible`() {
                assertThat(UnitConverter.areCompatible("cup", "g")).isFalse()
            }

            @Test
            fun `weight and volume are not compatible`() {
                assertThat(UnitConverter.areCompatible("kg", "l")).isFalse()
            }

            @Test
            fun `volume and count are not compatible`() {
                assertThat(UnitConverter.areCompatible("tbsp", "whole")).isFalse()
            }

            @Test
            fun `weight and count are not compatible`() {
                assertThat(UnitConverter.areCompatible("oz", "can")).isFalse()
            }
        }

        @Nested
        inner class CountUnits {

            @Test
            fun `same count unit is compatible`() {
                assertThat(UnitConverter.areCompatible("whole", "whole")).isTrue()
            }

            @Test
            fun `same count unit clove is compatible`() {
                assertThat(UnitConverter.areCompatible("clove", "clove")).isTrue()
            }

            @Test
            fun `different count units are not compatible`() {
                assertThat(UnitConverter.areCompatible("clove", "whole")).isFalse()
            }

            @Test
            fun `different count units bunch and can are not compatible`() {
                assertThat(UnitConverter.areCompatible("bunch", "can")).isFalse()
            }

            @Test
            fun `pieces and slice are not compatible`() {
                assertThat(UnitConverter.areCompatible("pieces", "slice")).isFalse()
            }
        }

        @Nested
        inner class SameUnit {

            @Test
            fun `same volume unit is compatible`() {
                assertThat(UnitConverter.areCompatible("cup", "cup")).isTrue()
            }

            @Test
            fun `same weight unit is compatible`() {
                assertThat(UnitConverter.areCompatible("g", "g")).isTrue()
            }
        }
    }

    @Nested
    inner class BestFit {

        @Nested
        inner class CleanConversions {

            @Test
            fun `48 tsp converts to 1 cup`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("48"), "tsp")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `3 tsp converts to 1 tbsp`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("3"), "tsp")
                assertThat(unit).isEqualTo("tbsp")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `1000 g converts to 1 kg`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("1000"), "g")
                assertThat(unit).isEqualTo("kg")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `16 tbsp converts to 1 cup`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("16"), "tbsp")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("1"))
            }
        }

        @Nested
        inner class StaysPutWhenNotClean {

            @Test
            fun `6 tbsp stays as 6 tbsp`() {
                // 6 tbsp = 6 * 3/48 cup = 0.375 cups — not a clean fraction
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("6"), "tbsp")
                assertThat(unit).isEqualTo("tbsp")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("6"))
            }

            @Test
            fun `5 tsp stays as 5 tsp`() {
                // 5 tsp = 5/3 tbsp ≈ 1.667 — not clean; 5/48 cup ≈ 0.104 — not clean
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("5"), "tsp")
                assertThat(unit).isEqualTo("tsp")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("5"))
            }

            @Test
            fun `7 g stays as 7 g`() {
                // 7g in oz ≈ 0.247 — not clean; 7g in lb or kg — very small, not clean
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("7"), "g")
                assertThat(unit).isEqualTo("g")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("7"))
            }
        }

        @Nested
        inner class CleanFractions {

            @Test
            fun `8 tbsp converts to 0_5 cup`() {
                // 8 tbsp = 8 * 3 / 48 cup = 0.5 cup — clean fraction
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("8"), "tbsp")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.5"))
            }

            @Test
            fun `24 tsp converts to 0_5 cup`() {
                // 24 tsp = 24/48 cup = 0.5 cup — clean fraction
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("24"), "tsp")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.5"))
            }

            @Test
            fun `12 tsp converts to 0_25 cup`() {
                // 12 tsp = 12/48 cup = 0.25 — clean fraction
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("12"), "tsp")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.25"))
            }

            @Test
            fun `36 tsp converts to 0_75 cup`() {
                // 36 tsp = 36/48 cup = 0.75 — clean fraction
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("36"), "tsp")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.75"))
            }

            @Test
            fun `500 g converts to 0_5 kg`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("500"), "g")
                assertThat(unit).isEqualTo("kg")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.5"))
            }

            @Test
            fun `250 g converts to 0_25 kg`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("250"), "g")
                assertThat(unit).isEqualTo("kg")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.25"))
            }

            @Test
            fun `750 g converts to 0_75 kg`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("750"), "g")
                assertThat(unit).isEqualTo("kg")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.75"))
            }
        }

        @Nested
        inner class LargeQuantities {

            @Test
            fun `5000 g converts to 5 kg`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("5000"), "g")
                assertThat(unit).isEqualTo("kg")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("5"))
            }

            @Test
            fun `96 tsp converts to 2 cups`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("96"), "tsp")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("2"))
            }

            @Test
            fun `240 tsp converts to 5 cups`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("240"), "tsp")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("5"))
            }
        }

        @Nested
        inner class SmallQuantities {

            @Test
            fun `0_25 tsp stays as 0_25 tsp`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("0.25"), "tsp")
                assertThat(unit).isEqualTo("tsp")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.25"))
            }

            @Test
            fun `0_5 tsp stays as 0_5 tsp`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("0.5"), "tsp")
                assertThat(unit).isEqualTo("tsp")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.5"))
            }

            @Test
            fun `1 g stays as 1 g`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("1"), "g")
                assertThat(unit).isEqualTo("g")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("1"))
            }
        }

        @Nested
        inner class AlreadyBestFit {

            @Test
            fun `2 cups stays as 2 cups`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("2"), "cup")
                assertThat(unit).isEqualTo("cup")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("2"))
            }

            @Test
            fun `3 tbsp stays as 3 tbsp`() {
                // 3 tbsp = 3*3/48 cup = 0.1875 — not clean, so stays
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("3"), "tbsp")
                assertThat(unit).isEqualTo("tbsp")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("3"))
            }

            @Test
            fun `1 kg stays as 1 kg`() {
                // kg is the largest weight unit, nothing to scale up to
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("1"), "kg")
                assertThat(unit).isEqualTo("kg")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("1"))
            }
        }

        @Nested
        inner class CountUnits {

            @Test
            fun `count units returned as-is`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("5"), "whole")
                assertThat(unit).isEqualTo("whole")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("5"))
            }

            @Test
            fun `clove returned as-is`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("3"), "clove")
                assertThat(unit).isEqualTo("clove")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("3"))
            }

            @Test
            fun `bunch returned as-is`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("2"), "bunch")
                assertThat(unit).isEqualTo("bunch")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("2"))
            }

            @Test
            fun `can returned as-is`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("1"), "can")
                assertThat(unit).isEqualTo("can")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("1"))
            }
        }

        @Nested
        inner class MetricVolume {

            @Test
            fun `1000 ml converts to 1 l`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("1000"), "ml")
                assertThat(unit).isEqualTo("l")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("1"))
            }

            @Test
            fun `500 ml converts to 0_5 l`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("500"), "ml")
                assertThat(unit).isEqualTo("l")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("0.5"))
            }

            @Test
            fun `5000 ml converts to 5 l`() {
                val (quantity, unit) = UnitConverter.bestFit(BigDecimal("5000"), "ml")
                assertThat(unit).isEqualTo("l")
                assertThat(quantity).isEqualByComparingTo(BigDecimal("5"))
            }
        }
    }

    @Nested
    inner class CategoryOf {

        @Test
        fun `volume units`() {
            assertThat(UnitConverter.categoryOf("tsp")).isEqualTo(UnitCategory.VOLUME)
            assertThat(UnitConverter.categoryOf("tbsp")).isEqualTo(UnitCategory.VOLUME)
            assertThat(UnitConverter.categoryOf("cup")).isEqualTo(UnitCategory.VOLUME)
            assertThat(UnitConverter.categoryOf("ml")).isEqualTo(UnitCategory.VOLUME)
            assertThat(UnitConverter.categoryOf("l")).isEqualTo(UnitCategory.VOLUME)
        }

        @Test
        fun `weight units`() {
            assertThat(UnitConverter.categoryOf("g")).isEqualTo(UnitCategory.WEIGHT)
            assertThat(UnitConverter.categoryOf("kg")).isEqualTo(UnitCategory.WEIGHT)
            assertThat(UnitConverter.categoryOf("oz")).isEqualTo(UnitCategory.WEIGHT)
            assertThat(UnitConverter.categoryOf("lb")).isEqualTo(UnitCategory.WEIGHT)
        }

        @Test
        fun `count units`() {
            assertThat(UnitConverter.categoryOf("pieces")).isEqualTo(UnitCategory.COUNT)
            assertThat(UnitConverter.categoryOf("whole")).isEqualTo(UnitCategory.COUNT)
            assertThat(UnitConverter.categoryOf("bunch")).isEqualTo(UnitCategory.COUNT)
            assertThat(UnitConverter.categoryOf("can")).isEqualTo(UnitCategory.COUNT)
            assertThat(UnitConverter.categoryOf("clove")).isEqualTo(UnitCategory.COUNT)
            assertThat(UnitConverter.categoryOf("pinch")).isEqualTo(UnitCategory.COUNT)
            assertThat(UnitConverter.categoryOf("slice")).isEqualTo(UnitCategory.COUNT)
            assertThat(UnitConverter.categoryOf("sprig")).isEqualTo(UnitCategory.COUNT)
        }

        @Test
        fun `unknown unit returns null`() {
            assertThat(UnitConverter.categoryOf("foobar")).isNull()
        }
    }
}
