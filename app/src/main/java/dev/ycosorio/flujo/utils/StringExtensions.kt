package dev.ycosorio.flujo.utils

fun String.isValidEmail(): Boolean {
    val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    return matches(emailPattern.toRegex())
}