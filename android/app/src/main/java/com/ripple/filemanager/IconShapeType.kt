package com.ripple.filemanager

enum class IconShapeType(val label: String) {
    COOKIE("Cookie"),
    CIRCLE("Circle"),
    SQUARE("Square"),
    ROUNDED_SQUARE("Rounded Square"),
    IOS("iOS"),
    ONE_UI("One UI"),
    AUTO("Auto");

    companion object {
        val selectableShapes = listOf(COOKIE, CIRCLE, SQUARE, ROUNDED_SQUARE, IOS, ONE_UI, AUTO)
        val fixedShapes = listOf(COOKIE, CIRCLE, SQUARE, ROUNDED_SQUARE, IOS, ONE_UI)

        fun fromString(name: String?): IconShapeType {
            if (name == null) return COOKIE
            return try {
                valueOf(name)
            } catch (e: Exception) {
                when (name) {
                    "SYSTEM" -> COOKIE
                    "SQUIRCLE" -> IOS
                    else -> COOKIE
                }
            }
        }
    }
}
