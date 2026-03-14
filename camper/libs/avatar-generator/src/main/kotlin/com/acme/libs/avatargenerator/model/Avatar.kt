package com.acme.libs.avatargenerator.model

/** Deterministically generated avatar composed of visual properties. */
data class Avatar(
    val hairStyle: HairStyle,
    val hairColor: HairColor,
    val skinColor: SkinColor,
    val clothingStyle: ClothingStyle,
    val pantsColor: PantsColor,
    val shirtColor: ShirtColor
)

enum class HairStyle {
    SHORT, LONG, CURLY, WAVY, BUZZ, MOHAWK, PONYTAIL, BUN
}

enum class HairColor {
    BLACK, BROWN, BLONDE, RED, GRAY, WHITE, AUBURN, PLATINUM
}

enum class SkinColor {
    LIGHT, FAIR, MEDIUM, OLIVE, TAN, BROWN, DARK, DEEP
}

enum class ClothingStyle {
    CASUAL, SPORTY, OUTDOORSY, FORMAL, RUGGED, BOHEMIAN, PREPPY, MINIMALIST
}

enum class PantsColor {
    BLACK, NAVY, KHAKI, OLIVE, BROWN, GRAY, DENIM, CHARCOAL
}

enum class ShirtColor {
    RED, BLUE, GREEN, YELLOW, ORANGE, PURPLE, WHITE, TEAL
}
