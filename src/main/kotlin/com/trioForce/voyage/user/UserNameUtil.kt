package com.trioForce.voyage.user

/** 由名、姓拼展示用全名（欧美习惯：First Last）。 */
fun buildDisplayName(firstName: String, lastName: String): String =
    listOf(firstName.trim(), lastName.trim()).filter { it.isNotEmpty() }.joinToString(" ")
