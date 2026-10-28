package com.hexel.core.model

import com.hexel.core.hex.HexDirection

/** "Swiping from [sourcePackage]'s tile towards [direction] launches [targetPackage]." */
data class GestureAssignment(
    val sourcePackage: String,
    val direction: HexDirection,
    val targetPackage: String,
)
